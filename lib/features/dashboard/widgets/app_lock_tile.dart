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
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        minVerticalPadding: 8,
        minTileHeight: 72,
        leading: app.iconBytes == null
            ? CircleAvatar(child: Icon(app.icon))
            : ClipRRect(
                borderRadius: BorderRadius.circular(10),
                child: Image.memory(
                  app.iconBytes!,
                  width: 42,
                  height: 42,
                  fit: BoxFit.cover,
                ),
              ),
        title: Text(app.name, maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Text(
          app.packageName,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton.filledTonal(
              constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
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
