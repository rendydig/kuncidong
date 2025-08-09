# Smart Lock Screen - Testing Guide

## Pre-Testing Setup

### 1. Build the Application
```bash
cd d:\projects\android
./gradlew assembleDebug
```

### 2. Install on Device
```bash
./gradlew installDebug
```

## Testing Steps

### Phase 1: Initial Setup
1. **Launch the App**
   - Open "Smart Lock Screen" from app drawer
   - You should see the main setup screen

2. **Enable Device Administrator**
   - Tap "Enable Device Admin" button
   - Android will prompt for device admin permissions
   - Grant the permission
   - Button should change to "Device Admin Enabled ✓"

3. **Start Lock Screen Service**
   - Tap "Start Lock Screen Service" button
   - You should see a notification "Smart Lock Screen Active"
   - Status should show "Status: Smart Lock Screen active ✓"

### Phase 2: Lock Screen Testing
1. **Test Lock Screen Activation**
   - Press power button to lock device
   - Press power button again to turn on screen
   - Smart Lock Screen should appear with:
     - Current time and date
     - Educational question with 4 multiple choice answers
     - Category badge (MATEMATIKA, Science, etc.)

2. **Test Answer Selection**
   - **Correct Answer**: 
     - Button turns green
     - Shows "Correct! Well done! 🎉"
     - Device unlocks after 1.5 seconds
   - **Incorrect Answer**:
     - Selected button turns red, correct answer turns green
     - Shows "Incorrect. Try again! 🤔"
     - New question loads after 2 seconds

3. **Test Question Variety**
   - Lock and unlock device multiple times
   - Verify different questions appear from various categories:
     - MATEMATIKA (15 + 27 = ?)
     - Science (Chemical symbol for water?)
     - Geography (Capital of France?)
     - History (Year WWII ended?)
     - Language (Plural of child?)
     - General Knowledge (Days in leap year?)

### Phase 3: Service Persistence
1. **Test Service Restart**
   - Go to Settings > Apps > Smart Lock Screen
   - Force stop the app
   - Lock and unlock device
   - Service should automatically restart

2. **Test Boot Persistence**
   - Restart the device
   - After boot, lock and unlock device
   - Smart Lock Screen should still be active

### Phase 4: Edge Cases
1. **Test Back Button**
   - On lock screen, press back button
   - Should NOT close the lock screen

2. **Test Home Button**
   - On lock screen, press home button
   - Should NOT bypass the lock screen

3. **Test Recent Apps**
   - Try accessing recent apps from lock screen
   - Should be blocked until question is answered

## Expected Behaviors

### ✅ Correct Behaviors
- Lock screen appears every time device is locked/unlocked
- Questions are random and varied
- Correct answers unlock the device
- Incorrect answers show new questions
- Service persists across app restarts
- Service auto-starts after device reboot
- UI is responsive and kid-friendly

### ❌ Issues to Watch For
- Lock screen not appearing (check device admin)
- Same question repeating (check QuestionRepository)
- Service not starting (check permissions)
- App crashes (check logs)
- Lock screen can be bypassed (security issue)

## Troubleshooting

### Lock Screen Not Appearing
1. Check device admin is enabled in Android Settings
2. Verify service notification is visible
3. Restart the lock screen service from main app

### Questions Not Loading
1. Check QuestionRepository has questions
2. Verify no crashes in LockScreenActivity
3. Check Android logs for errors

### Service Not Persistent
1. Check boot receiver permissions
2. Verify foreground service notification
3. Check battery optimization settings

## Performance Testing

### Memory Usage
- Monitor app memory usage during extended use
- Should remain stable around 20-30MB

### Battery Impact
- Service should have minimal battery impact
- Foreground notification should be visible but low priority

### Response Time
- Lock screen should appear within 1-2 seconds
- Answer feedback should be immediate
- New questions should load within 2 seconds

## Security Validation

### Device Admin Integration
- Verify app appears in Device Administrators list
- Test that disabling admin stops lock screen
- Confirm proper permission handling

### Lock Screen Security
- Ensure lock screen cannot be bypassed
- Verify back/home buttons are properly handled
- Test that incorrect answers don't unlock device

## Success Criteria

The Smart Lock Screen is working correctly if:
- ✅ Device admin permissions are properly granted
- ✅ Lock screen appears on every screen wake
- ✅ Educational questions display correctly
- ✅ Answer feedback is immediate and accurate
- ✅ Service persists across restarts and reboots
- ✅ UI is responsive and child-friendly
- ✅ Security is maintained (no bypass methods)

## Next Steps After Testing

1. **User Acceptance Testing**: Have children test the educational content
2. **Performance Optimization**: Monitor and optimize battery/memory usage
3. **Content Expansion**: Add more questions and categories
4. **Feature Enhancement**: Implement progress tracking and statistics
5. **Accessibility**: Add support for users with disabilities
