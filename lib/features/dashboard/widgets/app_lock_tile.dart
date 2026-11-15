/// List tile for configuring one managed application.
library;

import 'package:flutter/material.dart';

import '../models/managed_app_model.dart';

/// Renders the app identity and independent lock/masking controls.
class AppLockTile extends StatelessWidget {
  /// Creates an app policy tile.
  const AppLockTile({
    required this.app,
    required this.onLockChanged,
    required this.onMaskChanged,
    super.key,
  });

  /// Application represented by this tile.
  final ManagedAppModel app;

  /// Called when the launch lock switch changes.
  final ValueChanged<bool> onLockChanged;

  /// Called when the notification mask control changes.
  final ValueChanged<bool> onMaskChanged;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(child: Icon(app.icon)),
        title: Text(app.name, maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Text(
          app.packageName,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              tooltip: app.isNotifMasked
                  ? 'Disable notification masking'
                  : 'Enable notification masking',
              onPressed: () => onMaskChanged(!app.isNotifMasked),
              icon: Icon(
                app.isNotifMasked
                    ? Icons.notifications_off
                    : Icons.notifications_none,
              ),
              color: app.isNotifMasked
                  ? colorScheme.primary
                  : colorScheme.onSurfaceVariant,
            ),
            Switch(
              value: app.isLocked,
              onChanged: onLockChanged,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
          ],
        ),
      ),
    );
  }
}
