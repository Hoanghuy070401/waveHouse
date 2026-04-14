# System Architecture

## Architecture Diagram Overview
The application architecture is layered into the following components:
1. **User Interface (UI) Layer**: Activities and Fragments handling the layout and ViewBinding. Subscribing to ViewModel states.
2. **Presentation Layer**: ViewModels handling presentation logic and emitting states via LiveData or StateFlow.
3. **Data Layer**: Repositories abstracting the exact data sources. Remote responses parsed via GSON, retrieved by Retrofit.

## Third-Party Integrations
- **Map engine**: MapLibre GL for custom maps and routing.
- **3D engine**: Filament / Sceneview for rendering advanced graphics and 3D overlays.
- **Navigation/Maps**: Play Services integrations.
