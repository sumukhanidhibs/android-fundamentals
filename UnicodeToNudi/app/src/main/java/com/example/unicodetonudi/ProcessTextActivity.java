package com.example.unicodetonudi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Shows up as "Copy as Nudi" in the text-selection menu of any app
 * (Intent.ACTION_PROCESS_TEXT, Android 6.0+).
 *
 * It has no UI: it converts the selected text, puts the Nudi version on the
 * clipboard and finishes immediately, leaving the user in the original app.
 */
public class ProcessTextActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CharSequence selected = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if (selected != null && selected.length() > 0) {
            String nudi = NudiConverter.convert(selected.toString());
            ClipboardUtils.copy(this, getString(R.string.label_nudi), nudi);
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.nothing_to_copy, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
