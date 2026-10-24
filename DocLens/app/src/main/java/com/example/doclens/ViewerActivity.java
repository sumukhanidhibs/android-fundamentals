package com.example.doclens;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows a PDF. Opened from the library, or by any other app with
 * ACTION_VIEW / ACTION_SEND (that's what makes DocLens a PDF viewer the
 * system can offer, and set as default).
 *
 * Fast open: the file is handed straight to PdfRenderer (no copy), the
 * first page is shown as soon as it's rendered, and the rest render on
 * demand while scrolling.
 */
public class ViewerActivity extends AppCompatActivity {

    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_KEY = "key";
    private static final String EXTRA_NAME = "name";
    private static final String PREFS = "viewer";
    private static final String KEY_NIGHT = "night";
    private static final long PILL_HIDE_DELAY_MS = 1200;

    private static final int ERROR_DAMAGED = 0;
    private static final int ERROR_PASSWORD = 1;
    private static final int ERROR_MISSING = 2;

    /** Opens a file from this device. */
    static void openFile(Context context, String path, String name) {
        openFile(context, path, keyForPath(path), name);
    }

    /** Opens a file that stands in for {@code key}, e.g. the private copy of a shared PDF. */
    static void openFile(Context context, String path, String key, String name) {
        context.startActivity(new Intent(context, ViewerActivity.class)
                .putExtra(EXTRA_PATH, path)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_KEY, key));
    }

    /** Opens a content:// document this app has (persisted) access to. */
    static void openUri(Context context, Uri uri, String key, String name) {
        context.startActivity(new Intent(context, ViewerActivity.class)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_KEY, key));
    }

    static String keyForPath(String path) {
        return Uri.fromFile(new File(path)).toString();
    }

    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hidePill = this::hidePill;

    private MaterialToolbar toolbar;
    private View bottomBar;
    private ZoomRecyclerView pages;
    private LinearLayoutManager layoutManager;
    private CircularProgressIndicator progress;
    private TextView pageLabel;
    private TextView pagePill;
    private Slider pageSlider;
    private SharedPreferences prefs;
    private RecentStore recents;

    private PdfDoc doc;
    private PageAdapter adapter;
    private String key;
    private String path;
    private Uri uri;
    private String name;
    private int currentPage;
    private boolean barsVisible = true;
    private boolean night;
    private boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        toolbar = findViewById(R.id.toolbar);
        bottomBar = findViewById(R.id.bottomBar);
        pages = findViewById(R.id.pages);
        progress = findViewById(R.id.progress);
        pageLabel = findViewById(R.id.pageLabel);
        pagePill = findViewById(R.id.pagePill);
        pageSlider = findViewById(R.id.pageSlider);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        recents = new RecentStore(this);
        night = prefs.getBoolean(KEY_NIGHT, false);

        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(this::onMenuItem);
        toolbar.getMenu().findItem(R.id.action_night).setChecked(night);
        findViewById(R.id.errorClose).setOnClickListener(v -> finish());
        bottomBar.setVisibility(View.INVISIBLE);
        setMenuEnabled(false);

        layoutManager = new LinearLayoutManager(this) {
            @Override
            protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state, @NonNull int[] extra) {
                // Lay out (and so render) about one screen ahead and behind
                int space = pages.getHeight();
                extra[0] = space;
                extra[1] = space;
            }
        };
        pages.setLayoutManager(layoutManager);
        pages.setItemAnimator(null);
        pages.setHasFixedSize(true);
        pages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                updateCurrentPage(dy != 0);
            }
        });
        pages.setListener(new ZoomRecyclerView.Listener() {
            @Override
            public void onTap() {
                setBarsVisible(!barsVisible);
            }

            @Override
            public void onZoomChanged(float scale) {
                if (adapter != null) adapter.setZoom(scale);
            }
        });
        pageSlider.setLabelFormatter(value -> String.valueOf((int) value));
        pageSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) goToPage((int) value - 1);
        });

        if (!readIntent(getIntent())) {
            showError(getString(R.string.error_no_file), ERROR_DAMAGED);
            return;
        }
        toolbar.setTitle(name);
        load();
    }

    /** Works out what to open: our own extras, ACTION_VIEW data, or a shared stream. */
    private boolean readIntent(Intent intent) {
        path = intent.getStringExtra(EXTRA_PATH);
        name = intent.getStringExtra(EXTRA_NAME);
        key = intent.getStringExtra(EXTRA_KEY);
        if (path != null) {
            if (key == null) key = keyForPath(path);
            return true;
        }

        Uri data = intent.getData();
        if (data == null && Intent.ACTION_SEND.equals(intent.getAction())) {
            data = streamExtra(intent);
        }
        if (data == null) return false;

        if (ContentResolver.SCHEME_FILE.equals(data.getScheme()) && data.getPath() != null) {
            path = data.getPath();
            key = keyForPath(path);
        } else {
            uri = data;
            if (key == null) key = data.toString();
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private static Uri streamExtra(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
        }
        return intent.getParcelableExtra(Intent.EXTRA_STREAM);
    }

    private void load() {
        progress.show();
        final Context app = getApplicationContext();
        loader.execute(() -> {
            String displayName = name;
            long size = 0;
            PdfDoc opened = null;
            int error = ERROR_DAMAGED;
            try {
                if (path != null) {
                    File file = new File(path);
                    if (!file.isFile()) throw new FileNotFoundException(path);
                    size = file.length();
                    if (displayName == null) displayName = file.getName();
                    opened = new PdfDoc(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
                } else {
                    String[] meta = queryNameAndSize(app, uri);
                    if (displayName == null) displayName = meta[0];
                    if (meta[1] != null) size = Long.parseLong(meta[1]);
                    opened = openContent(app, uri);
                }
            } catch (SecurityException e) {
                // PdfRenderer throws this for password-protected files, a provider for a revoked grant
                error = e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("password")
                        ? ERROR_PASSWORD : ERROR_MISSING;
            } catch (FileNotFoundException e) {
                error = ERROR_MISSING;
            } catch (IOException | RuntimeException e) {
                error = ERROR_DAMAGED;
            }

            final PdfDoc result = opened;
            final String finalName = displayName != null ? displayName : getString(R.string.app_name);
            final long finalSize = size;
            final int finalError = error;
            handler.post(() -> {
                if (destroyed) {
                    if (result != null) result.close();
                    return;
                }
                name = finalName;
                toolbar.setTitle(name);
                if (result == null) {
                    showError(null, finalError);
                } else {
                    onLoaded(result, finalSize);
                }
            });
        });
    }

    private static String[] queryNameAndSize(Context context, Uri uri) {
        String[] result = {uri.getLastPathSegment(), null};
        try (Cursor c = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int nameCol = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeCol = c.getColumnIndex(OpenableColumns.SIZE);
                if (nameCol >= 0 && !c.isNull(nameCol)) result[0] = c.getString(nameCol);
                if (sizeCol >= 0 && !c.isNull(sizeCol)) result[1] = String.valueOf(c.getLong(sizeCol));
            }
        } catch (RuntimeException e) {
            // Some providers don't support queries; the URI is enough
        }
        return result;
    }

    /**
     * Content from another app: use its file descriptor directly when it can seek
     * (fast). Streams that can't (some mail/chat apps) are copied to the cache first.
     */
    private static PdfDoc openContent(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        try {
            ParcelFileDescriptor fd = resolver.openFileDescriptor(uri, "r");
            if (fd != null) return new PdfDoc(fd);
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException e) {
            // fall through to the copy
        }
        File copy = new File(context.getCacheDir(), "open.pdf");
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new FileNotFoundException(uri.toString());
            copyStream(in, copy);
        }
        return new PdfDoc(ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY));
    }

    private static void copyStream(InputStream in, File target) throws IOException {
        File tmp = new File(target.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        }
        if (!tmp.renameTo(target)) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            throw new IOException("Couldn't save " + target);
        }
    }

    private void onLoaded(PdfDoc loaded, long size) {
        doc = loaded;
        progress.hide();
        setMenuEnabled(true);

        RecentStore.Entry previous = recents.get(key);
        recents.opened(key, name, path, size, doc.pageCount);
        if (path == null) keepPrivateCopy();

        adapter = new PageAdapter(doc);
        adapter.setNight(night);
        pages.setBackgroundColor(getColor(night ? R.color.brand_navy : R.color.viewer_background));
        startWhenMeasured(() -> {
            adapter.setBaseWidth(pageWidth());
            pages.setAdapter(adapter);
            int resume = previous != null ? Math.min(previous.lastPage, doc.pageCount - 1) : 0;
            if (resume > 0) layoutManager.scrollToPositionWithOffset(resume, 0);
            currentPage = resume;
            showPage(false);
        });
        pages.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol) adapter.setBaseWidth(pageWidth());
        });

        if (doc.pageCount > 1) {
            pageSlider.setValueTo(doc.pageCount);
        } else {
            pageSlider.setVisibility(View.GONE);
        }
        bottomBar.setVisibility(barsVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private void startWhenMeasured(Runnable action) {
        if (pages.getWidth() > 0) {
            action.run();
        } else {
            pages.post(action);
        }
    }

    /** Page width in pixels: the list width minus the page margins. */
    private int pageWidth() {
        float margin = 6 * getResources().getDisplayMetrics().density;
        return Math.max(1, Math.round(pages.getWidth() - pages.getPaddingLeft() - pages.getPaddingRight() - 2 * margin));
    }

    /**
     * A PDF from another app can only be read while that app's permission lasts.
     * Keep a private copy (in the background, after it is already on screen)
     * so it can be reopened from Recent later.
     */
    private void keepPrivateCopy() {
        if (hasPersistedPermission(uri)) return;
        final Context app = getApplicationContext();
        final Uri source = uri;
        final String entryKey = key;
        loader.execute(() -> {
            File dir = RecentStore.privateCopyDir(app);
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            File copy = new File(dir, sha1(entryKey) + ".pdf");
            if (!copy.isFile()) {
                try (InputStream in = app.getContentResolver().openInputStream(source)) {
                    if (in == null) return;
                    copyStream(in, copy);
                } catch (IOException | RuntimeException e) {
                    return;
                }
            }
            handler.post(() -> recents.setPath(entryKey, copy.getPath()));
        });
    }

    private boolean hasPersistedPermission(Uri uri) {
        for (UriPermission p : getContentResolver().getPersistedUriPermissions()) {
            if (p.isReadPermission() && p.getUri().equals(uri)) return true;
        }
        return false;
    }

    private static String sha1(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            return String.valueOf(text.hashCode());
        }
    }

    private void showError(String message, int kind) {
        progress.hide();
        setMenuEnabled(false);
        bottomBar.setVisibility(View.GONE);
        pages.setVisibility(View.GONE);
        if (message == null) {
            message = getString(kind == ERROR_PASSWORD ? R.string.error_password
                    : kind == ERROR_MISSING ? R.string.error_missing : R.string.error_damaged);
        }
        ((TextView) findViewById(R.id.errorText)).setText(message);
        ((ImageView) findViewById(R.id.errorIcon)).setImageResource(
                kind == ERROR_PASSWORD ? R.drawable.ic_lock : R.drawable.ic_document);
        findViewById(R.id.errorState).setVisibility(View.VISIBLE);
    }

    private void setMenuEnabled(boolean enabled) {
        for (int i = 0; i < toolbar.getMenu().size(); i++) {
            toolbar.getMenu().getItem(i).setVisible(enabled);
        }
    }

    private boolean onMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_night) {
            night = !night;
            item.setChecked(night);
            prefs.edit().putBoolean(KEY_NIGHT, night).apply();
            if (adapter != null) adapter.setNight(night);
            pages.setBackgroundColor(getColor(night ? R.color.brand_navy : R.color.viewer_background));
            return true;
        } else if (id == R.id.action_go_to_page) {
            askPage();
            return true;
        } else if (id == R.id.action_share) {
            Files.share(this, path, uri, name);
            return true;
        } else if (id == R.id.action_fit_width) {
            pages.resetZoom();
            return true;
        }
        return false;
    }

    private void askPage() {
        if (doc == null) return;
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setImeOptions(EditorInfo.IME_ACTION_GO);
        input.setHint(getString(R.string.go_to_page_hint, doc.pageCount));
        input.setSingleLine();
        FrameLayout box = new FrameLayout(this);
        int pad = Math.round(24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_go_to_page)
                .setView(box)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.go, (d, w) -> goToTypedPage(input))
                .create();
        input.setOnEditorActionListener((v, actionId, event) -> {
            goToTypedPage(input);
            dialog.dismiss();
            return true;
        });
        dialog.show();
        input.requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void goToTypedPage(EditText input) {
        try {
            goToPage(Integer.parseInt(input.getText().toString().trim()) - 1);
        } catch (NumberFormatException ignored) {
        }
    }

    private void goToPage(int page) {
        if (doc == null) return;
        page = Math.max(0, Math.min(doc.pageCount - 1, page));
        pages.resetZoom();
        pages.stopScroll();
        layoutManager.scrollToPositionWithOffset(page, 0);
        currentPage = page;
        showPage(true);
    }

    private void updateCurrentPage(boolean scrolling) {
        View child = pages.findChildAtCenter();
        if (child == null) return;
        int page = pages.getChildAdapterPosition(child);
        if (page == RecyclerView.NO_POSITION) return;
        if (page != currentPage || scrolling) {
            currentPage = page;
            showPage(scrolling);
        }
    }

    private void showPage(boolean flashPill) {
        if (doc == null) return;
        String text = getString(R.string.page_indicator, currentPage + 1, doc.pageCount);
        pageLabel.setText(text);
        pagePill.setText(text);
        toolbar.setSubtitle(text);
        if (doc.pageCount > 1 && pageSlider.getValue() != currentPage + 1) {
            pageSlider.setValue(currentPage + 1);
        }
        if (flashPill && !barsVisible) {
            pagePill.animate().alpha(1f).setDuration(120).start();
            handler.removeCallbacks(hidePill);
            handler.postDelayed(hidePill, PILL_HIDE_DELAY_MS);
        }
    }

    private void hidePill() {
        pagePill.animate().alpha(0f).setDuration(250).start();
    }

    /** Tap on a page hides/shows the bars for distraction-free reading. */
    private void setBarsVisible(boolean visible) {
        if (doc == null) return;
        barsVisible = visible;
        animateBar(toolbar, visible, -toolbar.getHeight());
        animateBar(bottomBar, visible, bottomBar.getHeight());
        if (visible) {
            hidePill();
        } else {
            showPage(true);
        }
    }

    private static void animateBar(View bar, boolean visible, float hiddenOffset) {
        bar.animate().cancel();
        if (visible) {
            bar.setVisibility(View.VISIBLE);
            bar.animate().translationY(0).alpha(1f).setDuration(180).start();
        } else {
            bar.animate().translationY(hiddenOffset).alpha(0f).setDuration(180)
                    .withEndAction(() -> bar.setVisibility(View.INVISIBLE)).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (doc != null && key != null) recents.setLastPage(key, currentPage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        if (doc != null) doc.close();
        loader.shutdown();
    }
}
