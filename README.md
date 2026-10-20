# Android Learning Projects

A collection of Android Studio projects covering core Android development concepts, from Activity lifecycle and Services to Intents, BroadcastReceivers, Context types, and Jetpack Compose.

---

## Projects Overview

| Project | Concept | Language |
|---|---|---|
| [Demo1Activities](./Demo1Activities) | Activity basics & navigation template | Java |
| [LifecycleLogger](./LifecycleLogger) | Activity Lifecycle (all 7 callbacks) | Java |
| [MyApplication](./MyApplication) | Jetpack Compose & Kotlin modern UI | Kotlin |
| [ServiceDemo](./ServiceDemo) | Started Services & Bound Services | Java |
| [StudenInfo](./StudenInfo) | Android Context types (Application/Activity/Fragment/View) | Java |
| [StudentCommunicationApp](./StudentCommunicationApp) | Explicit & Implicit Intents, BroadcastReceivers | Java |
| [StudentManager](./StudentManager) | AndroidManifest declarations, Permissions, Metadata | Java |
| [UnicodeToNudi](./UnicodeToNudi) | Kannada Unicode → Nudi converter: clipboard, Share & text-selection intents | Java |

---

## Learning Path

These projects follow a natural progression of Android fundamentals:

```
1. Demo1Activities          ← Start here: understand what an Activity is
2. LifecycleLogger          ← How Activities live and die (lifecycle)
3. StudenInfo               ← Context: the backbone of Android components
4. StudentCommunicationApp  ← Intents & BroadcastReceivers (inter-component communication)
5. StudentManager           ← The Manifest: declaring all components to the OS
6. ServiceDemo              ← Background work with Services
7. MyApplication            ← Modern Android with Kotlin + Jetpack Compose
8. UnicodeToNudi            ← A real utility app: intents, clipboard, unit-tested logic
```

---

## Common Setup

All projects share these configurations:
- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 34 (Android 14)
- **Build System:** Gradle with Kotlin DSL (`.gradle.kts`)
- **IDE:** Android Studio

---

## Key Concepts Quick Reference

### Android's Four App Components

| Component | Purpose | Declared in Manifest |
|---|---|---|
| `Activity` | Screen with UI | Yes |
| `Service` | Background work, no UI | Yes |
| `BroadcastReceiver` | Respond to system/custom events | Yes (or dynamic) |
| `ContentProvider` | Share data between apps | Yes |

### Intent Types

```
Explicit Intent → targets specific component (intra-app)
Implicit Intent → declares action, system finds the right app
Broadcast Intent → sent to all interested receivers
```

### Context Hierarchy

```
Application Context   ← global, lives forever
Activity Context      ← tied to Activity lifecycle
Fragment Context      ← delegates to its parent Activity
View Context          ← the Activity that inflated the View
```

### Service Types

```
Started Service  ← runs independently until stopped
Bound Service    ← tied to client lifecycle, supports direct calls
Foreground Service ← started service with persistent notification
```

---

*All projects target Android 7.0+ and are written for educational purposes.*
