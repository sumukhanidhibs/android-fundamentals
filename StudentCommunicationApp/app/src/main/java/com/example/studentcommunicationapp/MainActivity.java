package com.example.studentcommunicationapp;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/*
 * MainActivity.java — The launcher activity of the Student Communication App.
 *
 * KEY CONCEPTS EXPLAINED:
 *
 * 1. This class extends AppCompatActivity (not plain Activity).
 *    WHY? AppCompatActivity gives us backward-compatible features like
 *    the ActionBar, Fragments, and Material Design components on older
 *    Android versions (API < 21). Always prefer AppCompatActivity.
 *
 * 2. We register a BroadcastReceiver dynamically here. There are two ways
 *    to register receivers:
 *      a) In AndroidManifest.xml (static) — survives app restart
 *      b) In Java code (dynamic) — lives only while the component is alive
 *    We use BOTH in this project so you can compare them.
 *
 * 3. Every button demonstrates a different type of Intent:
 *      - Explicit Intent  → targets a specific Activity by class name
 *      - Implicit Intents → declare an ACTION and let Android find a match
 *      - Broadcast Intent → sends data to all registered BroadcastReceivers
 *
 * 4. IMPORTANT LESSON — API VERSION CHECKS:
 *    Some methods only exist on newer Android versions. If you call a
 *    method that doesn't exist on the device's Android version, you get:
 *        java.lang.NoSuchMethodError
 *    The solution is ALWAYS to wrap the call in:
 *        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.XXXX) { ... }
 *    This checks the device's ACTUAL Android version at runtime.
 */
public class MainActivity extends AppCompatActivity {

    // ── Field declarations ──────────────────────────────────────────────
    // We keep a reference to the dynamically registered receiver so we
    // can unregister it in onDestroy(). Failing to unregister causes a
    // memory leak and a "leaked IntentFilter" warning in Logcat.
    private MyBroadcastReceiver dynamicReceiver;

    // A constant string used as the "action" for our custom broadcast.
    // Convention: use your package name as a prefix to avoid collisions
    // with broadcasts from other apps.
    private static final String CUSTOM_BROADCAST_ACTION =
            "com.example.studentcommunicationapp.CUSTOM_BROADCAST";

    // ── Lifecycle: onCreate ─────────────────────────────────────────────
    // onCreate() is the FIRST callback in the Activity lifecycle.
    // It runs once when the Activity is created. This is where you:
    //   • Inflate the layout (setContentView)
    //   • Find views by their IDs
    //   • Set up listeners
    //   • Initialize data
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setContentView() inflates the XML layout file into actual View
        // objects in memory and attaches them to this Activity's window.
        setContentView(R.layout.activity_main);

        // ── Find views ──────────────────────────────────────────────────
        // findViewById() searches the inflated view hierarchy for a View
        // with the given ID. It returns a generic View, so we cast it.
        // CAREFUL: If the ID doesn't exist in your layout XML, this
        // returns null and your app will crash with NullPointerException.
        EditText etStudentName = findViewById(R.id.etStudentName);
        Button btnOpenSecond   = findViewById(R.id.btnOpenSecondActivity);
        Button btnSendEmail    = findViewById(R.id.btnSendEmail);
        Button btnOpenWebsite  = findViewById(R.id.btnOpenWebsite);
        Button btnDialPhone    = findViewById(R.id.btnDialPhone);
        Button btnSendBroadcast = findViewById(R.id.btnSendBroadcast);

        // ── Register dynamic BroadcastReceiver ──────────────────────────
        // We create an IntentFilter that tells Android: "This receiver
        // is interested in broadcasts whose action matches our custom string."
        dynamicReceiver = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter(CUSTOM_BROADCAST_ACTION);

        // ╔══════════════════════════════════════════════════════════════╗
        // ║  FIX: API VERSION CHECK                                     ║
        // ║                                                             ║
        // ║  Build.VERSION.SDK_INT = the Android version running on     ║
        // ║  the DEVICE right now (e.g., 24 for Android 7.0).          ║
        // ║                                                             ║
        // ║  Build.VERSION_CODES.TIRAMISU = 33 (Android 13).           ║
        // ║                                                             ║
        // ║  The 3-parameter registerReceiver() with the                ║
        // ║  RECEIVER_NOT_EXPORTED flag was added in API 33.            ║
        // ║  Calling it on API < 33 causes NoSuchMethodError.           ║
        // ║                                                             ║
        // ║  RULE: Whenever you use a method introduced in a newer      ║
        // ║  API, ALWAYS wrap it in an API check like this.             ║
        // ╚══════════════════════════════════════════════════════════════╝
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ (Android 13+): We can use RECEIVER_NOT_EXPORTED flag.
            // This flag tells the system: "Only MY app can send broadcasts
            // to this receiver." It's a security best practice.
            registerReceiver(dynamicReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            // API 21-32: Use the older 2-parameter version.
            // On these older versions, dynamically registered receivers
            // are already NOT exported by default, so this is still safe.
            // The flag didn't exist yet — that's why we skip it.
            registerReceiver(dynamicReceiver, filter);
        }

        // ══════════════════════════════════════════════════════════════════
        //  BUTTON 1: EXPLICIT INTENT — Navigate to SecondActivity
        // ══════════════════════════════════════════════════════════════════
        // An Explicit Intent specifies the EXACT component (class) to start.
        // Use explicit intents when you know the target — typically for
        // navigating between your own app's Activities.
        btnOpenSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Read the student name from the EditText.
                // getText() returns an Editable; toString() converts it.
                String studentName = etStudentName.getText().toString().trim();

