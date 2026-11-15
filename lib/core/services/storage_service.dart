/// Persistent storage for the user's app-lock and notification-mask choices.
library;

import 'package:shared_preferences/shared_preferences.dart';

/// Stores package names in typed sets backed by SharedPreferences.
class StorageService {
  /// Creates a storage service with the default preferences backend.
  StorageService({SharedPreferences? preferences}) : _preferences = preferences;

  static const _lockedPackagesKey = 'locked_packages';
  static const _maskedPackagesKey = 'masked_packages';
  SharedPreferences? _preferences;

  Future<SharedPreferences> get _instance async =>
      _preferences ??= await SharedPreferences.getInstance();

  /// Returns all package names currently protected by Need.
  Future<Set<String>> getLockedPackages() async => _readSet(_lockedPackagesKey);

  /// Returns all package names currently masked by Need.
  Future<Set<String>> getMaskedPackages() async => _readSet(_maskedPackagesKey);

  /// Persists whether [packageName] is protected.
  Future<void> setLocked(String packageName, bool value) =>
      _setValue(_lockedPackagesKey, packageName, value);

  /// Persists whether notifications for [packageName] are masked.
  Future<void> setMasked(String packageName, bool value) =>
      _setValue(_maskedPackagesKey, packageName, value);

  Future<Set<String>> _readSet(String key) async =>
      (await _instance).getStringList(key)?.toSet() ?? <String>{};

  Future<void> _setValue(String key, String packageName, bool value) async {
    final preferences = await _instance;
    final values = preferences.getStringList(key)?.toSet() ?? <String>{};
    if (value) {
      values.add(packageName);
    } else {
      values.remove(packageName);
    }
    await preferences.setStringList(key, values.toList()..sort());
  }
}
