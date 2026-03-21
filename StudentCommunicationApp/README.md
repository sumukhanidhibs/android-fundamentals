# StudentCommunicationApp

An Android app demonstrating **Explicit Intents**, **Implicit Intents**, and **BroadcastReceivers** (both static and dynamic registration) using a student communication scenario.

## What It Implements

- **Explicit Intent**: Navigate to `SecondActivity` with student name and ID passed as extras
- **Implicit Intents**: Open email client, web browser, and phone dialer without specifying which app to use
- **Custom Broadcast**: Send a custom broadcast that both statically and dynamically registered receivers can intercept
- **Dynamic BroadcastReceiver**: Registered in code with proper API 33+ `RECEIVER_NOT_EXPORTED` flag
- **`SecondActivity`**: Receives and displays data from `MainActivity`
- **`MyBroadcastReceiver`**: Processes the custom broadcast action

## Concepts Covered

### Android Intents

An `Intent` is a messaging object used to request an action from another app component. It's Android's primary inter-component communication mechanism.

```
Component A ──Intent──► Android System ──routes──► Component B
```

### Explicit Intents

Explicit intents target a **specific component** by class name. Used for intra-app navigation.

```java
// Navigate to SecondActivity with data
Intent intent = new Intent(MainActivity.this, SecondActivity.class);
intent.putExtra("STUDENT_NAME", studentName);
intent.putExtra("STUDENT_ID", 1001);
startActivity(intent);

// Receive in SecondActivity
String name = getIntent().getStringExtra("STUDENT_NAME");
int id = getIntent().getIntExtra("STUDENT_ID", 0);
```

**Common intent extras methods:**
| Method | Type |
|---|---|
| `putExtra(key, String)` | String |
| `putExtra(key, int)` | Integer |
| `putExtra(key, boolean)` | Boolean |
| `putExtra(key, Serializable)` | Object (implements Serializable) |
| `putExtra(key, Parcelable)` | Object (implements Parcelable) |

### Implicit Intents

Implicit intents declare a **general action** and let the system find the best component to handle it. The system matches the intent against registered `<intent-filter>` entries in installed apps.

```java
// Open email client
Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
emailIntent.setData(Uri.parse("mailto:student@example.com"));
if (emailIntent.resolveActivity(getPackageManager()) != null) {
    startActivity(emailIntent);
}

// Open web browser
Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"));
startActivity(webIntent);

// Open phone dialer (no permission needed)
Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+1234567890"));
startActivity(dialIntent);
```

**Important:** Always call `resolveActivity()` before starting an implicit intent to avoid `ActivityNotFoundException`.

### Package Visibility (Android 11+)

Android 11 (API 30) restricted what apps you can "see". To use implicit intents for specific URI schemes, declare them in the manifest:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.SENDTO" />
        <data android:scheme="mailto" />
    </intent>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="https" />
    </intent>
    <intent>
        <action android:name="android.intent.action.DIAL" />
        <data android:scheme="tel" />
    </intent>
</queries>
```

### BroadcastReceivers

A `BroadcastReceiver` is a component that responds to system-wide broadcast announcements (or custom ones).

#### Static Registration (Manifest)

Registered in `AndroidManifest.xml`. Can receive broadcasts even when the app is not running.

```xml
<receiver
    android:name=".MyBroadcastReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.example.studentapp.STUDENT_BROADCAST" />
    </intent-filter>
</receiver>
```

#### Dynamic Registration (Code)

Registered programmatically. Only active while the app is running (while registered).

```java
// API 33+ requires RECEIVER_NOT_EXPORTED or RECEIVER_EXPORTED flag
MyBroadcastReceiver receiver = new MyBroadcastReceiver();
IntentFilter filter = new IntentFilter("com.example.studentapp.STUDENT_BROADCAST");

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED);
} else {
    registerReceiver(receiver, filter);
}

// Always unregister to avoid leaks
@Override
protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(receiver);
}
```

#### Sending a Custom Broadcast

```java
Intent broadcastIntent = new Intent("com.example.studentapp.STUDENT_BROADCAST");
broadcastIntent.setPackage(getPackageName()); // security: restrict to own app
broadcastIntent.putExtra("MESSAGE", "New student enrolled!");
sendBroadcast(broadcastIntent);
```

### Static vs Dynamic Registration

| Feature | Static (Manifest) | Dynamic (Code) |
|---|---|---|
| Registered | At install time | At runtime |
| Active when | Even app is closed | Only while registered |
| Use case | System events (boot, connectivity) | UI-bound events |
| Android 8.0+ | Most implicit broadcasts blocked | Still works |

### Intent Resolution Process

```
sendBroadcast(intent)
        ↓
Android PackageManager scans all registered receivers
        ↓
Matches action string against intent-filter <action> tags
        ↓
Calls onReceive() on all matching receivers
```

## Tech Stack

- **Language:** Java
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Dependencies:** AndroidX AppCompat, Material Design
