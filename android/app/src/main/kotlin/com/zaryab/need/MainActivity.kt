package com.zaryab.need

import android.os.Bundle
import com.zaryab.need.bridge.SettingsMethodHandler
import com.zaryab.need.state.LockSessionManager
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/** Hosts the Flutter dashboard and exposes the Android settings bridge. */
class MainActivity : FlutterFragmentActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            SettingsMethodHandler.CHANNEL_NAME,
        ).setMethodCallHandler(SettingsMethodHandler(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LockSessionManager.initialize(this)
    }
}
