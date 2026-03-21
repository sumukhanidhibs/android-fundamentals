package com.example.studeninfo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * StudentService is a background Service.
 *
 * Context here = Service Context (this)
 * WHY: Services run in the background — they are NOT tied to any Activity.
 * The 'this' keyword inside a Service returns Service Context.
 *
 * IMPORTANT: Services run on the MAIN thread by default.
 * For real background work, use IntentService or WorkManager.
 */
public class StudentService extends Service {

    private static final String TAG = "StudentService";

    /**
     * Called when the Service is first created.
     * Service Context is available from this point.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created — Service Context: " + this.getClass().getName());
    }

    /**
     * Called every time startService() is called.
     * This is where your background work happens.
     *
     * Context used here: Service Context (this)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "Service Started — Running background task...");

        // Fetch student data using Service Context (Application Context via getApplicationContext())
        // WHY: We use getApplicationContext() inside Service to access global app data
        MyApplication myApp = (MyApplication) getApplicationContext();
        String studentName = myApp.getStudentName();

        // Toast using Service Context
        // NOTE: Toast from Service works but is generally discouraged in production.
        // Use Notification instead for background alerts.
        Toast.makeText(
                this,  // Service Context
                "Service Running for: " + studentName,
                Toast.LENGTH_LONG
        ).show();

        Log.d(TAG, "Processing data for student: " + studentName);

        /*
         * START_NOT_STICKY: If Android kills this service to free memory,
         * it will NOT restart it automatically.
         * This is safe for simple one-time tasks.
         */
        return START_NOT_STICKY;
    }

    /**
     * onBind() is required to implement but we return null
     * because this is a STARTED service, not a BOUND service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed — Service Context released");
    }
}