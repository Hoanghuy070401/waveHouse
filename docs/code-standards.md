# Code Standards

## Architectural Pattern
- **MVVM**: The app uses ViewModels combined with Repositories and clean UI patterns.
- **Dependency Injection**: Handled heavily by Dagger Hilt.

## Language and Styling Core
- **Kotlin**: Primary language. Follow standard Kotlin idiomatic patterns and conventions.
- **Coroutines & RxJava**: Use Coroutines for asynchronous operations; existing RxJava usages should be encapsulated in repositories or migrated.
- **XML Layouts**: Use DataBinding and ViewBinding for view interactions.
- **Formatting**: Adhere to default Android Studio Kotlin formatting conventions (ktlint standard).
