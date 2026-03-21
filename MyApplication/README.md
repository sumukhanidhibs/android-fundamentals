# MyApplication

A modern Android app built with **Jetpack Compose** and **Kotlin**, demonstrating the declarative UI paradigm as opposed to the traditional XML-based View system.

## What It Implements

- `MainActivity` as a `ComponentActivity` (Compose entry point)
- `setContent {}` block to compose the UI declaratively
- A `Greeting` composable function that displays a personalized message
- `Material Design 3` theming via `MyApplicationTheme`
- `@Preview` annotation for live design-time preview in Android Studio

## Concepts Covered

### Jetpack Compose

Jetpack Compose is Android's modern, fully declarative UI toolkit. Instead of defining UI in XML and manipulating Views imperatively, you describe what the UI should look like for a given state.

```
Traditional (XML + View):          Compose (Kotlin + Composable):
─────────────────────────          ──────────────────────────────
activity_main.xml                  @Composable fun MyUI() {
  <TextView                            Text("Hello!")
    android:text="Hello!" />       }
```

### Composable Functions

Any function annotated with `@Composable` can describe UI. Compose calls these functions to build the UI tree.

```kotlin
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
```

**Rules of Composable functions:**
- Must be called from other composable functions or `setContent {}`
- Should be **side-effect free** (no modifying external state)
- Can be called multiple times (recomposed) when state changes
- Named with `PascalCase` by convention

### setContent {} Entry Point

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Greeting("Android")
                }
            }
        }
    }
}
```

`setContent {}` replaces `setContentView(R.layout.activity_main)` and is where the entire Compose UI tree is anchored.

### Recomposition

Compose automatically re-executes composable functions when their **state** changes:

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }  // state holder
    Button(onClick = { count++ }) {
        Text("Count: $count")  // recomposed when count changes
    }
}
```

- `remember {}` keeps state across recompositions
- `mutableStateOf()` creates observable state that triggers recomposition

### Material Design 3 Theming

```kotlin
// Theme.kt (auto-generated)
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
```

Material 3 (Material You) introduces **dynamic color** that adapts to the user's wallpaper on Android 12+.

### Preview Annotation

```kotlin
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}
```

`@Preview` lets you see the UI directly in Android Studio without running the app on a device.

### Compose vs Traditional Views

| Aspect | XML + Views | Jetpack Compose |
|---|---|---|
| UI definition | XML files | Kotlin functions |
| UI updates | Imperative (`setText()`) | Declarative (state drives UI) |
| Reusability | Custom Views | Composable functions |
| Tooling | Layout Editor | Interactive Preview |
| Performance | View hierarchy | Optimized recomposition |
| Learning curve | Established | Modern (recommended) |

### Kotlin Features Used

- **Lambda expressions**: `setContent { }`, `onClick = { }`
- **String templates**: `"Hello $name!"`
- **Default parameters**: `modifier: Modifier = Modifier`
- **Extension functions**: Built throughout Compose APIs

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Key Dependencies:**
  - `compose-bom` (Bill of Materials for consistent Compose versions)
  - `androidx.compose.ui`
  - `androidx.compose.material3`
  - `androidx.activity:activity-compose`
