package com.example.studentmanager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * DataSyncService — A background Service component.
 *
 * WHY DECLARE IN MANIFEST:
 * Android's component system requires EVERY Service to be registered
 * in AndroidManifest.xml inside the <application> tag. If you skip
 * the declaration, calling startService() throws:
 *     IllegalStateException: Not allowed to start service Intent {...}
 *
 * WHAT THIS SERVICE DOES:
 * 1. Simulates a 3-second data sync operation (using a background thread).
 * 2. Once complete, sends a custom broadcast with action
 *    "com.example.studentmanager.STUDENT_ADDED".
 * 3. The NotificationReceiver (declared in the manifest) catches this
 *    broadcast and can show a notification or Toast.
 *
 * WHY A SEPARATE THREAD:
 * Services run on the MAIN (UI) thread by default. If you do heavy work
 * directly in onStartCommand(), the UI freezes and Android may show an
 * "App Not Responding" (ANR) dialog after 5 seconds. Always offload
 * heavy work to a background thread.
 */
public class DataSyncService extends Service {

    private static final String TAG = "DataSyncService";

    // Custom broadcast action — must match EXACTLY what the manifest declares
    // in the receiver's <intent-filter>. A single typo breaks the connection.
    public static final String ACTION_STUDENT_ADDED =
            "com.example.studentmanager.STUDENT_ADDED";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Sync service started");

        // Run sync work on a background thread to avoid blocking the UI.
        new Thread(() -> {
            try {
                // Simulate network sync (3 seconds)
                Thread.sleep(3000);
                Log.d(TAG, "Sync complete");

                // Send a broadcast to notify the app that sync is done.
                // The NotificationReceiver in the manifest listens for this action.
                Intent broadcastIntent = new Intent(ACTION_STUDENT_ADDED);

                // IMPORTANT: Set the package to restrict this broadcast to our app only.
                // Without this, on some Android versions, the broadcast might not
                // reach manifest-declared receivers due to security restrictions.
                broadcastIntent.setPackage(getPackageName());

                sendBroadcast(broadcastIntent);

            } catch (InterruptedException e) {
                Log.e(TAG, "Sync interrupted", e);
                Thread.currentThread().interrupt(); // Restore interrupt flag
            }

            // Stop the service after work is done.
            // If you forget this, the service stays alive consuming resources.
            stopSelf();
        }).start();

        // START_NOT_STICKY means: if the system kills this service to free memory,
        // do NOT restart it automatically. This is appropriate for one-time sync tasks.
        // Use START_STICKY for services that should always be running (like music players).
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We are not binding to this service from any Activity.
        // Return null for started services (not bound services).
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Sync service destroyed");
    }
}
