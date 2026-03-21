package com.example.studentcommunicationapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/*
 * SecondActivity.java — Receives data from MainActivity via an Explicit Intent.
 *
 * KEY CONCEPTS EXPLAINED:
 *
 * 1. getIntent() — Every Activity has access to the Intent that started it.
 *    This is how you retrieve data sent by the calling Activity.
 *    getIntent() never returns null for an Activity (it always has an Intent),
 *    but the extras inside it might be null if nothing was passed.
 *
 * 2. getStringExtra() / getIntExtra() — Typed convenience methods to pull
 *    values out of the Intent's extras Bundle. They handle null-safety for
 *    you (getStringExtra returns null if key missing, getIntExtra returns
 *    a default value you specify).
 *
 * 3. IMPORTANT: The keys used here ("STUDENT_NAME", "STUDENT_ID") must
 *    EXACTLY match the keys used in MainActivity's putExtra() calls.
 *    A common bug is mismatched keys — the app won't crash, but
 *    you'll get null/default values silently. Be careful with spelling.
 *
 *    PRO TIP: Define keys as public static final constants in one place
 *    (e.g., in MainActivity or a separate Constants class) and reference
 *    them from both Activities. This prevents typos.
 */
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout for this Activity.
        setContentView(R.layout.activity_second);

        // Find the TextViews where we'll display the received data.
        TextView tvStudentName = findViewById(R.id.tvStudentName);
        TextView tvStudentId = findViewById(R.id.tvStudentId);

        // ── Retrieve the Intent that started this Activity ──────────
        // getIntent() returns the Intent object that was used to launch
        // this Activity. It's the same object created in MainActivity.
        Intent receivedIntent = getIntent();

        // ── Extract data from the Intent's extras ───────────────────
        // getStringExtra(key):
        //   - Returns the String value associated with the key
        //   - Returns null if the key doesn't exist
        //   - CAREFUL: If you call .toString() or .length() on a null
        //     result, you'll get a NullPointerException. Always check!
        String studentName = receivedIntent.getStringExtra("STUDENT_NAME");

        // getIntExtra(key, defaultValue):
        //   - Returns the int value associated with the key
        //   - Returns defaultValue (here, 0) if the key doesn't exist
        //   - Unlike getStringExtra, this can't return null because int
        //     is a primitive type in Java, not an object.
        int studentId = receivedIntent.getIntExtra("STUDENT_ID", 0);

        // ── Display the data ────────────────────────────────────────
        // Null-check the String before using it. This is defensive
        // programming — even if you're "sure" the data was sent, always
        // guard against null. The calling Activity might change later.
        if (studentName != null) {
            tvStudentName.setText("Student Name: " + studentName);
        } else {
            tvStudentName.setText("Student Name: Not provided");
        }

        tvStudentId.setText("Student ID: " + studentId);
    }
}
