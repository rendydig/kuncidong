# Smart Lock Screen - Android Application

## Overview
Smart Lock Screen is an educational Android application built in Kotlin that transforms the traditional lock screen experience into a learning opportunity for children. Instead of simply swiping to unlock, kids must answer randomly generated educational questions to access their device.

## Features Implemented

### 🔒 **Custom Lock Screen**
- Replaces the default Android lock screen
- Secure locking and unlocking mechanism
- Child-friendly interface design with modern Material Design

### 🧠 **Educational Questions**
- 30+ pre-loaded questions across 6 categories:
  - Mathematics (basic arithmetic, percentages, powers)
  - Science (chemistry, biology, physics basics)
  - Geography (countries, capitals, continents)
  - History (world events, famous figures)
  - Language (grammar, vocabulary, spelling)
  - General Knowledge (mixed educational content)
- Multiple choice answer format (4 options per question)
- Random question selection for each unlock attempt

### 🎯 **Smart Learning System**
- Visual feedback for correct/incorrect answers
- Color-coded answer buttons (green for correct, red for incorrect)
- Encourages retry on wrong answers with new questions
- Real-time clock and date display on lock screen

### 🔧 **Technical Implementation**
- **Device Admin Integration**: Uses Android Device Admin API for secure lock screen control
- **Foreground Service**: Persistent background service to monitor screen events
- **Boot Receiver**: Automatically starts service after device reboot
- **Modern Architecture**: Clean separation of concerns with data models and repositories
- **Material Design**: Beautiful, kid-friendly UI with custom themes and colors

## Project Structure

```
app/src/main/
├── java/com/rendy/customlockscreen/
│   ├── data/
│   │   ├── Question.kt                 # Question data model
│   │   └── QuestionRepository.kt       # Question data management
│   ├── SmartLockApplication.kt         # Application class
│   ├── MainActivity.kt                 # Setup and configuration activity
│   ├── LockScreenActivity.kt           # Main lock screen interface
│   ├── LockScreenService.kt            # Background service
│   ├── DeviceAdminReceiver.kt          # Device admin permissions
│   └── BootReceiver.kt                 # Auto-start on boot
├── res/
│   ├── layout/
│   │   ├── activity_main.xml           # Setup screen layout
│   │   └── activity_lock_screen.xml    # Lock screen layout
│   ├── drawable/
│   │   ├── answer_button_selector.xml  # Button state selector
│   │   └── category_badge.xml          # Category badge design
│   ├── values/
│   │   ├── colors.xml                  # App color scheme
│   │   ├── strings.xml                 # Text resources
│   │   └── themes.xml                  # App themes
│   └── xml/
│       └── device_admin.xml            # Device admin configuration
└── AndroidManifest.xml                 # App permissions and components
```

## Installation & Setup

### Prerequisites
- Android device (API level 24+)
- Android Studio for development
- Device administrator permissions

### Setup Steps
1. **Install the Application**
   ```bash
   ./gradlew installDebug
   ```

2. **Enable Device Administrator**
   - Open the Smart Lock Screen app
   - Tap "Enable Device Admin"
   - Grant device administrator permissions

3. **Start Lock Screen Service**
   - Tap "Start Lock Screen Service"
   - The service will run in the background

4. **Test the Lock Screen**
   - Lock your device (power button)
   - Turn on the screen
   - Answer the educational question to unlock

## How It Works

### Lock Screen Flow
1. **Screen On Event**: Service detects screen turning on
2. **Question Display**: Random question appears with 4 multiple choice options
3. **Answer Selection**: User selects an answer
4. **Feedback**: Visual feedback shows correct/incorrect
5. **Unlock/Retry**: Correct answer unlocks device, incorrect shows new question

### Background Service
- Monitors screen on/off events
- Automatically launches lock screen activity
- Runs as foreground service with notification
- Persists across device reboots

### Device Admin Integration
- Required for lock screen functionality
- Allows app to disable keyguard
- Provides secure unlock mechanism
- Can be disabled through Android settings

## Educational Categories

### Mathematics
- Basic arithmetic (addition, subtraction, multiplication, division)
- Percentages and fractions
- Powers and exponents

### Science
- Chemistry basics (water formula, elements)
- Biology (plants, animals)
- Physics (temperature, states of matter)

### Geography
- World capitals and countries
- Continents and oceans
- Natural landmarks

### History
- Major world events
- Famous historical figures
- Important dates and periods

### Language
- Grammar rules
- Vocabulary and synonyms
- Spelling and word formation

### General Knowledge
- Mixed educational content
- Logic and reasoning
- Common knowledge facts

## Technical Details

### Permissions Required
- `DISABLE_KEYGUARD`: Control lock screen
- `WAKE_LOCK`: Keep screen active
- `SYSTEM_ALERT_WINDOW`: Display over other apps
- `RECEIVE_BOOT_COMPLETED`: Auto-start service
- `REORDER_TASKS`: Manage app stack

### Architecture Components
- **MVVM Pattern**: Clean separation of UI and business logic
- **Repository Pattern**: Centralized data management
- **Service-Oriented**: Background processing
- **Material Design**: Modern, accessible UI

### Security Considerations
- All questions stored locally (no internet required)
- Device admin permissions properly managed
- Service runs with minimal permissions
- No sensitive data collection

## Customization Options

### Adding New Questions
Edit `QuestionRepository.kt` to add new questions:
```kotlin
Question(
    id = 31,
    category = QuestionCategory.MATHEMATICS,
    questionText = "What is 12 × 8?",
    options = listOf("94", "96", "98", "100"),
    correctAnswerIndex = 1
)
```

### Modifying Categories
Update `QuestionCategory` enum in `Question.kt`:
```kotlin
enum class QuestionCategory(val displayName: String) {
    NEW_CATEGORY("New Category Name")
}
```

### UI Customization
- Colors: `res/values/colors.xml`
- Themes: `res/values/themes.xml`
- Layouts: `res/layout/`

## Troubleshooting

### Common Issues
1. **Lock screen not appearing**: Ensure device admin is enabled
2. **Service not starting**: Check if app has required permissions
3. **Questions not loading**: Verify QuestionRepository initialization

### Debug Steps
1. Check device admin status in Android Settings
2. Verify service is running in notification panel
3. Review app permissions in device settings
4. Check Android logs for error messages

## Future Enhancements

### Planned Features
- **Progress Tracking**: Track learning progress and statistics
- **Difficulty Levels**: Adaptive question difficulty
- **Parent Dashboard**: Monitor child's learning activity
- **Custom Categories**: Allow parents to add custom questions
- **Achievement System**: Rewards and badges for learning milestones
- **Offline Sync**: Backup and restore question progress

### Technical Improvements
- **Database Integration**: SQLite for better data management
- **Analytics**: Learning pattern analysis
- **Accessibility**: Enhanced support for users with disabilities
- **Localization**: Multi-language support

## Contributing
This educational app is designed to make learning fun and accessible. Contributions for new questions, categories, or features are welcome!

## License
Educational use - designed to promote learning and development in children.
