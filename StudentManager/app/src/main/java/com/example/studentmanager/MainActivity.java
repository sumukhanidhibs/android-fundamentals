package com.example.studentmanager;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity — The LAUNCHER activity.
 *
 * WHY THIS WORKS:
 * The manifest declares this activity with an <intent-filter> containing
 * MAIN + LAUNCHER. When the user taps the app icon, Android reads the
 * manifest, finds this filter, and starts this Activity.
 *
 * This class also demonstrates how to READ <meta-data> from the manifest
 * at runtime using PackageManager.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvVersionLabel;
    private Button btnOpenDetails;
    private Button btnStartSync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Initialize views ---
        tvVersionLabel = findViewById(R.id.tvVersionLabel);
        btnOpenDetails = findViewById(R.id.btnOpenDetails);
        btnStartSync = findViewById(R.id.btnStartSync);

        // --- Read metadata from AndroidManifest.xml ---
        // This shows how <meta-data> tags are accessed at runtime.
        // Libraries like Firebase and Google Maps do the same thing internally.
        readManifestMetadata();

        // --- Navigate to SecondActivity ---
        // This works because SecondActivity is DECLARED in the manifest.
        // If you remove its <activity> tag, this line causes a CRASH:
        // android.content.ActivityNotFoundException
        btnOpenDetails.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra("student_name", "Rahul Sharma");
            startActivity(intent);
        });

        // --- Start the background sync service ---
        // This works because DataSyncService is DECLARED in the manifest.
        // If you remove its <service> tag, this line causes a CRASH:
        // java.lang.IllegalStateException
        btnStartSync.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(MainActivity.this, DataSyncService.class);
            startService(serviceIntent);
            Toast.makeText(this, "Sync started in background...", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Reads the "app_version_label" metadata from the manifest.
     *
     * HOW IT WORKS:
     * 1. PackageManager retrieves the ApplicationInfo for our own app.
     * 2. The metaData Bundle inside ApplicationInfo contains all <meta-data>
     *    key-value pairs declared inside <application>.
     * 3. We fetch the value using the key name we defined in the manifest.
     *
     * WHY THIS IS USEFUL:
     * Libraries use this exact mechanism. When Google Maps reads your API key,
     * it calls the same PackageManager methods to find the <meta-data> tag
     * with name "com.google.android.geo.API_KEY".
     */
    private void readManifestMetadata() {
        try {
            ApplicationInfo appInfo = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);

            if (appInfo.metaData != null) {
                String versionLabel = appInfo.metaData.getString("app_version_label");
                tvVersionLabel.setText(versionLabel);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // This should never happen since we're querying our OWN package.
            // But always handle exceptions — defensive coding is a best practice.
            tvVersionLabel.setText("Version info unavailable");
        }
    }
}
