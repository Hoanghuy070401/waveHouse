# Codebase Summary

## Overview
The codebase is a native Android application written primarily in Kotlin, making use of the Gradle build system. The architecture relies on modern Android development patterns, particularly MVVM (Model-View-ViewModel) and Clean Architecture principles.

## Structure
- `app/src/main/java/com/gtelots/maps/`
  - `ui/`: Contains Activities, Fragments, and Adapters (e.g., `MainActivity.kt`, `DemoActivity.kt`, `SearchFragment.kt`).
  - `data/`: Contains network models, API service configurations (Retrofit).
  - `utils/`: Common utilities and state handling (`State.kt`).
- `app/src/main/res/`: Resources such as layouts, drawables, and values.
- `gradle/, app/build.gradle`: Project architecture utilizes Gradle KTS/Groovy structures integrating map libraries and dependencies.
