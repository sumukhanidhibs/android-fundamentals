# StudentManager

An Android app demonstrating **Android Manifest declarations** comprehensively — activities, services, broadcast receivers, permissions, metadata — along with service-to-receiver broadcast communication.

## What It Implements

- **`MainActivity`**: Reads `<meta-data>` from the manifest at runtime using `PackageManager`; navigates to `SecondActivity`; starts `DataSyncService`
- **`SecondActivity`**: Receives student name via intent extras with proper back-navigation via `android:parentActivityName`
- **`DataSyncService`**: Simulates a 3-second data sync on a background thread; sends `"STUDENT_ADDED"` broadcast upon completion; calls `stopSelf()` when done
- **`NotificationReceiver`**: Catches the `"STUDENT_ADDED"` broadcast and shows a Toast

## Concepts Covered

### AndroidManifest.xml — The App's Blueprint

The `AndroidManifest.xml` is the configuration file every Android app must have at its root. The Android system reads it before running any app code to understand the app's structure.

#### Full Manifest Structure

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.studentmanager">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.StudentManager">

        <!-- Activity Declaration -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <meta-data
                android:name="app_version_label"
                android:value="v1.0-beta" />
        </activity>

        <!-- Activity with parent for Up navigation -->
        <activity
            android:name=".SecondActivity"
            android:exported="false"
            android:parentActivityName=".MainActivity" />

        <!-- Service Declaration -->
        <service
            android:name=".DataSyncService"
            android:exported="false" />

        <!-- BroadcastReceiver Declaration -->
        <receiver
            android:name=".NotificationReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="com.example.studentmanager.STUDENT_ADDED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

### Manifest Components Explained

#### `<uses-permission>`

Declares permissions the app needs from the user/system:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

**Permission types:**
- **Normal permissions** (auto-granted): `INTERNET`, `ACCESS_NETWORK_STATE`
- **Dangerous permissions** (user must approve): `CAMERA`, `READ_CONTACTS`, `ACCESS_FINE_LOCATION`
- **Signature permissions**: Only granted to apps signed with same key

#### `android:exported` (Required since API 31)

Controls whether other apps can start this component:

```xml
android:exported="true"   <!-- other apps CAN access this component -->
android:exported="false"  <!-- only your app can access this component -->
```

**Rule of thumb:**
- Launcher activity → `exported="true"` (must be, for the launcher to start it)
- All other activities, services, receivers → `exported="false"` (security best practice)

#### `<meta-data>` — Reading Manifest Data at Runtime

Store configuration values in the manifest:

```xml
<activity android:name=".MainActivity" ...>
    <meta-data
        android:name="app_version_label"
        android:value="v1.0-beta" />
</activity>
```

Read them at runtime with `PackageManager`:

```java
ActivityInfo info = getPackageManager()
    .getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
String version = info.metaData.getString("app_version_label");
// version = "v1.0-beta"
```

#### Parent Activity Navigation

```xml
<activity
    android:name=".SecondActivity"
    android:parentActivityName=".MainActivity" />
```

This enables the Up button (←) in the ActionBar to navigate back to `MainActivity`. No code needed.

### Service with Background Thread

Services run on the **main thread** by default. Long operations must use a background thread to avoid ANR (Application Not Responding):

```java
// DataSyncService.java
@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    new Thread(() -> {
        // simulate sync
        Thread.sleep(3000);

        // notify via broadcast
        Intent broadcastIntent = new Intent("com.example.studentmanager.STUDENT_ADDED");
        broadcastIntent.setPackage(getPackageName());
        sendBroadcast(broadcastIntent);

        stopSelf(startId); // stop after work is done
    }).start();

    return START_NOT_STICKY; // don't restart if killed
}
```

**Why `stopSelf(startId)` instead of `stopSelf()`?**
If `startService()` is called multiple times, `stopSelf(startId)` only stops the service when the latest start request has been processed, preventing premature stopping.

### Service → Receiver Communication via Broadcast

```
DataSyncService
    │ (work complete)
    └──sendBroadcast("STUDENT_ADDED")──► Android System
                                               │
                                     NotificationReceiver.onReceive()
                                               │
                                          show Toast
```

```java
// NotificationReceiver.java
@Override
public void onReceive(Context context, Intent intent) {
    if ("com.example.studentmanager.STUDENT_ADDED".equals(intent.getAction())) {
        Toast.makeText(context, "Student sync complete!", Toast.LENGTH_LONG).show();
    }
}
```

### Security Best Practices in Manifest

1. **Use `setPackage()`** when sending broadcasts to restrict to your own app
2. **Set `exported="false"`** for all internal components
3. **Declare only needed permissions** — the fewer, the better for user trust
4. **Use explicit intents** when starting your own components (not implicit)

### Component Lifecycle Summary

```
App Launch → System reads AndroidManifest.xml
           → Instantiates Application class
           → Starts MainActivity (LAUNCHER activity)
           → MainActivity starts DataSyncService
           → DataSyncService sends broadcast on completion
           → NotificationReceiver.onReceive() called
```

## Tech Stack

- **Language:** Java
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Permissions:** INTERNET, ACCESS_NETWORK_STATE
- **Dependencies:** AndroidX AppCompat, Material Design
