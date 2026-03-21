package com.example.lifecyclelogger;

// Standard Android imports
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // TAG is used with Log.d() to filter logs in Logcat
    // Always define a TAG constant — it's best practice
    private static final String TAG = "LifecycleLogger_Main";

    // UI components
    private TextView tvLog;       // displays lifecycle logs on screen
    private ScrollView scrollView; // allows the log to scroll
    private Button btnOpenSecond;  // navigates to SecondActivity
    private Button btnClearLog;    // clears the log TextView

    // ─────────────────────────────────────────────
    // onCreate() — Activity is first created
    // ─────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ALWAYS call super.onCreate() first — it initializes the
        // Android framework components for this activity
        super.onCreate(savedInstanceState);

        // Inflate the XML layout and attach it to this Activity
        // From this point, all XML views are loaded into memory
        setContentView(R.layout.activity_main);

        // ── Initialize views by finding them using their XML IDs ──
        tvLog = findViewById(R.id.tvLog);
        scrollView = findViewById(R.id.scrollView);
        btnOpenSecond = findViewById(R.id.btnOpenSecond);
        btnClearLog = findViewById(R.id.btnClearLog);

        // ── Log the onCreate event ──
        appendLog("onCreate() called ✅ — Activity CREATED");
        Log.d(TAG, "onCreate() called");
        showToast("onCreate()");

        // ── Button: Navigate to SecondActivity ──
        btnOpenSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent is used to navigate between Activities
                // 'MainActivity.this' is the source context
                // 'SecondActivity.class' is the destination
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);
            }
        });

        // ── Button: Clear the log TextView ──
        btnClearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvLog.setText(""); // reset text to empty
            }
        });
    }

    // ─────────────────────────────────────────────
    // onStart() — Activity becomes visible
    // ─────────────────────────────────────────────
    @Override
    protected void onStart() {
        super.onStart(); // always call super first
        appendLog("onStart() called 👁️ — Activity VISIBLE");
        Log.d(TAG, "onStart() called");
        showToast("onStart()");
    }

    // ─────────────────────────────────────────────
    // onResume() — Activity is interactive (foreground)
    // ─────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        appendLog("onResume() called ▶️ — Activity RUNNING (foreground)");
        Log.d(TAG, "onResume() called");
        showToast("onResume()");
    }

    // ─────────────────────────────────────────────
    // onPause() — Activity partially hidden
    // Called when another activity comes to the front
    // or when Home is pressed
    // ─────────────────────────────────────────────
    @Override
    protected void onPause() {
        super.onPause();
        appendLog("onPause() called ⏸️ — Activity PAUSED");
        Log.d(TAG, "onPause() called");
        // Note: We do NOT show Toast here intentionally
        // because Toast requires UI context which may not be reliable
        // after the activity loses focus — but it still works in demos
        showToast("onPause()");
    }

    // ─────────────────────────────────────────────
    // onStop() — Activity completely hidden
    // ─────────────────────────────────────────────
    @Override
    protected void onStop() {
        super.onStop();
        appendLog("onStop() called 🛑 — Activity STOPPED");
        Log.d(TAG, "onStop() called");
        showToast("onStop()");
    }

    // ─────────────────────────────────────────────
    // onRestart() — Called after onStop(), before onStart()
    // when user returns to this activity
    // ─────────────────────────────────────────────
    @Override
    protected void onRestart() {
        super.onRestart();
        appendLog("onRestart() called 🔄 — Activity RESTARTING");
        Log.d(TAG, "onRestart() called");
        showToast("onRestart()");
    }

    // ─────────────────────────────────────────────
    // onDestroy() — Activity is being destroyed permanently
    // ─────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // At this point, the activity is being torn down
        // We still log it — but the UI may already be removed
        appendLog("onDestroy() called ❌ — Activity DESTROYED");
        Log.d(TAG, "onDestroy() called");
        // Toast is skipped here intentionally because the
        // Activity window may already be detached from the WindowManager
    }

    // ─────────────────────────────────────────────
    // HELPER METHOD: appendLog()
    // Appends a new line to the tvLog TextView
    // and scrolls to the bottom automatically
    // ─────────────────────────────────────────────
    private void appendLog(String message) {
        // getText() returns the current text, we append the new message
        // "\n" adds a newline so each log appears on a new line
        tvLog.setText(tvLog.getText() + "\n" + message);

        // post() runs the scroll AFTER the layout is updated
        // Without this, scrollTo might run before new text is drawn
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                // Scroll to the very bottom of the ScrollView
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    // ─────────────────────────────────────────────
    // HELPER METHOD: showToast()
    // Shows a short Toast message on screen
    // ─────────────────────────────────────────────
    private void showToast(String methodName) {
        // Toast.LENGTH_SHORT shows for ~2 seconds
        Toast.makeText(this, "MainActivity → " + methodName, Toast.LENGTH_SHORT).show();
    }
}