package com.zaryab.need.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.zaryab.need.R
import com.zaryab.need.services.AppLockNotificationListener
import com.zaryab.need.state.LockSessionManager
import java.util.function.Consumer

/** Native frosted lock overlay with no Flutter engine startup cost. */
class LockActivity : AppCompatActivity() {
    private var targetPackage: String? = null
    private var promptShown = false
    private var blurConfigured = false
    private var blurListener: Consumer<Boolean>? = null
    private var scrim: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        disableActivityTransition(open = true)
        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (targetPackage.isNullOrBlank()) {
            goHome()
            return
        }
        LockSessionManager.initialize(this)
        LockSessionManager.setLockActivityVisible(true)
        setContentView(createOverlay())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !blurConfigured) {
            configureBlurSafely()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!promptShown) {
            promptShown = true
            authenticate()
        }
    }

    override fun onBackPressed() {
        goHome()
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurListener?.let {
                getSystemService(WindowManager::class.java)
                    .removeCrossWindowBlurEnabledListener(it)
            }
        }
        LockSessionManager.setLockActivityVisible(false)
        super.onDestroy()
    }

    private fun configureBlurSafely() {
        try {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            )
            window.attributes = window.attributes.apply {
                dimAmount = 0.45f
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 150
                }
                window.setBackgroundBlurRadius(150)
                blurListener = Consumer { enabled ->
                    scrim?.setBackgroundColor(
                        if (enabled) Color.parseColor("#660B0F17")
                        else Color.parseColor("#F20B0F17"),
                    )
                }
                getSystemService(WindowManager::class.java)
                    .addCrossWindowBlurEnabledListener(mainExecutor, blurListener!!)
            }
            blurConfigured = true
        } catch (throwable: Throwable) {
            Log.e("NeedLock", "Blur configuration unavailable; using dark scrim", throwable)
            scrim?.setBackgroundColor(Color.parseColor("#F20B0F17"))
        }
    }

    private fun createOverlay(): FrameLayout {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        scrim = FrameLayout(this).apply {
            // Start opaque enough to hide content if the system disables blur.
            setBackgroundColor(Color.parseColor("#F20B0F17"))
        }
        root.addView(
            scrim,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 48, 48, 48)
        }
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            contentDescription = getString(R.string.app_name)
        }
        content.addView(icon, LinearLayout.LayoutParams(112, 112))
        content.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        content.addView(TextView(this).apply {
            text = "Locked — Verify to continue"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        })
        content.addView(Button(this).apply {
            text = "Unlock"
            setOnClickListener {
                promptShown = false
                authenticate()
            }
        }, LinearLayout.LayoutParams(180, 56))
        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )
        return root
    }

    private fun authenticate() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            goHome()
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    targetPackage?.let { packageName ->
                        LockSessionManager.markUnlocked(packageName)
                        sendBroadcast(
                            Intent(AppLockNotificationListener.ACTION_APP_UNLOCKED).apply {
                                setPackage(this@LockActivity.packageName)
                                putExtra(AppLockNotificationListener.EXTRA_PACKAGE, packageName)
                            },
                        )
                    }
                    disableActivityTransition(open = false)
                    finish()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    Log.d("NeedLock", "Authentication failed: $errorCode $errString")
                    goHome()
                }
            },
        )
        try {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.lock_prompt_title))
                    .setSubtitle(getString(R.string.lock_prompt_subtitle))
                    .setAllowedAuthenticators(authenticators)
                    .build(),
            )
        } catch (throwable: Throwable) {
            Log.e("NeedLock", "Unable to show biometric prompt", throwable)
            goHome()
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        disableActivityTransition(open = false)
        finish()
    }

    private fun disableActivityTransition(open: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (open) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            }
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }
}
