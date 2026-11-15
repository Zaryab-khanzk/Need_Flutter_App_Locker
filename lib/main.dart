/// Application entry point for Need.
library;

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'core/services/native_bridge_service.dart';
import 'core/services/storage_service.dart';
import 'features/dashboard/providers/app_locker_controller.dart';
import 'features/dashboard/screens/dashboard_screen.dart';

/// Starts the Need application.
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const NeedApp());
}

/// Root widget that configures the dark Material 3 experience.
class NeedApp extends StatelessWidget {
  /// Creates the root application widget.
  const NeedApp({super.key});

  @override
  Widget build(BuildContext context) {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: const Color(0xFF8AB4F8),
      brightness: Brightness.dark,
    );
    return MultiProvider(
      providers: [
        Provider<StorageService>(create: (_) => StorageService()),
        Provider<NativeBridgeService>(create: (_) => NativeBridgeService()),
        ChangeNotifierProxyProvider2<
          StorageService,
          NativeBridgeService,
          AppLockerController
        >(
          create: (context) => AppLockerController(
            storage: context.read<StorageService>(),
            nativeBridge: context.read<NativeBridgeService>(),
          )..initialize(),
          update: (_, storage, nativeBridge, controller) =>
              controller ??
                    AppLockerController(
                      storage: storage,
                      nativeBridge: nativeBridge,
                    )
                ..initialize(),
        ),
      ],
      child: MaterialApp(
        title: 'Need',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: colorScheme,
          brightness: Brightness.dark,
          scaffoldBackgroundColor: colorScheme.surface,
          inputDecorationTheme: const InputDecorationTheme(
            border: OutlineInputBorder(),
          ),
        ),
        home: const DashboardScreen(),
      ),
    );
  }
}
