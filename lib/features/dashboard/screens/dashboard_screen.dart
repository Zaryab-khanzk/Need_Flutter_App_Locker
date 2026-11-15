/// Main Need dashboard screen.
library;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../../core/services/native_bridge_service.dart';
import '../providers/app_locker_controller.dart';
import '../widgets/app_lock_tile.dart';
import '../widgets/permission_status_card.dart';

/// Shows permission health, search, and app policy controls.
class DashboardScreen extends StatelessWidget {
  /// Creates the dashboard screen.
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = context.watch<AppLockerController>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Need'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: controller.isLoading ? null : controller.initialize,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: controller.initialize,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
          children: [
            Text(
              'App privacy, under your control.',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            PermissionStatusCard(
              status: controller.permissionStatus,
              onOpenUsageAccess: () => _openSettings(
                context,
                (bridge) => bridge.openUsageAccessSettings(),
              ),
              onOpenNotificationAccess: () => _openSettings(
                context,
                (bridge) => bridge.openNotificationAccessSettings(),
              ),
              onOpenOverlay: () => _openSettings(
                context,
                (bridge) => bridge.openOverlaySettings(),
              ),
              onOpenAccessibility: () => _openSettings(
                context,
                (bridge) => bridge.openAccessibilitySettings(),
              ),
            ),
            const SizedBox(height: 20),
            TextField(
              onChanged: controller.setQuery,
              decoration: const InputDecoration(
                hintText: 'Search installed apps',
                prefixIcon: Icon(Icons.search),
              ),
            ),
            const SizedBox(height: 16),
            if (controller.errorMessage != null)
              _ErrorBanner(message: controller.errorMessage!),
            if (controller.isLoading && controller.apps.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32),
                  child: CircularProgressIndicator(),
                ),
              )
            else if (controller.filteredApps.isEmpty)
              const _EmptyApps(),
            ...controller.filteredApps.map(
              (app) => AppLockTile(
                app: app,
                onLockChanged: (value) => controller.setLocked(app, value),
                onMaskChanged: (value) => controller.setNotifMasked(app, value),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _openSettings(
    BuildContext context,
    Future<void> Function(NativeBridgeService bridge) action,
  ) async {
    try {
      await action(context.read<NativeBridgeService>());
      if (context.mounted) {
        await context.read<AppLockerController>().refreshPermissions();
      }
    } on PlatformException catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.message ?? 'Unable to open settings.')),
        );
      }
    }
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: Theme.of(context).colorScheme.errorContainer,
      child: ListTile(
        leading: const Icon(Icons.error_outline),
        title: Text(message),
      ),
    );
  }
}

class _EmptyApps extends StatelessWidget {
  const _EmptyApps();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 40),
      child: Center(child: Text('No launchable apps found.')),
    );
  }
}
