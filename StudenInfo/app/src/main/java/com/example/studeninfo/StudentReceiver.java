package com.example.studeninfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * StudentReceiver listens for our custom broadcast.
 *
 * Context here = BroadcastReceiver Context (passed as parameter)
 *
 * CRITICAL RULE: BroadcastReceiver Context is SHORT-LIVED.
 * It is valid ONLY during the onReceive() method.
 * Never store this context in a variable outside onReceive().
 * Never start async operations using this context.
 */
public class StudentReceiver extends BroadcastReceiver {

    private static final String TAG = "StudentReceiver";

    /**
     * This method is called when the broadcast is received.
     *
     * @param context - BroadcastReceiver Context (very short-lived!)
     * @param intent  - The Intent that triggered this receiver
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "Broadcast received!");
        Log.d(TAG, "Receiver Context: " + context.getClass().getName());

        // Check which action triggered this broadcast
        if (MainActivity.ACTION_STUDENT_BROADCAST.equals(intent.getAction())) {

            /*
             * We access Application Context from within BroadcastReceiver.
             * WHY: BroadcastReceiver's own context is short-lived,
             * but Application Context is always safe to use.
             */
            MyApplication myApp = (MyApplication) context.getApplicationContext();
            String studentName = myApp.getStudentName();

            // Toast using the receiver's context
            // This is safe because we're still inside onReceive()
            Toast.makeText(
                    context, // BroadcastReceiver Context
                    "Broadcast Received! Student: " + studentName,
                    Toast.LENGTH_LONG
            ).show();

            Log.d(TAG, "Broadcast handled for student: " + studentName);
        }

        /*
         * ⚠️ IMPORTANT WARNING FOR STUDENTS:
         * Do NOT do this:
         *   someObject.setContext(context); ← WRONG! Context is dead after this method
         *
         * Do NOT do this:
         *   new AsyncTask().execute(); ← WRONG! Unreliable in BroadcastReceiver
         *
         * If you need async work, use goAsync() or start a Service instead.
         */
    }
}