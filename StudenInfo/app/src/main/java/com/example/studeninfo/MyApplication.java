package com.example.studeninfo;

import android.app.Application;
import android.util.Log;

/**
 * MyApplication extends Application class.
 * This gives us access to Application Context — the global context
 * that lives for the entire lifetime of the app.
 *
 * WHY: We use this to store student data globally so it is accessible
 * from any Activity, Fragment, or Service without passing it around.
 */
public class MyApplication extends Application {

    // TAG for logging
    private static final String TAG = "MyApplication";

    // Global student data — accessible throughout the app
    private String studentName;
    private String studentEmail;
    private String studentCourse;

    /**
     * onCreate() is called when the app process is created.
     * This is the first method called in the entire app lifecycle.
     * Context here = Application Context (this)
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Set default student data on app start
        // 'this' here refers to Application Context
        studentName = "Rahul Sharma";
        studentEmail = "rahul@bca.edu";
        studentCourse = "BCA - 3rd Year";

        Log.d(TAG, "Application started. Context: " + this.getClass().getName());
    }

    // ─── Getters and Setters ─────────────────────────────────────

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentCourse() {
        return studentCourse;
    }

    public void setStudentCourse(String studentCourse) {
        this.studentCourse = studentCourse;
    }
}