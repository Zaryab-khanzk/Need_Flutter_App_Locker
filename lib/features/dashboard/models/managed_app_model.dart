/// Immutable view model for an installed application managed by Need.
library;

import 'package:flutter/material.dart';
import 'dart:typed_data';

/// Describes one launchable application and its Need policies.
class ManagedAppModel {
  /// Creates an application model.
  const ManagedAppModel({
    required this.name,
    required this.packageName,
    required this.icon,
    this.iconBytes,
    this.isLocked = false,
    this.isNotifMasked = false,
  });

  /// Human-readable application label.
  final String name;

  /// Android package identifier.
  final String packageName;

  /// Material fallback icon used when Android does not provide an asset.
  final IconData icon;

  /// PNG bytes for the Android launcher icon, when available.
  final Uint8List? iconBytes;

  /// Whether launching this application requires authentication.
  final bool isLocked;

  /// Whether notifications from this application are sanitized.
  final bool isNotifMasked;

  /// Creates a model from the Android channel representation.
  factory ManagedAppModel.fromMap(Map<Object?, Object?> map) {
    return ManagedAppModel(
      name: map['name'] as String? ?? map['packageName'] as String? ?? 'App',
      packageName: map['packageName'] as String? ?? '',
      icon: Icons.apps_rounded,
      iconBytes: map['iconBytes'] is Uint8List
          ? map['iconBytes'] as Uint8List
          : null,
    );
  }

  /// Returns a copy with selected policy values changed.
  ManagedAppModel copyWith({bool? isLocked, bool? isNotifMasked}) {
    return ManagedAppModel(
      name: name,
      packageName: packageName,
      icon: icon,
      iconBytes: iconBytes,
      isLocked: isLocked ?? this.isLocked,
      isNotifMasked: isNotifMasked ?? this.isNotifMasked,
    );
  }
}
