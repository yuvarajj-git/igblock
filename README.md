# IgCap: strict 1 hour per day Instagram limit (Android)

IgCap runs as the phone's **Device Owner**. It adds up Instagram's on-screen time since midnight.
At 60 minutes it suspends Instagram (greyed out, will not open) until the next day.
The app has no setting or button to turn the limit off.

## What it blocks
- Uninstalling IgCap, or uninstalling Instagram to reset it
- Force stop / clearing data / disabling apps
- Changing the date and time (auto time is forced on)
- Safe mode, and adding a guest/second user
- It restarts after reboot and after an update

## Build
Open this folder in Android Studio and let it sync (it will fetch Gradle). Then:

1. **Test build** (2 minute limit, removable): Build > Build APK, or `./gradlew assembleDebug`
2. **Real build** (60 minute limit, NOT removable): `./gradlew assembleRelease`
   APK: `app/build/outputs/apk/release/app-release.apk`

## Install (do the test build first!)
1. Back up your phone. The only way out of the real build is a factory reset.
2. Remove all accounts temporarily (Settings > Accounts & sync). Device owner can only be set when none exist.
3. Developer options: enable **USB debugging** and **USB debugging (Security settings)**.
4. Connect the USB cable and run: `./setup.sh path/to/app.apk`
5. Re-add your accounts. Open Instagram for 2 minutes (test build) and confirm it locks.

To remove the **test build**: `adb shell dpm remove-active-admin com.example.igcap/.AdminReceiver` then uninstall.
Then build and install the release APK the same way.

## Known loopholes (be honest with yourself)
- **Instagram in a browser** or in third-party apps is not covered. Block instagram.com in your browser separately.
- **Xiaomi Dual apps / Second space** can create a second copy of Instagram. setup.sh asks you to remove them first. Don't recreate them.
- **Factory reset** removes everything. It is the intentional escape hatch, so a bug cannot brick your phone.
- **Changing the limit** means editing `LIMIT_MINUTES` in `app/build.gradle.kts` and reinstalling. With the release build that needs a factory reset first.
- Usage is read from Android's usage stats. Time with the screen on and Instagram open counts; time in the background does not.

## Files
- `Enforcer.kt` counting, suspending, hardening
- `MonitorService.kt` checks every 5 seconds
- `AdminReceiver.kt`, `BootReceiver.kt` setup and restart after reboot
- `MainActivity.kt` read-only status screen
