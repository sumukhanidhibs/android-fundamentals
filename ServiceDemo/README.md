# ServiceDemo

An Android app demonstrating the two primary types of Android Services: **Started Services** and **Bound Services**, with real-time interaction from the UI.

## What It Implements

- **StartedService**: Runs a repeating background task every 3 seconds using `Handler` + `Runnable`; returns `START_STICKY` to survive system kills
- **BoundService**: Maintains a running counter (increments every second) accessible from the Activity via the `LocalBinder` pattern
- **MainActivity**: UI to start/stop both services and bind/unbind to the bound service, with proper `ServiceConnection` callbacks

## Concepts Covered

### Android Services

A Service is an Android component that performs long-running background operations without a user interface.

```
Activity ──startService()──► StartedService
                                  (runs independently)

Activity ──bindService()───► BoundService
         ◄──IBinder──────────    (tight coupling, direct method calls)
```

### Started Service

A service started with `startService()` that runs until it explicitly calls `stopSelf()` or `stopService()` is called.

```java
// StartedService.java
@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    handler.postDelayed(runnable, 3000); // runs every 3 seconds
    return START_STICKY; // restart if killed
}
```

**Return values from `onStartCommand()`:**

| Flag | Meaning |
|---|---|
| `START_STICKY` | Restart after kill; intent will be null on restart |
| `START_NOT_STICKY` | Do NOT restart after kill |
| `START_REDELIVER_INTENT` | Restart after kill; re-deliver the original intent |

### Bound Service

A service that allows components to bind to it and call its public methods directly (client-server model).

```java
// BoundService.java
public class LocalBinder extends Binder {
    BoundService getService() {
        return BoundService.this; // return service instance
    }
}

@Override
public IBinder onBind(Intent intent) {
    return binder; // called when client binds
}
```

**Binding from Activity:**
```java
ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        BoundService.LocalBinder binder = (BoundService.LocalBinder) service;
        boundService = binder.getService();
        isBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
    }
};
bindService(intent, connection, Context.BIND_AUTO_CREATE);
```

### Service Lifecycle Comparison

```
Started Service:
onCreate() → onStartCommand() → [running] → onDestroy()

Bound Service:
onCreate() → onBind() → [bound clients active] → onUnbind() → onDestroy()

Both:
onCreate() → onStartCommand() + onBind() → [both running] → onUnbind() → onDestroy()
```

### Key Differences: Started vs Bound

| Feature | Started Service | Bound Service |
|---|---|---|
| Started by | `startService()` | `bindService()` |
| Stopped by | `stopSelf()` or `stopService()` | When all clients unbind |
| Communication | One-way (intents) | Two-way (direct method calls) |
| Lifespan | Independent of component | Tied to bound components |
| Use case | Music playback, file download | Live data feeds, IPC |

### Handler + Runnable for Periodic Tasks

```java
Handler handler = new Handler(Looper.getMainLooper());
Runnable runnable = new Runnable() {
    @Override
    public void run() {
        // do work
        handler.postDelayed(this, 3000); // reschedule
    }
};
handler.post(runnable); // start
handler.removeCallbacks(runnable); // stop
```

### Memory Leak Prevention

Always unbind in `onDestroy()` and remove handler callbacks:
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (isBound) {
        unbindService(connection);
        isBound = false;
    }
    handler.removeCallbacks(runnable);
}
```

## Tech Stack

- **Language:** Java
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Dependencies:** AndroidX AppCompat, Material Design
