/// Dashboard card showing required Android access and settings shortcuts.
library;

import 'package:flutter/material.dart';

import '../../../core/services/native_bridge_service.dart';

/// Displays live permission health and opens the corresponding settings page.
class PermissionStatusCard extends StatelessWidget {
  /// Creates a permission status card.
  const PermissionStatusCard({
    required this.status,
    required this.onOpenUsageAccess,
    required this.onOpenNotificationAccess,
    required this.onOpenOverlay,
    required this.onOpenAccessibility,
    super.key,
  });

  /// Current permission values, or null while they are loading.
  final PermissionStatus? status;

  /// Opens Usage Access settings.
  final VoidCallback onOpenUsageAccess;

  /// Opens notification listener settings.
  final VoidCallback onOpenNotificationAccess;

  /// Opens overlay settings.
  final VoidCallback onOpenOverlay;

  /// Opens accessibility settings.
  final VoidCallback onOpenAccessibility;

  @override
  Widget build(BuildContext context) {
    final entries = <({String label, bool granted, VoidCallback action})>[
      (
        label: 'Usage access',
        granted: status?.usageAccess ?? false,
        action: onOpenUsageAccess,
      ),
      (
        label: 'Notification access',
        granted: status?.notificationAccess ?? false,
        action: onOpenNotificationAccess,
      ),
      (
        label: 'Display over other apps',
        granted: status?.overlay ?? false,
        action: onOpenOverlay,
      ),
      (
        label: 'Accessibility service',
        granted: status?.accessibility ?? false,
        action: onOpenAccessibility,
      ),
    ];
    final allGranted =
        status != null && entries.every((entry) => entry.granted);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              allGranted ? 'Protection is ready' : 'Complete setup',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              allGranted
                  ? 'Need can protect apps and sanitize notifications.'
                  : 'Grant the missing access below for reliable protection.',
            ),
            const SizedBox(height: 12),
            ...entries.map(
              (entry) => ListTile(
                contentPadding: EdgeInsets.zero,
                leading: Icon(
                  entry.granted ? Icons.check_circle : Icons.warning_rounded,
                  color: entry.granted
                      ? Theme.of(context).colorScheme.primary
                      : Theme.of(context).colorScheme.error,
                ),
                title: Text(entry.label),
                trailing: entry.granted
                    ? const Text('Granted')
                    : TextButton(
                        onPressed: entry.action,
                        child: const Text('Open settings'),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
