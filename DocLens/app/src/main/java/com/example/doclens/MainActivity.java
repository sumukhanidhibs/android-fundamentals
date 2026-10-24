package com.example.doclens;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The library: Recent files, every PDF on the device, and the same PDFs
 * grouped by folder. Tap a PDF to open it in {@link ViewerActivity}.
 */
public class MainActivity extends AppCompatActivity implements LibraryAdapter.Callbacks {

    private static final int TAB_RECENT = 0;
    private static final int TAB_ALL = 1;
    private static final int TAB_FOLDERS = 2;

    private static final String PREFS = "library";
    private static final String KEY_TAB = "tab";
    private static final String KEY_SORT = "sort";
    private static final String KEY_DEFAULT_DISMISSED = "default_dismissed";

    /** Last scan, kept for the life of the process so the list shows instantly. */
    private static List<PdfFile> lastScan;

    private final ExecutorService scanner = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MaterialToolbar toolbar;
    private ChipGroup tabs;
    private TextView summary;
    private SwipeRefreshLayout refresh;
    private RecyclerView list;
    private ExtendedFloatingActionButton fab;
    private LibraryAdapter adapter;
    private SharedPreferences prefs;
    private RecentStore recents;

    private int tab;
    private PdfLibrary.Sort sort;
    private String query = "";
    private PdfLibrary.Folder openFolder;
    private boolean showDefaultBanner;
    private boolean scanning;

