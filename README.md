# 🍅 Pomodoro Timer for Wear OS

A modern, Material 3 expressive Pomodoro timer built specifically for Wear OS. This app helps you stay productive with focused work sessions and restorative breaks, all from your wrist.

![Wear OS](https://img.shields.io/badge/Platform-Wear%20OS-blue?logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-green?logo=jetpackcompose)

## ✨ Features

- **Material 3 UI**: A sleek, responsive design using the latest Wear OS components.
- **Smart Session Management**: Automatically cycles through Work, Short Breaks, and Long Breaks.
- **Ongoing Activity**: Track your timer even when the app is in the background via the Ongoing Activity API.
- **Tactile Feedback**: Strong vibration alerts when a session finishes.
- **High-Priority Notifications**: Never miss a break with reliable alert channels.
- **Native Performance**: Lag-free scrolling and optimized battery usage.

## 🛠 Tech Stack

- **Jetpack Compose for Wear OS**: For a modern, declarative UI.
- **Kotlin Coroutines & Flow**: For reactive state management.
- **Wearable Ongoing Activity**: To keep the timer visible on the watch face.
- **DataStore Preferences**: For persistent settings and timer state restoration.
- **Material 3**: Utilizing the latest expressive design system.

## 🚀 Development

### Native Gradle Scripts
We've added convenience tasks to make development faster:
- `./gradlew fastCheck`: Quick lint check (used by pre-commit hook).
- `./gradlew fullCheck`: Runs full lint, unit tests, and build.
- `./gradlew install`: Installs the debug app on your connected watch.

### Quality Control
- **Pre-commit Hook**: Automatically runs a fast lint check before every commit.
- **Unit Tests**: Session transition logic is verified by a JUnit 5 suite.
- **Compose UI Tests**: Verifies UI integrity on real devices/emulators.

## 👷 CI/CD

This project uses **GitHub Actions** for automated quality assurance and delivery:

1. **Android CI**: Runs on every push to `main`. It performs a full lint check, unit tests, and a project build.
2. **Release Workflow**: 
   - **Automated**: Triggers when a tag (e.g., `v1.0.0`) is pushed.
   - **Manual**: Can be run from the Actions tab where you can specify a custom `versionName`.
   - **Artifacts**: Generates both signed App Bundles (.aab) and APKs (.apk).

---
*Built with ❤️ for Wear OS.*
