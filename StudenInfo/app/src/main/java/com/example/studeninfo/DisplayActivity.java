package com.example.studeninfo;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * DisplayActivity shows full student information.
 *
 * Context used here:
 * - Application Context → to fetch global student data
 * - Activity Context (this) → for Toast
 */
public class DisplayActivity extends AppCompatActivity {

    private TextView tvName;
    private TextView tvEmail;
    private TextView tvCourse;
    private TextView tvContextInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        // Initialize views
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvCourse = findViewById(R.id.tvCourse);
        tvContextInfo = findViewById(R.id.tvContextInfo);

        // ── APPLICATION CONTEXT: Fetch global student data ────────
        // We don't pass data through Intent here — instead we fetch
        // it directly from the Application class.
        // This is a key advantage of Application Context.
        MyApplication myApp = (MyApplication) getApplicationContext();

        // Display the global data in this new Activity
        tvName.setText("Name: " + myApp.getStudentName());
        tvEmail.setText("Email: " + myApp.getStudentEmail());
        tvCourse.setText("Course: " + myApp.getStudentCourse());

        // Show context class names for student learning
        String contextInfo =
                "App Context: " + getApplicationContext().getClass().getSimpleName()
                        + "\nActivity Context: " + this.getClass().getSimpleName();
        tvContextInfo.setText(contextInfo);

        // ── ACTIVITY CONTEXT: Toast ───────────────────────────────
        // 'this' here refers to DisplayActivity — Activity Context
        Toast.makeText(this,
                "Data loaded from Application Context!",
                Toast.LENGTH_LONG).show();
    }
}