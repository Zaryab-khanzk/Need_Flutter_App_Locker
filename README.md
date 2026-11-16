# Need

Need is a personal Android app locker and notification privacy tool built with
Flutter. Choose which installed apps should require device authentication before
they can be opened, and optionally mask notifications from selected apps.

## Features

- Browse and search launchable apps installed on the device.
- Enable or disable launch locking for each app independently.
- Unlock protected apps with Android biometric authentication or the device
  credential.
- Mask notifications from selected apps and replace their content with a
  privacy-friendly notification.
- View the status of every Android access required by the app from one
  dashboard.
- Keep app-lock choices locally on the device using shared preferences.

## How it works

The Flutter dashboard manages app policies and communicates with Android through
typed platform channels. Native Android components then provide the background
behavior:

- An accessibility service detects when a protected app comes to the
  foreground.
- A native lock activity displays the protected-app overlay and starts the
  biometric/device-credential prompt.
- A notification listener can intercept selected app notifications and mask
  their visible content.
- The app-lock and notification-mask lists are persisted locally on the device.

## Required Android access

Android requires the following access for the corresponding features:

| Access                  | Used for                                                   |
| ----------------------- | ---------------------------------------------------------- |
| Usage access            | Reading the foreground-app state reliably                  |
| Accessibility service   | Detecting protected app launches                           |
| Display over other apps | Showing the lock screen above the protected app            |
| Notification access     | Receiving notifications to be masked                       |
| Post notifications      | Posting sanitized replacement notifications on Android 13+ |

On first launch, use the permission status cards in the dashboard to open each
Android settings screen. Return to Need after changing a setting and refresh the
dashboard so the status is updated. If a permission is disabled, the related
feature cannot operate.

## Getting started

### Prerequisites

- Flutter SDK with Dart 3.8 or newer
- Android Studio or an Android SDK installation
- An Android device or emulator (the lock and notification features are
  Android-specific)

### Run locally

```bash
flutter pub get
flutter run
```

For a release APK:

```bash
flutter build apk --release
```

## Project structure

```text
lib/
  core/                 Shared constants, storage, and platform services
  features/dashboard/   Dashboard UI, app policies, and state management
android/
  ...                   Native lock activity and Android services
assets/icon/            Need branding assets
```

The Flutter layer uses `provider` for dashboard state and `shared_preferences`
for local policy storage. Android-specific behavior lives under
`android/app/src/main/kotlin/com/zaryab/need`.

## Privacy

Need is designed for local, personal use. App policies are stored locally on the
device. Android access permissions are powerful system capabilities; enable only
the permissions needed for the features you use and review them in Android
Settings at any time.

## Development

Run static analysis and tests before submitting changes:

```bash
flutter analyze
flutter test
```

Need is not currently published to pub.dev.
===========================================

>>>>>>> 9d21e8463b6d0fcd33b424bf510a56bf347a1817
>>>>>>>
>>>>>>
>>>>>
>>>>
>>>
>>
