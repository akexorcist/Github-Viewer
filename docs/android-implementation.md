# GitHub Viewer — Android Implementation Details

This document covers Android-specific implementation concerns that are separate from the shared KMP layer.

---

## App Configuration

```kotlin
// app/build.gradle.kts
android {
    namespace = "dev.akexorcist.githubviewer"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.akexorcist.githubviewer"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

---

## Build Tooling

### KSP (Required for Room 2.7+)

Room 2.7+ drops KAPT and requires KSP. KAPT must not be used.

```kotlin
// root build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "{ksp_version}" apply false
}

// core/database/build.gradle.kts
plugins {
    id("com.google.devtools.ksp")
}
```

KSP version must be aligned with the Kotlin version. Check compatibility at the KSP releases page.

### Compose Compiler Plugin (Kotlin 2.x)

Kotlin 2.x uses a standalone Compose compiler Gradle plugin instead of `kotlinCompilerExtensionVersion`.

```kotlin
// root build.gradle.kts
plugins {
    id("org.jetbrains.kotlin.plugin.compose") version "{kotlin_version}" apply false
}

// app/build.gradle.kts
plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    buildFeatures {
        compose = true
    }
    // No composeOptions block needed — plugin handles it for Kotlin 2.x
}
```

---

## Permissions

```xml
<!-- app/src/main/AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
```

No other runtime permissions are required.

---

## Build Types

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing config applied separately via local keystore properties
        }
    }
}
```

---

## ProGuard / R8 Rules

File: `app/proguard-rules.pro`

```proguard
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    kotlinx.serialization.descriptors.SerialDescriptor descriptor;
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coil
-dontwarn coil.**
```

---

## Theme

Material 3 with dark and light mode support. Dynamic Color (Material You) is disabled.

### Color Scheme

Two fixed color schemes are defined: one for light mode, one for dark mode. The system theme preference determines which is applied. No runtime color extraction from wallpaper.

```kotlin
// app/src/main/kotlin/dev/akexorcist/githubviewer/ui/theme/Theme.kt

@Composable
fun GithubViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GithubViewerTypography,
        content = content
    )
}
```

No `dynamicLightColorScheme` or `dynamicDarkColorScheme` — static schemes only.

### Theme Files Structure

```
app/src/main/kotlin/dev/akexorcist/githubviewer/ui/theme/
├── Color.kt        — light and dark color values
├── Theme.kt        — GithubViewerTheme composable
└── Type.kt         — typography scale
```

---

## Splash Screen

Uses the `androidx.core:core-splashscreen` library (Android 12+ SplashScreen API backport).

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("androidx.core:core-splashscreen:{version}")
}
```

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // must be called before super.onCreate
        super.onCreate(savedInstanceState)
        // ...
    }
}
```

```xml
<!-- app/src/main/res/values/themes.xml -->
<style name="Theme.GithubViewer.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/splash_background</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash</item>
    <item name="postSplashScreenTheme">@style/Theme.GithubViewer</item>
</style>

<!-- AndroidManifest.xml — apply to MainActivity -->
<activity android:theme="@style/Theme.GithubViewer.Splash" ... />
```

---

## Edge-to-Edge

Android 15 enforces edge-to-edge by default. The app must handle window insets on all screens.

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()      // sets up edge-to-edge rendering
        setContent {
            GithubViewerTheme {
                GithubViewerApp()
            }
        }
    }
}
```

All screens use `Scaffold` which automatically handles `contentWindowInsets`. For custom layouts that don't use `Scaffold`, apply `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` explicitly.

```kotlin
// Pattern for screens using Scaffold
Scaffold(
    topBar = { ... },
    // Scaffold handles top/bottom insets automatically
) { innerPadding ->
    Content(modifier = Modifier.padding(innerPadding))
}
```

---

## Navigation

Jetpack Navigation Compose with type-safe routes (`2.8+`). Routes are defined as serializable data classes/objects.

### Route Definitions

```kotlin
// app/src/main/kotlin/dev/akexorcist/githubviewer/navigation/Routes.kt

@Serializable
object SearchRoute

@Serializable
data class UserProfileRoute(val login: String)

@Serializable
data class RepositoryDetailRoute(val owner: String, val repo: String)
```

### Navigation Flow

```
SearchRoute (start)
    ├── user card tap → UserProfileRoute(login)
    │       └── repo item tap → RepositoryDetailRoute(owner, repo)
    └── repo card tap → RepositoryDetailRoute(owner, repo)
