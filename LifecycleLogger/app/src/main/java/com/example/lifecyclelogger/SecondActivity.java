package com.example.lifecyclelogger;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SecondActivity extends AppCompatActivity {

    // Separate TAG so we can distinguish logs from MainActivity vs SecondActivity
    private static final String TAG = "LifecycleLogger_Second";

    private TextView tvSecondLog;
    private ScrollView scrollViewSecond;
    private Button btnGoBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // Initialize views
        tvSecondLog = findViewById(R.id.tvSecondLog);
        scrollViewSecond = findViewById(R.id.scrollViewSecond);
        btnGoBack = findViewById(R.id.btnGoBack);

        appendLog("onCreate() called ✅ — SecondActivity CREATED");
        Log.d(TAG, "onCreate() called");
        showToast("onCreate()");

        // finish() is the correct way to programmatically go back
        // It destroys the current activity and returns to the previous one in the back stack
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // This triggers: onPause → onStop → onDestroy for SecondActivity
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        appendLog("onStart() called 👁️ — SecondActivity VISIBLE");
        Log.d(TAG, "onStart() called");
        showToast("onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        appendLog("onResume() called ▶️ — SecondActivity RUNNING");
        Log.d(TAG, "onResume() called");
        showToast("onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        appendLog("onPause() called ⏸️ — SecondActivity PAUSED");
        Log.d(TAG, "onPause() called");
        showToast("onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        appendLog("onStop() called 🛑 — SecondActivity STOPPED");
        Log.d(TAG, "onStop() called");
        showToast("onStop()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        appendLog("onRestart() called 🔄 — SecondActivity RESTARTING");
        Log.d(TAG, "onRestart() called");
        showToast("onRestart()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appendLog("onDestroy() called ❌ — SecondActivity DESTROYED");
        Log.d(TAG, "onDestroy() called");
    }

    private void appendLog(String message) {
        tvSecondLog.setText(tvSecondLog.getText() + "\n" + message);
        scrollViewSecond.post(new Runnable() {
            @Override
            public void run() {
                scrollViewSecond.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void showToast(String methodName) {
        Toast.makeText(this, "SecondActivity → " + methodName, Toast.LENGTH_SHORT).show();
    }
}