package com.example.servicedemo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class StartedService extends Service {

    // TAG is used for filtering logs in Logcat
    private static final String TAG = "StartedService";

    // Handler lets us post delayed/repeated tasks on the main thread
    // We use the main looper so Toast works (Toast must run on UI thread)
    private Handler handler;

    // Runnable holds the repeating task logic
    private Runnable backgroundTask;

    // Counter to track how many times the task has run
    private int taskCount = 0;

    // Flag to control whether the loop should continue
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // onCreate() is called ONCE when service is first created
        // Initialize our Handler here — it's tied to the main thread looper
        handler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "onCreate: Service created for the first time");
        Toast.makeText(this, "Started Service Created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // This is called EVERY time startService() is called
        // This is where we begin our actual background work

        Log.d(TAG, "onStartCommand: Service received start command. StartId = " + startId);

        // Prevent starting multiple loops if service is already running
        if (!isRunning) {
            isRunning = true;
            taskCount = 0;

            // Define the repeating task using a Runnable
            backgroundTask = new Runnable() {
                @Override
                public void run() {
                    if (isRunning) {
                        taskCount++;
                        String message = "Background Task Running... Count: " + taskCount;

                        // Log to Logcat (visible in Android Studio's Logcat panel)
                        Log.d(TAG, message);

                        // Toast on UI thread — Handler ensures this is safe
                        Toast.makeText(StartedService.this, message, Toast.LENGTH_SHORT).show();

                        // Schedule this same Runnable to run again after 3 seconds
                        // postDelayed(runnable, milliseconds)
                        handler.postDelayed(this, 3000);
                    }
                }
            };

            // Kick off the first execution immediately
            handler.post(backgroundTask);
        }

        // START_STICKY: If Android kills this service due to low memory,
        // it will restart it automatically (with intent = null)
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Started Services do NOT support binding
        // So we return null here — this tells Android "no binding supported"
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // onDestroy is our FINAL chance to clean up resources
        // If we don't stop the handler here, it will keep running
        // even after the service is destroyed → MEMORY LEAK!

        isRunning = false;

        // Remove all pending callbacks from the handler queue
        if (handler != null && backgroundTask != null) {
            handler.removeCallbacks(backgroundTask);
        }

        Log.d(TAG, "onDestroy: Service destroyed. Total tasks completed: " + taskCount);
        Toast.makeText(this, "Started Service Destroyed", Toast.LENGTH_SHORT).show();
    }
}