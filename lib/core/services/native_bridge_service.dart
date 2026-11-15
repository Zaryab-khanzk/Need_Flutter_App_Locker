/// Typed bridge for platform capabilities required by Need.
library;

import 'package:flutter/services.dart';

import '../../features/dashboard/models/managed_app_model.dart';
import '../constants/platform_channels.dart';

/// Represents the Android permissions needed by the lock and masking services.
class PermissionStatus {
  /// Creates a permission status value.
  const PermissionStatus({
    required this.usageAccess,
    required this.notificationAccess,
    required this.overlay,
    required this.accessibility,
  });

  /// Whether Usage Access is granted.
  final bool usageAccess;

  /// Whether notification listener access is granted.
  final bool notificationAccess;

  /// Whether display-over-other-apps access is granted.
  final bool overlay;

  /// Whether the accessibility service is enabled.
  final bool accessibility;

  /// Creates a status from the map returned by Android.
  factory PermissionStatus.fromMap(Map<Object?, Object?> map) {
    bool read(String key) => map[key] == true;
    return PermissionStatus(
      usageAccess: read('usageAccess'),
      notificationAccess: read('notificationAccess'),
      overlay: read('overlay'),
      accessibility: read('accessibility'),
    );
  }
}

/// Provides typed calls into the native Android implementation.
class NativeBridgeService {
  /// Creates a bridge backed by the Need settings channel.
  NativeBridgeService({MethodChannel? channel})
    : _channel = channel ?? const MethodChannel(PlatformChannels.settings);

  final MethodChannel _channel;

  /// Loads launchable applications known to Android.
  Future<List<ManagedAppModel>> getInstalledApps() async {
    final result = await _channel.invokeMethod<List<Object?>>(
      PlatformChannels.getInstalledApps,
    );
    return (result ?? const <Object?>[])
        .whereType<Map<Object?, Object?>>()
        .map(ManagedAppModel.fromMap)
        .toList(growable: false);
  }

  /// Reads the latest permission state from Android.
  Future<PermissionStatus> getPermissionStatus() async {
    final result = await _channel.invokeMethod<Object?>(
      PlatformChannels.getPermissionStatus,
    );
    if (result is! Map<Object?, Object?>) {
      throw PlatformException(
        code: 'invalid_permission_response',
        message: 'Android returned an invalid permission status.',
      );
    }
    return PermissionStatus.fromMap(result);
  }

  /// Opens the Android settings page for Usage Access.
  Future<void> openUsageAccessSettings() =>
      _invokeSettings(PlatformChannels.openUsageAccessSettings);

  /// Opens the Android settings page for notification listener access.
  Future<void> openNotificationAccessSettings() =>
      _invokeSettings(PlatformChannels.openNotificationAccessSettings);

  /// Opens the Android settings page for overlay access.
  Future<void> openOverlaySettings() =>
      _invokeSettings(PlatformChannels.openOverlaySettings);

  /// Opens the Android settings page for accessibility access.
  Future<void> openAccessibilitySettings() =>
      _invokeSettings(PlatformChannels.openAccessibilitySettings);

  Future<void> _invokeSettings(String method) async {
    await _channel.invokeMethod<void>(method);
  }
}
