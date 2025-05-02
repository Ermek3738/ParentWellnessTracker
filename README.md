# Parent Wellness Tracker

## Overview

Parent Wellness Tracker is a mobile application designed to facilitate health monitoring and caregiver coordination for elderly users. The application uses simulated health data to demonstrate core functionality while prioritizing accessibility and relationship-based care management.

## Key Features

- **Health Monitoring**: Visualizes key health metrics (heart rate, blood pressure, blood sugar, activity) with clear indicators for normal and concerning values
- **Caregiver Coordination**: Implements a bidirectional relationship model for caregivers and elderly users with permission-based health data access
- **Elderly-Focused Design**: Prioritizes accessibility with large text, high contrast, simplified navigation, and error-tolerant interaction
- **Firebase Integration**: Utilizes Firebase Authentication, Cloud Firestore, and Security Rules to ensure secure data management
- **Experimental Watch Integration**: Initial integration with Samsung Galaxy Watch for direct health monitoring

## Technologies Used

- **Language**: Kotlin 1.8.0
- **UI Framework**: Jetpack Compose 1.4.0
- **Backend**: Firebase Authentication 22.0.0, Cloud Firestore 24.6.0
- **Architecture**: MVVM (Model-View-ViewModel)
- **Other Libraries**:
  - Coroutines 1.6.4 (asynchronous operations)
  - ViewModel 2.6.0 (UI data management)
  - Navigation Compose 2.5.3 (screen navigation)
  - Material 3 1.0.1 (UI components)

## System Requirements

- **Development Environment**: Android Studio Electric Eel 2022.1.1 or higher
- **Minimum SDK**: Android API Level 26 (Android 8.0)
- **Target SDK**: Android API Level 33 (Android 13)
- **Firebase Project**: Required for authentication and data storage

## Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Ermek3738/parent-wellness-tracker.git
   cd parent-wellness-tracker
   ```

2. **Firebase Setup**:
   - Create a Firebase project at [firebase.google.com](https://firebase.google.com)
   - Enable Authentication (Email/Password)
   - Create a Cloud Firestore database
   - Add an Android app to your Firebase project
   - Download the `google-services.json` file and place it in the app directory
   - Implement the security rules provided in the `firebase/firestore.rules` file

3. **Build and Run**:
   - Open the project in Android Studio
   - Sync Gradle files
   - Build and run the application on an emulator or physical device

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/parentwellnesstracker/
│   │   │   ├── data/                  # Data layer
│   │   │   │   ├── repositories/      # Repository implementations
│   │   │   │   ├── models/            # Data models
│   │   │   │   └── sources/           # Data sources including simulations
│   │   │   ├── domain/                # Domain layer
│   │   │   │   ├── usecases/          # Business logic
│   │   │   │   └── utils/             # Utility functions
│   │   │   └── presentation/          # Presentation layer
│   │   │       ├── components/        # Reusable UI components
│   │   │       ├── screens/           # Application screens
│   │   │       └── viewmodels/        # ViewModels for each screen
│   │   └── res/                       # Resources
│   └── test/                          # Unit tests
└── build.gradle                       # App-level Gradle configuration
```

## Usage

### For Elderly Users
1. Register a new account as a "Parent" user
2. Complete the profile setup with personal details
3. View health metrics on the dashboard
4. Add caregivers by entering their email addresses
5. Manage caregiver permissions through the Manage Caregivers screen

### For Caregivers
1. Register a new account as a "Caregiver" user
2. Accept invitation from elderly user
3. Monitor health metrics for connected elderly users
4. Receive notifications for concerning health patterns
5. Manage notification settings

## Simulated Health Data

The application uses simulated health data instead of direct wearable device integration. This simulation provides realistic patterns based on:
- Age and gender-appropriate baseline values
- Daily activity patterns
- Occasional anomalies to test the notification system

To observe the notification system in action, the simulation will periodically generate out-of-range values.

## Experimental Watch Integration

The repository includes an experimental implementation for Samsung Galaxy Watch integration based on Wear OS 4. This integration demonstrates:
- Real-time heart rate monitoring
- Alert indicators for abnormal readings
- Firebase Realtime Database integration

## Security and Privacy

- All health data is secured using Firebase Security Rules
- Only authorized caregivers can access an elderly user's health data
- Elderly users maintain control over which data is shared with each caregiver
- No health data is stored locally in an unencrypted format

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a pull request

Please follow the coding standards and add appropriate tests for new features.

## Future Development

- Integration with real wearable devices beyond the experimental implementation
- Medical professional portal for healthcare provider access
- Multilingual support
- Predictive analytics for early health issue detection
- Voice-first interface for users with limited dexterity

## Acknowledgments

- Health Samples Repository for the ExerciseSampleCompose code
- Firebase for backend infrastructure
- Participants in usability testing for valuable feedback

## Contact

For questions or feedback, please contact:
- Email: ermekilikeshova@gmail.com

---

© 2025 Ermek Ilikeshova - Final Year Project
