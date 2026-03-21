package com.example.servicedemo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class BoundService extends Service {

    private static final String TAG = "BoundService";

    // IBinder is the communication channel between Service and Activity
    // We create a custom Binder subclass to expose our service methods
    private final IBinder binder = new LocalBinder();

    private int counter = 0;
    private Handler counterHandler;
    private Runnable counterRunnable;

    // -------------------------------------------------------
    // LocalBinder is an inner class that extends Binder
    // It acts as a BRIDGE — Activity uses this to call service methods
    // Because it's an inner class, it has direct access to BoundService fields
    // -------------------------------------------------------
    public class LocalBinder extends Binder {
        // This method gives the Activity a reference to the BoundService instance
        public BoundService getService() {
            return BoundService.this;
        }
    }

    // Public method that Activity can call to get current counter value
    // This is the "service API" exposed to clients
    public int getCounter() {
        return counter;
    }

    // Public method to get a status message
    public String getStatusMessage() {
        return "Bound Service is active. Counter = " + counter;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Start incrementing the counter in the background
        // This simulates the service doing ongoing work
        counterHandler = new Handler(Looper.getMainLooper());

        counterRunnable = new Runnable() {
            @Override
            public void run() {
                counter++;
                Log.d(TAG, "Counter incremented to: " + counter);

                // Schedule next increment after 1 second
                counterHandler.postDelayed(this, 1000);
            }
        };

        // Start the counter immediately
        counterHandler.post(counterRunnable);

        Log.d(TAG, "onCreate: Bound Service created");
        Toast.makeText(this, "Bound Service Created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // onBind() is called when an Activity calls bindService()
        // We return our LocalBinder instance here
        // The Activity will receive this in ServiceConnection.onServiceConnected()
        Log.d(TAG, "onBind: A client has bound to this service");
        Toast.makeText(this, "Client Bound to Service", Toast.LENGTH_SHORT).show();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Called when ALL clients have unbound from this service
        Log.d(TAG, "onUnbind: All clients unbound");
        Toast.makeText(this, "All Clients Unbound", Toast.LENGTH_SHORT).show();

        // Return TRUE to allow onRebind() to be called if a new client binds later
        // If we return false, onBind() would be called instead of onRebind()
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        // Called when a new client binds AFTER onUnbind() returned true
        Log.d(TAG, "onRebind: A new client rebound to service");
        Toast.makeText(this, "Client Rebound to Service", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop the background counter — prevent memory leak
        if (counterHandler != null && counterRunnable != null) {
            counterHandler.removeCallbacks(counterRunnable);
        }

        Log.d(TAG, "onDestroy: Bound Service destroyed. Final counter = " + counter);
        Toast.makeText(this, "Bound Service Destroyed", Toast.LENGTH_SHORT).show();
    }
}