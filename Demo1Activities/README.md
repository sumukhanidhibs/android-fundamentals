# Demo1Activities

A starter/template Android project for experimenting with Android Activities and multi-screen navigation.

## What It Implements

This project is a foundational template set up with the standard Android project structure. It is configured and ready for Activity-based experiments.

## Project Structure

```
Demo1Activities/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/        ← Activity classes go here
│   │   │   ├── res/         ← Layouts, drawables, strings
│   │   │   └── AndroidManifest.xml
│   │   ├── test/            ← Unit tests
│   │   └── androidTest/     ← Instrumented tests
│   └── build.gradle.kts
└── build.gradle.kts
```

## Concepts: Android Activities

### What is an Activity?

An `Activity` represents a single screen with a user interface. It is one of Android's four fundamental application components.

```
Your App
├── MainActivity      (Home screen)
├── DetailActivity    (Detail screen)
├── SettingsActivity  (Settings screen)
└── LoginActivity     (Login screen)
```

### Creating an Activity

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // inflate the layout

        // Initialize UI components
        Button button = findViewById(R.id.myButton);
        button.setOnClickListener(v -> {
            // handle click
        });
    }
}
```

### Registering in Manifest

Every Activity **must** be declared in `AndroidManifest.xml`:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <!-- MAIN + LAUNCHER = this is the app's entry point -->
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### Navigating Between Activities

```java
// Start another Activity
Intent intent = new Intent(this, SecondActivity.class);
startActivity(intent);

// Start and expect a result (modern API)
ActivityResultLauncher<Intent> launcher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();
            // process result
        }
    }
);
launcher.launch(new Intent(this, SecondActivity.class));
```

### Back Stack

Android maintains a **back stack** of activities. When you press the Back button, the current activity is popped off the stack:

```
Stack:            Back pressed:      Back pressed again:
[MainActivity]    [MainActivity]     [empty - app exits]
[SecondActivity]  (SecondActivity
[ThirdActivity]    destroyed)
```

### Task and Launch Modes

Control how activities are launched via `android:launchMode` in manifest:

| Mode | Behavior |
|---|---|
| `standard` | New instance every time (default) |
| `singleTop` | Reuse if already at top of stack |
| `singleTask` | Only one instance in back stack |
| `singleInstance` | Isolated in its own task |

## Tech Stack

- **Language:** Java
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Dependencies:** AndroidX AppCompat, Material Design
