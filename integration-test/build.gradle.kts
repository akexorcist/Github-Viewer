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
                implementation(libs.koin.core)
            }
        }
    }
}

// Allow running integration tests independently with:
// ./gradlew :integration-test:desktopTest
tasks.named<Test>("desktopTest") {
    description = "Runs integration tests against the real GitHub API."
    group = "verification"
}
