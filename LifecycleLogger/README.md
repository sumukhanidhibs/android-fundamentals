# LifecycleLogger

An Android educational app that visually demonstrates the **Activity Lifecycle** by logging every lifecycle callback in real-time to a scrollable UI.

## What It Implements

- Overrides all 7 Activity lifecycle methods: `onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onRestart`, `onDestroy`
- Displays timestamped lifecycle events in a scrollable `TextView`
- Toast notifications for each lifecycle transition
- Navigation to a `SecondActivity` to observe lifecycle interplay between two screens
- A "Clear Logs" button to reset the log view

## Concepts Covered

### Android Activity Lifecycle

The Activity lifecycle is a set of states an Activity goes through from creation to destruction. Android manages these states and calls specific methods at each transition:

```
onCreate() → onStart() → onResume()
                              ↓ (app running)
                         onPause()
                              ↓
                         onStop()
                         ↙       ↘
               onRestart()     onDestroy()
                    ↓
               onStart()
```

| Method | When Called | Use Case |
|---|---|---|
| `onCreate()` | First creation | Initialize UI, set content view, bind data |
| `onStart()` | Becoming visible | Register listeners, start animations |
| `onResume()` | Gaining focus | Resume camera, media, sensors |
| `onPause()` | Losing focus (partially hidden) | Pause camera, save draft state |
| `onStop()` | Completely hidden | Release heavy resources, save persistent data |
| `onRestart()` | Coming back from stopped state | Re-initialize released resources |
| `onDestroy()` | Being destroyed | Final cleanup, release all resources |

### Key Scenarios This App Demonstrates

1. **Home button pressed** → `onPause()` → `onStop()`
2. **App reopened** → `onRestart()` → `onStart()` → `onResume()`
3. **Navigate to SecondActivity** → `onPause()` → `onStop()`
4. **Back from SecondActivity** → `onRestart()` → `onStart()` → `onResume()`
5. **Screen rotation** → `onPause()` → `onStop()` → `onDestroy()` → `onCreate()` → `onStart()` → `onResume()`
6. **App killed by system** → `onPause()` → `onStop()` → `onDestroy()`

### Why Lifecycle Management Matters

- **Memory leaks**: Not releasing resources in `onStop`/`onDestroy` causes leaks
- **Battery drain**: Running sensors/GPS in background when not needed
- **Crashes**: Accessing destroyed views or contexts after `onDestroy`
- **Data loss**: Not saving user state before `onPause` can lose unsaved work

## Tech Stack

- **Language:** Java
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Dependencies:** AndroidX AppCompat, Material Design
