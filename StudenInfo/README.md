# StudenInfo (Student Information App)

An Android app demonstrating the **four types of Android Context** — Application, Activity, Fragment, and View — using a student information management scenario.

## What It Implements

- **`MyApplication`** (extends `Application`): Stores global student data (name, email, course) accessible app-wide via Application Context
- **`MainActivity`**: Demonstrates Application Context, Activity Context, and View Context side by side
- **`DisplayActivity`**: Shows student details passed via explicit intent
- **`StudentFragment`**: Demonstrates Fragment Context and accessing Application-level data from within a Fragment
- **`StudentService`**: A background started service example
- **`StudentReceiver`**: A `BroadcastReceiver` that listens for the custom action `"com.example.studentinfo.STUDENT_ACTION"`

## Concepts Covered

### Android Context

`Context` is one of Android's most fundamental concepts. It is an abstract class whose implementation is provided by the Android system. It provides access to application-specific resources and classes, as well as calls for application-level operations.

```
Context (abstract)
    ├── ContextWrapper
    │       ├── Application        ← Application Context
    │       └── ContextThemeWrapper
    │               ├── Activity   ← Activity Context
    │               └── Service
    └── Fragment (has getContext()) ← Fragment Context
```

### The Four Types of Context

#### 1. Application Context (`getApplicationContext()`)

- Tied to the **app's lifecycle** (lives as long as the app does)
- Safe for: Singletons, Room databases, SharedPreferences, global state
- **Do NOT use for:** UI operations, inflating layouts, showing dialogs

```java
// In MyApplication.java
public class MyApplication extends Application {
    private String studentName = "John Doe";
    private String studentEmail = "john@example.com";

    public String getStudentName() { return studentName; }
}

// Accessing from Activity
MyApplication app = (MyApplication) getApplicationContext();
String name = app.getStudentName();
```

#### 2. Activity Context (`this` or `ActivityName.this`)

- Tied to the **Activity's lifecycle**
- Required for: Showing dialogs, inflating layouts, starting activities
- **Danger:** Holding a reference past Activity's life causes **memory leaks**

```java
// Correct use
AlertDialog.Builder builder = new AlertDialog.Builder(this); // needs Activity context
Intent intent = new Intent(this, DisplayActivity.class);     // needs Activity context
startActivity(intent);
```

#### 3. Fragment Context (`getContext()` or `requireContext()`)

- Returns the **Activity** the fragment is attached to
- **Always check** `isAdded()` before accessing, as Fragment can detach

```java
// In StudentFragment.java
MyApplication app = (MyApplication) requireContext().getApplicationContext();
String studentName = app.getStudentName();

// Safe context access
Context ctx = getContext(); // may be null if fragment is detached
Context ctx = requireContext(); // throws IllegalStateException if detached
```

#### 4. View Context (`view.getContext()`)

- Returns the Context the View was constructed with (usually the Activity)
- Available from any View reference

```java
Button btn = findViewById(R.id.myButton);
Context viewContext = btn.getContext(); // returns Activity context
```

### Context Usage Decision Chart

```
Need Context?
    │
    ├── Inside Application class? → use `this`
    │
    ├── Showing Dialog/Toast/starting Activity? → use Activity Context (`this`)
    │
    ├── Singleton / Long-lived object? → use `getApplicationContext()`
    │
    ├── Inside Fragment? → use `requireContext()` (Activity context)
    │
    └── Inside View? → use `getContext()`
```

### Memory Leak Warning

**Never store Activity context in a static field or long-lived singleton:**

```java
// BAD - causes memory leak
public class MySingleton {
    private static Context context; // holds Activity reference forever
    MySingleton(Context ctx) { context = ctx; }
}

// GOOD - safe
public class MySingleton {
    private static Context context;
    MySingleton(Context ctx) { context = ctx.getApplicationContext(); }
}
```

### Application Class

The `Application` class is instantiated before any Activity, Service, or BroadcastReceiver. It's the ideal place for:

- Global state management
- Initializing libraries (Retrofit, Room, Firebase)
- Crash reporting setup

```xml
<!-- AndroidManifest.xml -->
<application
    android:name=".MyApplication"
    ...>
```

### Dynamic Fragment Management

```java
// Load a fragment dynamically
StudentFragment fragment = new StudentFragment();
getSupportFragmentManager()
    .beginTransaction()
    .replace(R.id.fragment_container, fragment)
    .commit();
```

## Tech Stack

- **Language:** Java
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Dependencies:** AndroidX AppCompat, Fragment, Material Design
