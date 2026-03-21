package com.example.studeninfo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * MainActivity demonstrates:
 * 1. Application Context — accessing global student data
 * 2. Activity Context — Toast, Intent
 * 3. View Context — inside button's onClick using v.getContext()
 */
public class MainActivity extends AppCompatActivity {

    // UI Components
    private TextView tvWelcome;
    private Button btnShowInfo;
    private Button btnStartService;
    private Button btnSendBroadcast;
    private Button btnLoadFragment;

    // TAG for Logcat
    private static final String TAG = "MainActivity";

    // Custom broadcast action string
    public static final String ACTION_STUDENT_BROADCAST =
            "com.example.studentinfo.STUDENT_ACTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        initViews();

        // ── APPLICATION CONTEXT USAGE ─────────────────────────────
        // We cast getApplicationContext() to MyApplication
        // to access our custom global variables.
        // WHY Application Context: This data lives globally, not tied to this screen.
        MyApplication myApp = (MyApplication) getApplicationContext();
        String studentName = myApp.getStudentName();

        // Display student name in welcome text
        tvWelcome.setText("Welcome, " + studentName + "!");

        Log.d(TAG, "App Context Class: " + getApplicationContext().getClass().getName());
        Log.d(TAG, "Activity Context Class: " + this.getClass().getName());

        // ── BUTTON 1: Navigate to DisplayActivity ─────────────────
        btnShowInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                 * ACTIVITY CONTEXT used here via 'MainActivity.this'
                 * WHY: Intent requires a context to know WHICH app is launching
                 * the next screen. Activity context is correct here.
                 *
                 * VIEW CONTEXT: v.getContext() also works here and returns
                 * the same Activity context.
                 */

                // Show Toast using Activity Context (this)
                Toast.makeText(MainActivity.this,
                        "Opening Student Info...",
                        Toast.LENGTH_SHORT).show();

                // Start DisplayActivity using Activity Context
                Intent intent = new Intent(MainActivity.this, DisplayActivity.class);
                startActivity(intent);
            }
        });

        // ── BUTTON 2: Start Background Service ───────────────────
        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                 * ACTIVITY CONTEXT used here.
                 * WHY: startService() needs context to resolve the component
                 * and bind it to the Android system.
                 */

                Intent serviceIntent = new Intent(MainActivity.this, StudentService.class);
                startService(serviceIntent);

                // Toast using Activity Context
                Toast.makeText(MainActivity.this,
                        "Background Service Started!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // ── BUTTON 3: Send Broadcast ──────────────────────────────
        btnSendBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                 * ACTIVITY CONTEXT used here.
                 * WHY: sendBroadcast() is an OS-level operation that requires
                 * context to route the broadcast correctly.
                 *
                 * VIEW CONTEXT DEMO: Using v.getContext() instead of MainActivity.this
                 * Both return Activity Context in this scenario.
                 */

                Context viewContext = v.getContext(); // View Context demo
                Intent broadcastIntent = new Intent(ACTION_STUDENT_BROADCAST);
                v.getContext().sendBroadcast(broadcastIntent);

                Toast.makeText(MainActivity.this,
                        "Broadcast Sent! (View Context used)",
                        Toast.LENGTH_SHORT).show();

                Log.d(TAG, "View Context: " + viewContext.getClass().getName());
            }
        });

        // ── BUTTON 4: Load Fragment ───────────────────────────────
        btnLoadFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                 * Fragment is loaded into a container in this Activity.
                 * Inside the Fragment, getContext() will return THIS Activity's context.
                 * This demonstrates Fragment Context indirectly.
                 */

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();

                StudentFragment fragment = new StudentFragment();
                transaction.replace(R.id.fragmentContainer, fragment);
                transaction.addToBackStack(null); // Allow back navigation
                transaction.commit();

                Toast.makeText(MainActivity.this,
                        "Fragment Loaded!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Initialize all UI components by finding them by ID
     * WHY: We separate this into its own method to keep onCreate() clean.
     * This is a good coding practice — single responsibility.
     */
    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnShowInfo = findViewById(R.id.btnShowInfo);
        btnStartService = findViewById(R.id.btnStartService);
        btnSendBroadcast = findViewById(R.id.btnSendBroadcast);
        btnLoadFragment = findViewById(R.id.btnLoadFragment);
    }

    // ── Activity Lifecycle Logs ───────────────────────────────────
    // These help you observe when Activity Context is created/destroyed

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called — Activity becoming visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called — Activity in foreground");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // When this is called, Activity Context is destroyed!
        // Any reference to 'this' after this point = memory leak
        Log.d(TAG, "onDestroy() called — Activity Context DESTROYED");
    }
}