package com.example.studentmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;

/**
 * NotificationReceiver — A BroadcastReceiver component.
 *
 * WHY DECLARE IN MANIFEST:
 * The manifest's <receiver> tag with an <intent-filter> tells Android:
 * "When a broadcast with action STUDENT_ADDED is sent, create an instance
 *  of this class and call its onReceive() method."
 *
 * Without the manifest declaration, this class is just a regular Java class.
 * Android has no idea it should be called when a broadcast is fired.
 *
 * HOW THE CHAIN WORKS:
 * 1. User taps "Start Sync" in MainActivity
 * 2. MainActivity calls startService() → DataSyncService starts
 * 3. DataSyncService finishes sync → calls sendBroadcast() with action STUDENT_ADDED
 * 4. Android checks the manifest → finds NotificationReceiver listens for that action
 * 5. Android instantiates NotificationReceiver and calls onReceive()
 * 6. onReceive() shows a Toast to the user
 *
 * IMPORTANT CONSTRAINT:
 * onReceive() runs on the MAIN thread and must complete within 10 seconds.
 * If you need to do long work inside a receiver, start a Service or use
 * goAsync() to get extra time. Never do network calls or database writes
 * directly inside onReceive().
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Verify the action matches what we expect.
        // This is a safety check — Android should only deliver matching actions,
        // but defensive coding prevents bugs if the filter changes.
        if (DataSyncService.ACTION_STUDENT_ADDED.equals(intent.getAction())) {
            Log.d(TAG, "Received STUDENT_ADDED broadcast");

            Toast.makeText(
                    context,
                    "Student data synced successfully!",
                    Toast.LENGTH_LONG
            ).show();

            // In a real app, you would show a proper Notification here
            // using NotificationManager and NotificationChannel (API 26+).
        }
    }
}
