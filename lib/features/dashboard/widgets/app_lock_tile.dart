/// List tile for configuring one managed application.
library;

import 'package:flutter/material.dart';

import '../models/managed_app_model.dart';

/// Renders the app identity and independent lock/masking switches.
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

  /// Called when the notification mask switch changes.
  final ValueChanged<bool> onMaskChanged;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(child: Icon(app.icon)),
        title: Text(app.name),
        subtitle: Text(app.packageName),
        trailing: SizedBox(
          width: 144,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _PolicySwitch(
                icon: Icons.lock_outline,
                label: 'Lock',
                value: app.isLocked,
                onChanged: onLockChanged,
              ),
              _PolicySwitch(
                icon: Icons.notifications_off_outlined,
                label: 'Mask',
                value: app.isNotifMasked,
                onChanged: onMaskChanged,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PolicySwitch extends StatelessWidget {
  const _PolicySwitch({
    required this.icon,
    required this.label,
    required this.value,
    required this.onChanged,
  });

  final IconData icon;
  final String label;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Icon(icon, size: 16),
        const SizedBox(width: 4),
        Text(label, style: Theme.of(context).textTheme.labelSmall),
        Switch(
          value: value,
          onChanged: onChanged,
          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
        ),
      ],
    );
  }
}
