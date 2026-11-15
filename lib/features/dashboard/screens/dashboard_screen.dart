/// Main Need dashboard screen.
library;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/services/native_bridge_service.dart';
import '../../../core/constants/app_constants.dart';
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
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 20),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
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
                    onRequestNotificationPermission: () =>
                        _requestNotificationPermission(context),
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
                    ),
                  if (!controller.isLoading && controller.filteredApps.isEmpty)
                    const _EmptyApps(),
                ]),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate((context, index) {
                  final app = controller.filteredApps[index];
                  return AppLockTile(
                    app: app,
                    onLockChanged: (value) => controller.setLocked(app, value),
                    onMaskChanged: (value) =>
                        controller.setNotifMasked(app, value),
                  );
                }, childCount: controller.filteredApps.length),
              ),
            ),
            SliverToBoxAdapter(
              child: _DeveloperFooter(onTap: () => _openDeveloperLink(context)),
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

  Future<void> _openDeveloperLink(BuildContext context) async {
    final uri = Uri.parse(kDeveloperPortfolioUrl);
    if (await launchUrl(uri, mode: LaunchMode.externalApplication)) return;
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Unable to open developer website.')),
      );
    }
  }

  Future<void> _requestNotificationPermission(BuildContext context) async {
    try {
      await context.read<NativeBridgeService>().requestNotificationPermission();
      if (context.mounted) {
        await context.read<AppLockerController>().refreshPermissions();
      }
    } on PlatformException catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.message ?? 'Unable to request access.')),
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

class _DeveloperFooter extends StatelessWidget {
  const _DeveloperFooter({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 32, 16, 16),
      child: Center(
        child: TextButton.icon(
          onPressed: onTap,
          icon: const Icon(Icons.open_in_new, size: 16),
          label: const Text('Developed by Zaryab Khan'),
        ),
      ),
    );
  }
}
