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
    │   └── button/ButtonExperiment.kt
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


## Animation article material

Two dev.to articles are written against this repo. Prose lives outside the repo; each chapter here is
one file that compiles, previews and teaches exactly one idea. Every chapter exposes its tuning numbers
as named constants (feel is the reader's call) and every catalog has a freeze + scrub control so a
single animation frame can be inspected.

### Article 1 — Button beyond Material

| Chapter | File | Compose APIs |
|---|---|---|
| 1. Bounce | [`ui/playground/button/BounceChapter.kt`](app/src/main/java/dev/hyuwah/composelab/ui/playground/button/BounceChapter.kt) | `animateFloatAsState`, `collectIsPressedAsState`, `Modifier.graphicsLayer` |
| 2. Interruption | [`ui/playground/button/InterruptionChapter.kt`](app/src/main/java/dev/hyuwah/composelab/ui/playground/button/InterruptionChapter.kt) | `Animatable`, `animateTo`, `spring(initialVelocity = …)`, `pointerInput` + `detectTapGestures` |
| 3. Morph | [`ui/playground/button/MorphChapter.kt`](app/src/main/java/dev/hyuwah/composelab/ui/playground/button/MorphChapter.kt) | `Animatable<Float>`, `lerp`, `RoundedCornerShape`, `AnimatedContent` + `SizeTransform` |
| 4. Polish | [`ui/playground/button/PolishChapter.kt`](app/src/main/java/dev/hyuwah/composelab/ui/playground/button/PolishChapter.kt) | `semantics { stateDescription }`, `liveRegion`, `indication`, `clickable` + `clip` |
| Catalog | [`ui/playground/button/ButtonExperiment.kt`](app/src/main/java/dev/hyuwah/composelab/ui/playground/button/ButtonExperiment.kt) | renders chapters 1–4 in order |

Shared primitives (container, `ActionState`, `ActionRunner`, `ScrubRow`) live in
[`ButtonBase.kt`](app/src/main/java/dev/hyuwah/composelab/ui/playground/button/ButtonBase.kt).

### Article 2 — Speak button case study

| Chapter | File | Compose APIs |
|---|---|---|
| 1. Glow | [`ui/casestudy/speak/GlowLayer.kt`](app/src/main/java/dev/hyuwah/composelab/ui/casestudy/speak/GlowLayer.kt) | `Canvas`, `Brush.radialGradient`, `BlendMode.Plus` |
| 2. Fake amplitude | [`ui/casestudy/speak/Amplitude.kt`](app/src/main/java/dev/hyuwah/composelab/ui/casestudy/speak/Amplitude.kt) | `State<Float>`, `withFrameNanos`, deterministic sine + value noise |
| 3. CTA morph | [`ui/casestudy/speak/CtaMorph.kt`](app/src/main/java/dev/hyuwah/composelab/ui/casestudy/speak/CtaMorph.kt) | reuses `ActionState` + `rememberMorphVisuals`, `detectTapGestures` press/release |
| 4. Composition | [`ui/casestudy/speak/SpeakScreen.kt`](app/src/main/java/dev/hyuwah/composelab/ui/casestudy/speak/SpeakScreen.kt) | glow + amplitude + CTA wired together, freeze + amplitude scrub |

No microphone anywhere: `android.media.*` and `RECORD_AUDIO` are not used, the amplitude is generated.

### Tests

Value tests, not appearance tests — they pin what a progress value turns into:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.hyuwah.composelab.playground.*' \
                                 --tests 'dev.hyuwah.composelab.casestudy.*'
```

| Test | File | Pins |
|---|---|---|
| `BounceMotionTest` | `test/.../playground/BounceMotionTest.kt` | scale/lift at progress 0, 0.5, 1 |
| `MorphMotionTest` | `test/.../playground/MorphMotionTest.kt` | corner + color lerp at t 0, 0.5, 1 |
| `FakeAmplitudeTest` | `test/.../casestudy/FakeAmplitudeTest.kt` | determinism for a fixed seed, 0..1 range, repeat period |

## License

MIT — see [LICENSE](LICENSE).
