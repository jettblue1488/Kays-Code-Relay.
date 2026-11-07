# Kay's Code Relay + Signal Shield

Native Android Java project. The GitHub Actions workflow builds an APK on every push to main and publishes it as a downloadable GitHub Release.

## Download
After a successful build, install from: https://github.com/jettblue1488/Kays-Code-Relay./releases/latest/download/Kays-Code-Relay.apk

If the Release is unavailable, open Actions, select the green workflow run, and download its APK artifact, then unzip to get app-debug.apk.

## Use
Install **on the Android phone that actually receives SMS for your selected number**. Launch the app and grant SMS, Phone and notification permissions. Enter your receiving number as a label (it does not connect to the number), enter a destination, select the active SIM/eSIM, and save settings. Enable forwarding only after reading the confirmation. Incoming texts are kept in a local private app preference and optionally forwarded over that SIM as ordinary SMS. Your carrier may charge for forwarded messages. Turn forwarding off whenever you no longer need it.

Do not share forwarded one-time codes with anyone you do not fully trust. A disconnected or reassigned number cannot be accessed by this app. RCS, MMS, email, calls, historical texts, and carrier interception are not supported. Android may restrict some background SMS activities.

The **SCAN PHONE SECURITY** button checks visible Android configuration and helps review settings. It cannot detect wiretaps, determine government surveillance, or establish whether someone is tracking you.

**Privacy:** There is no INTERNET permission; no server or remote sync. The project deliberately leaves telephone numbers out of the public repository. The downloaded debug APK is for personal testing, not Google Play publication. Google Play restricts SMS-related permissions; this is not a Play Store-ready SMS handler. Protect your device lock screen.
