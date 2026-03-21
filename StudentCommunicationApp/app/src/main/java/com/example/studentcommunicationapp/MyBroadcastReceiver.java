package com.example.studentcommunicationapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/*
 * MyBroadcastReceiver.java — Receives broadcast intents.
 *
 * KEY CONCEPTS EXPLAINED:
 *
 * 1. BroadcastReceiver is one of the four fundamental Android components:
 *      - Activity       → User interface screens
 *      - Service        → Background work without UI
 *      - ContentProvider → Share data between apps
 *      - BroadcastReceiver → Listen for system-wide or app-specific events
 *
 * 2. A BroadcastReceiver does NOT have a UI. It runs briefly, handles the
 *    broadcast, and finishes. The onReceive() method runs on the MAIN THREAD,
 *    so you must NOT do long-running work here (no network calls, no heavy
 *    database operations). If you need to do heavy work, start a Service
 *    or use WorkManager from within onReceive().
 *
 * 3. The system gives you roughly 10 seconds in onReceive(). After that,
 *    the system considers your receiver stuck and may kill your process.
 *
 * 4. Registration methods:
 *      a) STATIC (in AndroidManifest.xml):
 *         - Receiver is active even when the app is not running
 *         - Limited to certain broadcast types after Android 8.0 (API 26)
 *         - Good for: boot completed, package installed, etc.
 *
 *      b) DYNAMIC (in Java code via registerReceiver()):
 *         - Receiver is active only while the registering component is alive
 *         - Must be unregistered manually to avoid leaks
 *         - Good for: UI updates, temporary listening
 *
 *    In this project, we demonstrate BOTH methods.
 *
 * 5. SECURITY NOTE: From Android 14 (API 34), you must specify whether a
 *    dynamically registered receiver is exported (visible to other apps) or
 *    not. We use RECEIVER_NOT_EXPORTED for safety.
 */
public class MyBroadcastReceiver extends BroadcastReceiver {

    /*
     * onReceive() is called when a broadcast matching our IntentFilter arrives.
     *
     * PARAMETERS:
     *   context — The Context in which the receiver is running.
     *             Use this for Toast, starting services, etc.
     *             IMPORTANT: This is NOT an Activity context. You cannot
     *             use it to show dialogs or start Activities directly
     *             without adding FLAG_ACTIVITY_NEW_TASK.
     *
     *   intent  — The Intent that was broadcast. Contains:
     *             - The action string (which broadcast was sent)
     *             - Any extras (data) attached by the sender
     *
     * CAREFUL: This method runs on the MAIN (UI) thread. Keep it fast.
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        // Retrieve the message from the broadcast intent's extras.
        // The key "BROADCAST_MESSAGE" must match what the sender used.
        String message = intent.getStringExtra("BROADCAST_MESSAGE");

        // If no message was attached, use a fallback string.
        // This is defensive coding — always handle the null case.
        if (message == null) {
            message = "Broadcast received (no message)";
        }

        // Show the message as a Toast.
        // Toast.makeText() creates the Toast; .show() displays it.
        // COMMON MISTAKE: Forgetting to call .show() — the Toast is
        // created but never displayed. Always chain .show() at the end.
        Toast.makeText(context, "Broadcast: " + message, Toast.LENGTH_LONG).show();
    }
}