    private final ActivityResultLauncher<String> requestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    scan();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> pickPdf =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    // Keep access so the file can be reopened from Recent
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                }
                ViewerActivity.openUri(this, uri, uri.toString(), null);
            });

    private final OnBackPressedCallback closeFolder = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            setOpenFolder(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        tabs = findViewById(R.id.tabs);
        summary = findViewById(R.id.summary);
        refresh = findViewById(R.id.refresh);
        list = findViewById(R.id.list);
        fab = findViewById(R.id.fabOpen);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        recents = new RecentStore(this);

        tab = prefs.getInt(KEY_TAB, TAB_ALL);
        sort = PdfLibrary.Sort.values()[Math.min(prefs.getInt(KEY_SORT, PdfLibrary.Sort.DATE.ordinal()),
                PdfLibrary.Sort.values().length - 1)];

        adapter = new LibraryAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 8) fab.shrink();
                else if (dy < -8) fab.extend();
            }
        });

        refresh.setColorSchemeResources(R.color.brand_red);
        refresh.setOnChildScrollUpCallback((parent, child) -> list.canScrollVertically(-1));
        refresh.setOnRefreshListener(this::scan);

        tabs.check(tab == TAB_RECENT ? R.id.tabRecent : tab == TAB_FOLDERS ? R.id.tabFolders : R.id.tabAll);
        tabs.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            tab = id == R.id.tabRecent ? TAB_RECENT : id == R.id.tabFolders ? TAB_FOLDERS : TAB_ALL;
            prefs.edit().putInt(KEY_TAB, tab).apply();
            openFolder = null;
            render();
            list.scrollToPosition(0);
        });

        fab.setOnClickListener(v -> pickPdf.launch(new String[]{"application/pdf"}));
        setupToolbar();
        getOnBackPressedDispatcher().addCallback(this, closeFolder);

        if (lastScan != null) render();
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> setOpenFolder(null));
        Menu menu = toolbar.getMenu();
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String text) {
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String text) {
                    query = text;
                    render();
                    return true;
                }
            });
        }
        checkSortItem();
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.sort_name) {
                setSort(PdfLibrary.Sort.NAME);
            } else if (id == R.id.sort_date) {
                setSort(PdfLibrary.Sort.DATE);
            } else if (id == R.id.sort_size) {
                setSort(PdfLibrary.Sort.SIZE);
            } else if (id == R.id.action_refresh) {
                refresh.setRefreshing(true);
                scan();
            } else {
                return false;
            }
            return true;
        });
    }

    private void setSort(PdfLibrary.Sort newSort) {
        sort = newSort;
        prefs.edit().putInt(KEY_SORT, sort.ordinal()).apply();
        checkSortItem();
        if (openFolder != null) reopenFolder();
        render();
    }

    private void checkSortItem() {
        int id = sort == PdfLibrary.Sort.NAME ? R.id.sort_name
                : sort == PdfLibrary.Sort.SIZE ? R.id.sort_size : R.id.sort_date;
        MenuItem item = toolbar.getMenu().findItem(id);
        if (item != null) item.setChecked(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showDefaultBanner = !prefs.getBoolean(KEY_DEFAULT_DISMISSED, false) && !DefaultViewer.isDefault(this);
        // MediaStore answers in milliseconds, so refresh every time the library comes back
        if (StorageAccess.has(this)) scan();
        render();
    }

    private void scan() {
        if (!StorageAccess.has(this)) {
            refresh.setRefreshing(false);
            render();
            return;
        }
        if (scanning) return;
        scanning = true;
        if (lastScan == null) refresh.setRefreshing(true);
        scanner.execute(() -> {
            List<PdfFile> found = PdfScanner.scan(getApplicationContext());
            handler.post(() -> {
                scanning = false;
                lastScan = found;
                refresh.setRefreshing(false);
                if (isDestroyed()) return;
                if (openFolder != null) reopenFolder();
                render();
            });
        });
    }

    /** After a rescan or re-sort, show the fresh copy of the open folder. */
    private void reopenFolder() {
        if (openFolder == null || lastScan == null) return;
        for (PdfLibrary.Folder f : PdfLibrary.folders(lastScan, sort)) {
            if (f.path.equals(openFolder.path)) {
                openFolder = f;
                return;
            }
        }
        setOpenFolder(null);
    }

    private void setOpenFolder(PdfLibrary.Folder folder) {
        openFolder = folder;
        render();
        list.scrollToPosition(0);
    }

    /** Rebuilds the list for the current tab, folder, search and sort order. */
    private void render() {
        List<LibraryAdapter.Row> rows = new ArrayList<>();
        if (showDefaultBanner && openFolder == null && query.isEmpty()) rows.add(LibraryAdapter.Row.banner());
        int before = rows.size();
        boolean access = StorageAccess.has(this);
        String summaryText = null;

        if (tab == TAB_RECENT) {
            for (RecentStore.Entry e : recents.all()) {
                if (LibraryAdapter.isMissing(e)) {
                    recents.remove(e.key);
                    continue;
                }
                if (query.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT))) {
                    rows.add(LibraryAdapter.Row.recent(e));
                }
            }
            if (rows.size() == before && query.isEmpty()) {
                rows.add(LibraryAdapter.Row.empty(R.drawable.ic_document,
                        getString(R.string.empty_recent_title), getString(R.string.empty_recent_message),
                        getString(R.string.browse_all), LibraryAdapter.EMPTY_BROWSE_ALL));
            }
        } else if (!access) {
            rows.add(LibraryAdapter.Row.empty(R.drawable.ic_folder_open,
                    getString(R.string.permission_title), getString(R.string.permission_message),
                    getString(R.string.permission_button), LibraryAdapter.EMPTY_GRANT_ACCESS));
        } else if (lastScan != null) {
            List<PdfFile> all = lastScan;
            if (openFolder != null) {
                List<PdfFile> files = PdfLibrary.filter(openFolder.files, query);
                for (PdfFile f : files) rows.add(LibraryAdapter.Row.file(f, false));
                summaryText = PdfLibrary.displayPath(openFolder.path);
            } else if (tab == TAB_FOLDERS) {
                List<PdfLibrary.Folder> folders = PdfLibrary.folders(PdfLibrary.filter(all, query), sort);
                for (PdfLibrary.Folder f : folders) rows.add(LibraryAdapter.Row.folder(f));
                summaryText = getResources().getQuantityString(R.plurals.pdf_count, all.size(), all.size());
            } else {
                List<PdfFile> files = PdfLibrary.filter(all, query);
                PdfLibrary.sort(files, sort);
                for (PdfFile f : files) rows.add(LibraryAdapter.Row.file(f, true));
                summaryText = getString(R.string.summary_count,
                        getResources().getQuantityString(R.plurals.pdf_count, all.size(), all.size()));
            }
            if (rows.size() == before && query.isEmpty()) {
                rows.add(LibraryAdapter.Row.empty(R.drawable.ic_document,
                        getString(R.string.empty_all_title), getString(R.string.empty_all_message),
                        null, LibraryAdapter.EMPTY_NO_ACTION));
            }
        }
        if (rows.size() == before && !query.isEmpty()) {
            rows.add(LibraryAdapter.Row.empty(R.drawable.ic_search,
                    getString(R.string.empty_search_title), getString(R.string.empty_search_message, query.trim()),
                    null, LibraryAdapter.EMPTY_NO_ACTION));
        }

        adapter.submit(rows);
        summary.setText(summaryText);
        summary.setVisibility(summaryText != null ? View.VISIBLE : View.GONE);
        if (openFolder != null) {
            toolbar.setTitle(openFolder.name);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        } else {
            toolbar.setTitle(R.string.app_name);
            toolbar.setNavigationIcon(null);
        }
        closeFolder.setEnabled(openFolder != null);
    }

    // --- LibraryAdapter.Callbacks

    @Override
    public void onFile(PdfFile file) {
        ViewerActivity.openFile(this, file.path, file.name);
    }

    @Override
    public void onFileMenu(PdfFile file, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.inflate(R.menu.menu_file);
        menu.getMenu().findItem(R.id.file_remove_recent).setVisible(false);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.file_open) {
                onFile(file);
            } else if (item.getItemId() == R.id.file_share) {
                Files.share(this, file.path, null, file.name);
            }
            return true;
        });
        menu.show();
    }

    @Override
    public void onRecent(RecentStore.Entry entry) {
        if (entry.path != null) {
            if (new File(entry.path).isFile()) {
                ViewerActivity.openFile(this, entry.path, entry.key, entry.name);
                return;
            }
            Toast.makeText(this, R.string.error_missing, Toast.LENGTH_SHORT).show();
            recents.remove(entry.key);
            render();
            return;
        }
        ViewerActivity.openUri(this, Uri.parse(entry.key), entry.key, entry.name);
    }

    @Override
    public void onRecentMenu(RecentStore.Entry entry, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.inflate(R.menu.menu_file);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.file_open) {
                onRecent(entry);
            } else if (id == R.id.file_share) {
                Files.share(this, entry.path, entry.path == null ? Uri.parse(entry.key) : null, entry.name);
            } else if (id == R.id.file_remove_recent) {
                recents.remove(entry.key);
                render();
            }
            return true;
        });
        menu.show();
    }

    @Override
    public void onFolder(PdfLibrary.Folder folder) {
        setOpenFolder(folder);
    }

    @Override
    public void onEmptyAction(int action) {
        if (action == LibraryAdapter.EMPTY_GRANT_ACCESS) {
            if (StorageAccess.usesSettingsScreen()) {
                StorageAccess.openSettingsScreen(this);
            } else {
                requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else if (action == LibraryAdapter.EMPTY_BROWSE_ALL) {
            tabs.check(R.id.tabAll);
        }
    }

    @Override
    public void onSetDefault() {
        DefaultViewer.makeDefault(this);
    }

    @Override
    public void onDismissDefault() {
        prefs.edit().putBoolean(KEY_DEFAULT_DISMISSED, true).apply();
        showDefaultBanner = false;
        render();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanner.shutdown();
    }
}