                // Validate input — never trust user input blindly.
                if (studentName.isEmpty()) {
                    // Toast: a small popup message shown briefly.
                    // LENGTH_SHORT ≈ 2 seconds, LENGTH_LONG ≈ 3.5 seconds.
                    Toast.makeText(MainActivity.this,
                            "Please enter a student name", Toast.LENGTH_SHORT).show();
                    return; // Stop here — don't launch the next Activity.
                }

                // Create the explicit intent.
                // PARAMETER 1 (Context): "this" doesn't work inside an
                //   anonymous inner class because "this" refers to the
                //   OnClickListener, not the Activity. Use "MainActivity.this".
                // PARAMETER 2 (Class): The target Activity's .class reference.
                Intent explicitIntent = new Intent(MainActivity.this, SecondActivity.class);

                // putExtra() attaches key-value data to the Intent.
                // SecondActivity will retrieve this using the same key.
                // Think of extras like a Bundle (dictionary/map) riding
                // along with the Intent.
                explicitIntent.putExtra("STUDENT_NAME", studentName);
                explicitIntent.putExtra("STUDENT_ID", 101);

                // startActivity() hands the Intent to the Android system,
                // which creates SecondActivity and calls its onCreate().
                startActivity(explicitIntent);
            }
        });

        // ══════════════════════════════════════════════════════════════════
        //  BUTTON 2: IMPLICIT INTENT — Send Email
        // ══════════════════════════════════════════════════════════════════
        // An Implicit Intent does NOT name a target component.
        // Instead, it declares an ACTION (what to do) and DATA (what to do it with).
        // Android's system searches all installed apps for Activities whose
        // <intent-filter> matches this action+data combination.
        btnSendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ACTION_SENDTO is specifically for sending to a particular
                // address. Combined with a "mailto:" URI, it limits the
                // chooser to actual email apps (not social media, etc.).
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

                // Uri.parse() converts a string to a URI object.
                // "mailto:" is the scheme — like "https:" for web URLs.
                emailIntent.setData(Uri.parse("mailto:teacher@school.com"));

                // EXTRA_SUBJECT and EXTRA_TEXT are standard extras that
                // email apps know how to read.
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Student Query");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello Teacher, I have a doubt...");

                // resolveActivity() checks if ANY installed app can handle
                // this intent. If none can, it returns null.
                // WHY CHECK? If no email app is installed (common on
                // emulators), calling startActivity() would crash the app
                // with an ActivityNotFoundException.
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(emailIntent);
                } else {
                    Toast.makeText(MainActivity.this,
                            "No email app found!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ══════════════════════════════════════════════════════════════════
        //  BUTTON 3: IMPLICIT INTENT — Open Website
        // ══════════════════════════════════════════════════════════════════
        btnOpenWebsite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ACTION_VIEW tells Android: "Display this data to the user."
                // When combined with an "https://" URI, Android knows to
                // open a web browser.
                String url = "https://developer.android.com";
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                if (webIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(webIntent);
                } else {
                    Toast.makeText(MainActivity.this,
                            "No browser app found!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ══════════════════════════════════════════════════════════════════
        //  BUTTON 4: IMPLICIT INTENT — Open Phone Dialer
        // ══════════════════════════════════════════════════════════════════
        btnDialPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ACTION_DIAL opens the dialer with a pre-filled number
                // but does NOT place the call. The user must press "Call".
                // This does NOT require CALL_PHONE permission.
                //
                // Compare with ACTION_CALL which places the call immediately
                // and DOES require the dangerous CALL_PHONE permission.
                // Always prefer ACTION_DIAL for user safety.
                String phoneNumber = "tel:+919876543210";
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(phoneNumber));

                if (dialIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(dialIntent);
                } else {
                    Toast.makeText(MainActivity.this,
                            "No dialer app found!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ══════════════════════════════════════════════════════════════════
        //  BUTTON 5: BROADCAST INTENT — Send custom broadcast
        // ══════════════════════════════════════════════════════════════════
        // A Broadcast Intent is NOT used to start an Activity.
        // Instead, it "broadcasts" a message to all registered
        // BroadcastReceivers that match the intent's action.
        // Think of it like a radio station transmitting a signal —
        // any radio (receiver) tuned to that frequency picks it up.
        btnSendBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent broadcastIntent = new Intent(CUSTOM_BROADCAST_ACTION);

                // Attach data to the broadcast — the receiver will read this.
                broadcastIntent.putExtra("BROADCAST_MESSAGE",
                        "Hello from MainActivity! This is a custom broadcast.");

                // setPackage() restricts this broadcast to only receivers
                // within our own app. This is a security best practice —
                // without it, any app on the device could receive our broadcast.
                broadcastIntent.setPackage(getPackageName());

                // sendBroadcast() dispatches the intent to the system.
                // The system then delivers it to all matching receivers.
                sendBroadcast(broadcastIntent);

                Toast.makeText(MainActivity.this,
                        "Broadcast Sent!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Lifecycle: onDestroy ────────────────────────────────────────────
    // onDestroy() is called when the Activity is finishing or being
    // destroyed by the system. ALWAYS unregister dynamic receivers here.
    // If you registered in onCreate(), unregister in onDestroy().
    // If you registered in onResume(), unregister in onPause().
    // The rule: match the scope of register/unregister.
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister to prevent memory leaks.
        if (dynamicReceiver != null) {
            unregisterReceiver(dynamicReceiver);
        }
    }
}
