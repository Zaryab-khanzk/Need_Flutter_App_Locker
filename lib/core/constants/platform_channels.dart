/// Names and method keys shared by Dart and the Android platform layer.
abstract final class PlatformChannels {
  /// Method channel used by the Flutter dashboard and Android services.
  static const settings = 'com.zaryab.need/settings';

  /// Returns installed launchable applications as serializable maps.
  static const getInstalledApps = 'getInstalledApps';

  /// Returns the current status of required Android permissions.
  static const getPermissionStatus = 'getPermissionStatus';

  /// Opens Android Usage Access settings.
  static const openUsageAccessSettings = 'openUsageAccessSettings';

  /// Opens Android notification listener settings.
  static const openNotificationAccessSettings =
      'openNotificationAccessSettings';

  /// Opens Android overlay permission settings.
  static const openOverlaySettings = 'openOverlaySettings';

  /// Opens Android accessibility settings.
  static const openAccessibilitySettings = 'openAccessibilitySettings';

  /// Requests the Android runtime notification permission.
  static const requestNotificationPermission = 'requestNotificationPermission';
}
