// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.diffplug.spotless") version "6.25.0"
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint("1.2.1")
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("**/*.kts")
            targetExclude("${layout.buildDirectory}/**/*.kts")
            ktlint("1.2.1")
        }
    }
}

// Convenience Tasks (Android-style scripts)
tasks.register("fastCheck") {
    group = "verification"
    description = "Quick lint check and format check"
    dependsOn("spotlessCheck", ":app:lintDebug")
}

tasks.register("format") {
    group = "formatting"
    description = "Fix code formatting issues"
    dependsOn("spotlessApply")
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