# Telemetry Lab

A high-performance Android application that simulates edge computing workloads while maintaining smooth UI performance. The app demonstrates best practices for background processing, power efficiency, and performance monitoring in Android.

## Features

- Real-time frame processing at 20Hz (10Hz in battery saver mode)
- Adjustable compute load (1-5)
- Performance monitoring with JankStats
- Battery-aware operation
- Foreground Service implementation for background processing
- Clean, responsive UI built with Jetpack Compose

## Technical Implementation

### Architecture
- **MVVM** architecture with ViewModel for UI state management
- **Coroutines** for asynchronous operations
- **Flow** for reactive UI updates
- **Dependency Injection** with Hilt (optional but recommended)

### Performance Optimizations
- Offloaded compute-intensive operations to background threads
- Used `derivedStateOf` for efficient UI updates
- Implemented backpressure handling to prevent UI jank
- Used stable types and keys in Compose for optimal recomposition

### Foreground Service Implementation
- **Service Type**: `dataSync` (Android 14+)
- **Rationale**: 
  - The app performs continuous data processing (20Hz frame simulation)
  - Matches the pattern of scheduled data processing similar to data synchronization
  - Provides better system resource management and user transparency
  - Aligns with Android 14's FGS type system

## Performance Metrics

### JankStats Results
- **Target**: ≤5% jank at load=2
- **Actual Performance**: [Your jank percentage here]% jank at load=2

### Battery Saver Mode
- Automatically reduces frame rate to 10Hz
- Reduces compute load by 1 (minimum 1)
- Shows a "Power-save mode" banner when active

## Setup

### Prerequisites
- Android Studio Flamingo (2022.2.1) or newer
- Android SDK 34 (Android 14)
- Kotlin 1.9.0 or newer

### Building the Project
1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Run the app on a physical device or emulator (API 34 recommended)

## Testing

### Unit Tests
Run unit tests using:
```bash
./gradlew test
```

### Instrumentation Tests
Run UI tests using:
```bash
./gradlew connectedAndroidTest
```

## Performance Testing

### JankStats Integration
JankStats is integrated to monitor and log UI jank. The dashboard displays:
- Current frame latency (ms)
- Moving average latency
- Jank percentage (last 30s)
- Total jank frame count

### Macrobenchmark (Bonus)
[Include before/after results if implemented]

## Implementation Details

### Threading Model
- **Main Thread**: Handles UI updates and user interactions
- **Default Dispatcher**: For non-blocking operations
- **IO Dispatcher**: For disk I/O operations (if any)
- **Default Dispatcher (with limited parallelism)**: For compute-intensive tasks

### Backpressure Handling
- Used `buffer()` and `conflate()` operators to handle backpressure
- Implemented frame dropping strategy during high load to maintain UI responsiveness
- Used `stateIn` with `SharingStarted.WhileSubscribed()` for efficient state management

## Screenshots
[Add screenshots of the app in action]

## Video Demo
[Link to video demonstration]

## Known Issues
- [List any known issues or limitations]

## Future Improvements
- Implement WorkManager for non-interactive batch processing
- Add more detailed performance analytics
- Implement adaptive frame rate based on device capabilities
- Add support for dark/light theme

