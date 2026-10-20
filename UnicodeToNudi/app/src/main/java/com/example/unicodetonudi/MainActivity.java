package com.example.unicodetonudi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Type or paste Kannada Unicode at the top, the Nudi (ASCII) version
 * appears live underneath.
 *
 * "Automatic" flow:
 *   1. Copy Kannada Unicode text anywhere (WhatsApp, browser, ...)
 *   2. Open this app → it picks up the clipboard and converts it
 *   3. With auto-copy on, the Nudi text is already on the clipboard → paste it
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "settings";
    private static final String KEY_AUTO_COPY = "auto_copy";
    private static final String KEY_CLIPBOARD_CHECKED = "clipboard_checked";
    private static final long AUTO_COPY_DELAY_MS = 600;

    private EditText inputUnicode;
    private TextView outputNudi;
    private CheckBox checkAutoCopy;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoCopyTask = () -> copyOutput(false);

    // Clipboard can only be read once the window has focus (Android 10+),
    // so the "import from clipboard on launch" happens in onWindowFocusChanged.
    private boolean clipboardChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputUnicode = findViewById(R.id.inputUnicode);
        outputNudi = findViewById(R.id.outputNudi);
        checkAutoCopy = findViewById(R.id.checkAutoCopy);
        Button btnPaste = findViewById(R.id.btnPaste);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnCopy = findViewById(R.id.btnCopy);
        Button btnShare = findViewById(R.id.btnShare);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        checkAutoCopy.setChecked(prefs.getBoolean(KEY_AUTO_COPY, true));
        checkAutoCopy.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(KEY_AUTO_COPY, checked).apply());

        inputUnicode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                outputNudi.setText(NudiConverter.convert(s.toString()));
                handler.removeCallbacks(autoCopyTask);
                if (checkAutoCopy.isChecked() && s.length() > 0) {
                    handler.postDelayed(autoCopyTask, AUTO_COPY_DELAY_MS);
                }
            }
        });

        btnPaste.setOnClickListener(v -> pasteFromClipboard());
        btnClear.setOnClickListener(v -> inputUnicode.setText(""));
        btnCopy.setOnClickListener(v -> copyOutput(true));
        btnShare.setOnClickListener(v -> shareOutput());

        if (savedInstanceState != null) {
            clipboardChecked = savedInstanceState.getBoolean(KEY_CLIPBOARD_CHECKED);
        } else if (handleSharedText(getIntent())) {
            clipboardChecked = true;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSharedText(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !clipboardChecked) {
            clipboardChecked = true;
            if (inputUnicode.length() == 0) {
                String clip = ClipboardUtils.read(this);
                if (NudiConverter.containsKannada(clip)) {
                    inputUnicode.setText(clip);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CLIPBOARD_CHECKED, clipboardChecked);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(autoCopyTask);
        super.onDestroy();
    }

    /** Text shared from another app via Share → Unicode to Nudi. */
    private boolean handleSharedText(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared != null) {
                inputUnicode.setText(shared);
                return true;
            }
        }
        return false;
    }

    private void pasteFromClipboard() {
        String clip = ClipboardUtils.read(this);
        if (clip == null || clip.isEmpty()) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NudiConverter.containsKannada(clip)) {
            Toast.makeText(this, R.string.no_kannada, Toast.LENGTH_SHORT).show();
        }
        inputUnicode.setText(clip);
        inputUnicode.setSelection(inputUnicode.length());
    }

    private void copyOutput(boolean showMessage) {
        String nudi = outputNudi.getText().toString();
        if (nudi.isEmpty()) {
            if (showMessage) {
                Toast.makeText(this, R.string.nothing_to_copy, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        ClipboardUtils.copy(this, getString(R.string.label_nudi), nudi);
        if (showMessage) {
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareOutput() {
        String nudi = outputNudi.getText().toString();
        if (nudi.isEmpty()) {
            Toast.makeText(this, R.string.nothing_to_copy, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, nudi);
        startActivity(Intent.createChooser(send, getString(R.string.share_title)));
    }
}
