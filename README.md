# HiNurse
# HiNurse - Campus Health Assistant

HiNurse is a thoughtfully crafted Android app designed to streamline campus clinic workflows and deliver personalized care to students and staff. Built with modern Android development practices and Firebase Firestore integration.

## 🏥 Features

### Core Functionality
- **Secure Authentication**: Firebase Authentication with email/password
- **Main Dashboard**: Personalized greeting with five core options
- **Smart Triage**: AI-powered symptom analysis for personalized care recommendations
- **Ask a Nurse**: Submit health questions and receive responses from clinic staff
- **Book Appointment**: Calendar-based scheduling with time slot selection
- **Health Tips**: Curated wellness advice in a scrollable format
- **Medical Records**: Secure access to personal health history
- **Real-time Chat**: Messaging with clinic staff
- **User Profile**: Personal information management
- **Activity Log**: Track user actions and app usage

### Technical Features
- **Firebase Firestore**: Real-time data synchronization
- **Modern UI**: Clean, responsive layouts with Material Design
- **Modular Architecture**: Well-organized, maintainable code structure
- **Data Models**: Comprehensive models for all app entities
- **Navigation**: Intuitive bottom navigation and activity transitions

## 🏗️ Architecture

### Data Models
- `User`: User profile and authentication data
- `Appointment`: Appointment scheduling and management
- `HealthQuestion`: Health inquiries and responses
- `ChatMessage`: Real-time messaging
- `MedicalRecord`: Health history and records
- `SymptomAnalysis`: AI-powered symptom assessment and recommendations

### Activities
- `SignInActivity`: User authentication
- `SignUpActivity`: User registration with comprehensive profile creation
- `MainDashboardActivity`: Main app interface
- `SmartTriageActivity`: AI-powered symptom analysis
- `AskNurseActivity`: Health question submission
- `AppointmentBookingActivity`: Appointment scheduling
- `HealthTipsActivity`: Wellness advice
- `MedicalRecordsActivity`: Health records access
- `ChatActivity`: Real-time messaging
- `UserProfileActivity`: Profile management
- `ActivityLogActivity`: Usage tracking

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Firebase project setup
- Android device or emulator (API 29+)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd HiNurse20
   ```

2. **Firebase Configuration**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Enable Authentication and Firestore Database
   - Download `google-services.json` and place it in the `app/` directory
   - Configure Authentication to allow email/password sign-in

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

### Firebase Setup Details

1. **Authentication**
   - Go to Firebase Console > Authentication > Sign-in method
   - Enable Email/Password authentication

2. **Firestore Database**
   - Go to Firebase Console > Firestore Database
   - Create database in production mode
   - Set up security rules for your collections

3. **Collections Structure**
   ```
   users/
   ├── {userId}/
   │   ├── userId: string
   │   ├── email: string
   │   ├── firstName: string
   │   ├── lastName: string
   │   └── ...
   
   appointments/
   ├── {appointmentId}/
   │   ├── appointmentId: string
   │   ├── userId: string
   │   ├── appointmentDate: timestamp
   │   └── ...
   
   health_questions/
   ├── {questionId}/
   │   ├── questionId: string
   │   ├── userId: string
   │   ├── question: string
   │   └── ...
   
   chat_messages/
   ├── {messageId}/
   │   ├── messageId: string
   │   ├── chatId: string
   │   ├── message: string
   │   └── ...
   
   medical_records/
   ├── {recordId}/
   │   ├── recordId: string
   │   ├── userId: string
   │   ├── recordType: string
   │   └── ...
   
   symptom_analyses/
   ├── {analysisId}/
   │   ├── analysisId: string
   │   ├── userId: string
   │   ├── symptoms: array
   │   ├── severity: string
   │   ├── urgency: string
   │   ├── recommendation: string
   │   └── ...
   ```

## 📱 User Interface

### Design Principles
- **Clean & Modern**: Minimalist design with focus on usability
- **Responsive**: Adapts to different screen sizes
- **Accessible**: Clear typography and intuitive navigation
- **Consistent**: Unified color scheme and component styling

### Color Scheme
- Primary: Black (#000000)
- Secondary: Light Blue (#E3F2FD)
- Accent: Dark Blue (#1976D2)
- Background: White (#FFFFFF)
- Surface: Light Gray (#F5F5F5)

## 🔧 Development

### Project Structure
```
app/
├── src/main/
│   ├── java/com/example/hinurse20/
│   │   ├── models/           # Data model classes
│   │   ├── SignInActivity.java
│   │   ├── MainDashboardActivity.java
│   │   └── ...
│   ├── res/
│   │   ├── layout/           # XML layouts
│   │   ├── values/           # Colors, strings, themes
│   │   └── drawable/         # Custom drawables
│   └── AndroidManifest.xml
├── build.gradle.kts          # App-level dependencies
└── google-services.json      # Firebase configuration
```

### Key Dependencies
- **Firebase BOM**: Unified Firebase version management
- **Firebase Auth**: User authentication
- **Firebase Firestore**: NoSQL database
- **Material Design**: UI components
- **ConstraintLayout**: Flexible layouts
- **RecyclerView**: Efficient list rendering

## 📋 Features Implementation

### Authentication Flow
1. User opens app → SignInActivity
2. Enter credentials or tap "Sign Up" button
3. SignUpActivity for new users with comprehensive form
4. Firebase Authentication validates credentials
5. User profile created in Firestore with full details
6. Navigate to MainDashboardActivity

### Main Dashboard
- Personalized greeting with user's name
- Four core feature cards in grid layout
- Recent activity display
- Bottom navigation for quick access

### Smart Triage
- AI-powered symptom analysis and assessment
- Intelligent urgency and severity evaluation
- Personalized care recommendations
- Self-care tips and guidance
- Priority-based appointment scheduling
- Reduces unnecessary clinic visits

### Ask a Nurse
- Submit health questions with categories
- View question history and responses
- Real-time status updates

### Appointment Booking
- Calendar view for date selection
- Time slot picker
- Nurse selection
- Reason specification
- Confirmation and booking

### Health Tips
- Curated wellness advice
- Scrollable list format
- Categorized tips for easy browsing

### Medical Records
- Secure health history access
- Chronological record display
- Detailed record information

### Chat System
- Real-time messaging with clinic staff
- Message history
- User and nurse message differentiation

### User Profile
- Personal information management
- Emergency contact details
- Profile updates with Firestore sync

## 🔒 Security

### Data Protection
- Firebase Authentication for secure user management
- Firestore security rules for data access control
- Encrypted data transmission
- User-specific data isolation

### Privacy
- Personal health information protection
- Secure chat messaging
- Controlled data access permissions

## 🚀 Deployment

### Build Configuration
- Target SDK: 36
- Minimum SDK: 29
- Java 11 compatibility
- ProGuard optimization for release builds

### Release Process
1. Update version code and name
2. Configure signing keys
3. Build release APK
4. Test on multiple devices
5. Deploy to Google Play Store

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

## 🔮 Future Enhancements

- Push notifications for appointment reminders
- Offline data synchronization
- Advanced health tracking features
- Integration with wearable devices
- Multi-language support
- Dark mode theme
- Advanced analytics and reporting

---

**HiNurse** - Streamlining campus health services with modern technology and user-centered design.
