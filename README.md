# Compose Lab

A single-module Jetpack Compose sandbox for trying out Compose, Material 3 and
[Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3) patterns.
Each experiment lives in its own file; the app shell never changes.

Not a production template — experiments are meant to be small, ugly, and thrown away.

## Stack

| | |
|---|---|
| AGP | 9.5.2 |
| Kotlin | 2.4.21 (Compose, Serialization plugins) |
| Compose BOM | 2026.11.01 · Material 3 |
| Navigation 3 | 1.2.2 |
| Gradle | 9.6.0 (daemon toolchain: JDK 25) |
| compileSdk / targetSdk | 37 | 
| minSdk | 26 |

All versions are declared in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) — no version
strings in build scripts.

## Build & run

```bash
# Windows
gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

Or just open the project in Android Studio and hit Run. The first build downloads the JDK 25
toolchain declared in [`gradle/gradle-daemon-jvm.properties`](gradle/gradle-daemon-jvm.properties).

## How it's put together

```
app/src/main/java/dev/hyuwah/composelab/
├── MainActivity.kt              single activity, sets ComposeLabNavHost
├── nav/
│   ├── Destinations.kt          @Serializable NavKey route objects/data classes
│   └── ComposeLabNavHost.kt     NavDisplay + back stack (rememberNavBackStack)
└── ui/
    ├── experiments/
    │   ├── Experiment.kt            the interface every experiment implements
    │   ├── ExperimentListScreen.kt  home list
    │   ├── ExperimentScaffold.kt    shared top bar + description chrome
    │   └── ExperimentRegistry.kt    the one central list
    ├── playground/
    │   ├── button/ButtonExperiment.kt
    │   └── toggle/ToggleExperiment.kt
    └── theme/                   Material 3 theme
```

Navigation is back-stack-as-state: `rememberNavBackStack(Home)` returns a `MutableList`, so
`add(...)` pushes and `removeLastOrNull()` pops. No `NavController`, no string routes.

## Adding an experiment

1. Create `ui/playground/<name>/<Name>Experiment.kt` implementing `Experiment`:

   ```kotlin
   object WaveExperiment : Experiment {
       override val id = "wave"
       override val title = "Wave"
       override val description = "Animated loading wave"
       @Composable
       override fun Content(onBack: () -> Unit, modifier: Modifier) {
           ExperimentScaffold(title, description, onBack, modifier) { /* canvas */ }
       }
   }
   ```

2. Add it to the list in
   [`ExperimentRegistry.kt`](app/src/main/java/dev/hyuwah/composelab/ui/experiments/ExperimentRegistry.kt).

That's the whole wiring — the nav host already has a single `entry<Experiment>` branch and the list
screen renders whatever the registry exposes.


## License

MIT — see [LICENSE](LICENSE).
