/// State controller for installed applications and Need policies.
library;

import 'package:flutter/foundation.dart';

import '../../../core/services/native_bridge_service.dart';
import '../../../core/services/storage_service.dart';
import '../models/managed_app_model.dart';

/// Coordinates dashboard state, filtering, persistence, and native refreshes.
class AppLockerController extends ChangeNotifier {
  /// Creates a controller using the platform bridge and preferences store.
  AppLockerController({
    required StorageService storage,
    required NativeBridgeService nativeBridge,
  }) : _storage = storage,
       _nativeBridge = nativeBridge;

  final StorageService _storage;
  final NativeBridgeService _nativeBridge;
  final List<ManagedAppModel> _apps = <ManagedAppModel>[];
  String _query = '';
  PermissionStatus? _permissionStatus;
  bool _isLoading = false;
  String? _errorMessage;

  /// All installed applications loaded from Android.
  List<ManagedAppModel> get apps => List.unmodifiable(_apps);

  /// Applications matching the current search query.
  List<ManagedAppModel> get filteredApps {
    final query = _query.trim().toLowerCase();
    if (query.isEmpty) return apps;
    return _apps
        .where(
          (app) =>
              app.name.toLowerCase().contains(query) ||
              app.packageName.toLowerCase().contains(query),
        )
        .toList(growable: false);
  }

  /// Current search query.
  String get query => _query;

  /// Latest Android permission state, if loaded.
  PermissionStatus? get permissionStatus => _permissionStatus;

  /// Whether initial data is being loaded.
  bool get isLoading => _isLoading;

  /// An actionable loading error, if one occurred.
  String? get errorMessage => _errorMessage;

  /// Loads applications, saved policies, and permission status.
  Future<void> initialize() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    try {
      final results = await Future.wait<Object>([
        _nativeBridge.getInstalledApps(),
        _storage.getLockedPackages(),
        _storage.getMaskedPackages(),
        _nativeBridge.getPermissionStatus(),
      ]);
      final locked = results[1] as Set<String>;
      final masked = results[2] as Set<String>;
      _apps
        ..clear()
        ..addAll(
          (results[0] as List<ManagedAppModel>).map(
            (app) => app.copyWith(
              isLocked: locked.contains(app.packageName),
              isNotifMasked: masked.contains(app.packageName),
            ),
          ),
        );
      _permissionStatus = results[3] as PermissionStatus;
    } catch (error) {
      _errorMessage = 'Unable to load Android app data: $error';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Refreshes permission indicators after returning from Android settings.
  Future<void> refreshPermissions() async {
    try {
      _permissionStatus = await _nativeBridge.getPermissionStatus();
      _errorMessage = null;
      notifyListeners();
    } catch (error) {
      _errorMessage = 'Unable to refresh permission status: $error';
      notifyListeners();
    }
  }

  /// Updates the search filter.
  void setQuery(String value) {
    if (_query == value) return;
    _query = value;
    notifyListeners();
  }

  /// Changes the launch-lock policy for [app].
  Future<void> setLocked(ManagedAppModel app, bool value) async {
    await _storage.setLocked(app.packageName, value);
    _replace(app.copyWith(isLocked: value));
  }

  /// Changes the notification-mask policy for [app].
  Future<void> setNotifMasked(ManagedAppModel app, bool value) async {
    await _storage.setMasked(app.packageName, value);
    _replace(app.copyWith(isNotifMasked: value));
  }

  void _replace(ManagedAppModel replacement) {
    final index = _apps.indexWhere(
      (app) => app.packageName == replacement.packageName,
    );
    if (index == -1) return;
    _apps[index] = replacement;
    notifyListeners();
  }
}
