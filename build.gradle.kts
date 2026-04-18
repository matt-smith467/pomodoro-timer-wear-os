// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Convenience Tasks (Android-style scripts)
tasks.register("fastCheck") {
    group = "verification"
    description = "Quick lint check (debug only)"
    dependsOn(":app:lintDebug")
}

tasks.register("fullCheck") {
    group = "verification"
    description = "Full lint, unit tests, and build"
    dependsOn(":app:lint", ":app:testDebugUnitTest", ":app:build")
}

tasks.register("install") {
    group = "install"
    description = "Install the debug app on a device"
    dependsOn(":app:installDebug")
}