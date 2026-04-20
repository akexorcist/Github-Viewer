plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopTest by getting {
            dependencies {
                implementation(projects.core.common)
                implementation(projects.core.network)
                implementation(projects.core.database)
                implementation(projects.data)
                implementation(projects.presentation)

                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.kotest.assertions.core)
                implementation(libs.koin.core)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }
    }
}

// Allow running integration tests independently with:
// ./gradlew :integration-test:desktopTest
tasks.named<Test>("desktopTest") {
    description = "Runs integration tests against the real GitHub API."
    group = "verification"
    // Run from the root project directory so that File("integration-test/.env")
    // in TestEnvironment resolves correctly.
    workingDir = rootProject.projectDir
    // Show HTTP request/response logs from the test process on the console.
    testLogging {
        showStandardStreams = true
    }
}