```

Repository Detail shows owner login as plain text only. There is no navigation from Repository Detail to the owner's profile.

### NavHost

```kotlin
// app/src/main/kotlin/dev/akexorcist/githubviewer/navigation/GithubViewerNavHost.kt

@Composable
fun GithubViewerNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = SearchRoute
    ) {
        composable<SearchRoute> {
            SearchScreen(
                onUserClick = { login -> navController.navigate(UserProfileRoute(login)) },
                onRepoClick = { owner, repo -> navController.navigate(RepositoryDetailRoute(owner, repo)) }
            )
        }
        composable<UserProfileRoute> { backStackEntry ->
            val route: UserProfileRoute = backStackEntry.toRoute()
            UserProfileScreen(
                login = route.login,
                onRepoClick = { owner, repo -> navController.navigate(RepositoryDetailRoute(owner, repo)) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable<RepositoryDetailRoute> { backStackEntry ->
            val route: RepositoryDetailRoute = backStackEntry.toRoute()
            RepositoryDetailScreen(
                owner = route.owner,
                repo = route.repo,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
```

---

## SavedStateHandle (Process Death Survival)

ViewModels for `UserProfileRoute` and `RepositoryDetailRoute` receive their navigation arguments via `SavedStateHandle`. Type-safe routes backed by Navigation Compose automatically populate `SavedStateHandle` from the route arguments, so these survive process death without extra code.

```kotlin
// presentation/src/commonMain/kotlin/.../UserProfileViewModel.kt
class UserProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
) : ViewModel() {
    // Navigation Compose populates this from UserProfileRoute.login automatically
    private val login: String = savedStateHandle.toRoute<UserProfileRoute>().login
}

// presentation/src/commonMain/kotlin/.../RepositoryDetailViewModel.kt
class RepositoryDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repoRepository: RepoRepository,
) : ViewModel() {
    private val owner: String = savedStateHandle.toRoute<RepositoryDetailRoute>().owner
    private val repo: String = savedStateHandle.toRoute<RepositoryDetailRoute>().repo
}
```

`SavedStateHandle.toRoute<T>()` is available from `navigation-compose 2.8+` and works with the KMP `lifecycle-viewmodel` artifact.

---

## Offline / No Network — Snackbar

The snackbar is the single mechanism for communicating network unavailability when cached data is present.

### Trigger Conditions

| Situation | Behavior |
|---|---|
| No network + empty cache | Full-screen error state with retry button |
| No network + cache has data | Show cached data + snackbar "No internet connection" |
| Manual refresh fails (no network) | Keep cached data visible + snackbar "No internet connection" |
| Manual refresh fails (rate limit) | Keep cached data visible + snackbar "Rate limit reached, resets at HH:mm" |

### Implementation Pattern

Each screen's `Scaffold` hosts a `SnackbarHost`. The ViewModel emits one-shot events for snackbar messages via a `Channel` or `SharedFlow` (not part of `UiState`, since snackbars are fire-and-forget):

```kotlin
// In ViewModel (commonMain — no Android dependency)
val snackbarEvent: SharedFlow<SnackbarMessage>

sealed class SnackbarMessage {
    data object NoInternet : SnackbarMessage()
    data class RateLimit(val resetAt: Instant) : SnackbarMessage()
}
```

```kotlin
// In Compose screen (androidMain/app)
val snackbarHostState = remember { SnackbarHostState() }

LaunchedEffect(Unit) {
    viewModel.snackbarEvent.collect { message ->
        snackbarHostState.showSnackbar(
            message = when (message) {
                is SnackbarMessage.NoInternet -> "No internet connection"
                is SnackbarMessage.RateLimit -> "Rate limit reached, resets at ${message.resetAt.format()}"
            }
        )
    }
}

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
) { ... }
```

---

## Dependency Injection (Koin — Android side)

The shared modules (network, database, data, presentation) are defined in `commonMain`. The Android `app` module provides the Koin start point and any Android-specific bindings.

```kotlin
// app/src/main/kotlin/dev/akexorcist/githubviewer/GithubViewerApplication.kt
class GithubViewerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GithubViewerApplication)
            modules(
                networkModule,      // core/network commonMain
                databaseModule,     // core/database androidMain (AndroidSQLiteDriver)
                dataModule,         // data commonMain
                presentationModule, // presentation commonMain
            )
        }
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<application
    android:name=".GithubViewerApplication"
    ... />
```

---

## Android Instrumentation Tests

No Android instrumentation tests (Espresso / Compose UI Test) are in scope. All non-UI layer testing is done via Desktop JVM (see `integration-test` module and `commonTest` source sets). UI correctness is verified manually during development.
