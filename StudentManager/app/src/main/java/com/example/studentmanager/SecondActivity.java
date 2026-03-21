package com.example.studentmanager;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SecondActivity — An INTERNAL activity (no intent-filter).
 *
 * WHY exported="false" IN THE MANIFEST:
 * This activity is only launched from within our app using an explicit Intent.
 * No external app should ever be able to start it. Setting exported="false"
 * enforces this security boundary. If another app tries to launch this
 * activity, Android blocks it with a SecurityException.
 *
 * WHY parentActivityName IS DECLARED:
 * The manifest includes android:parentActivityName=".MainActivity".
 * This tells the system that pressing the Up arrow (back arrow in the
 * action bar) should navigate to MainActivity, not just pop the back stack.
 * This matters when the activity is launched from a notification or deep link
 * — the back stack might be empty, so Android needs to know the parent.
 */
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // Enable the Up button in the action bar.
        // This works together with parentActivityName in the manifest.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Retrieve data passed from MainActivity via Intent extras.
        // getStringExtra() returns null if the key doesn't exist,
        // so we provide a default value to avoid NullPointerException.
        TextView tvStudentName = findViewById(R.id.tvStudentName);
        String studentName = getIntent().getStringExtra("student_name");

        if (studentName != null) {
            tvStudentName.setText("Student: " + studentName);
        } else {
            tvStudentName.setText("No student selected");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // This is called when the user presses the Up button.
        // finish() closes this activity and returns to the parent.
        finish();
        return true;
    }
}
