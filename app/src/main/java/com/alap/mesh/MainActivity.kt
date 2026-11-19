package com.alap.mesh

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alap.mesh.ui.AlapViewModel
import com.alap.mesh.ui.AppRouteStage
import com.alap.mesh.ui.components.AlapToastBanner
import com.alap.mesh.ui.components.PermissionBottomSheet
import com.alap.mesh.ui.onboarding.OnboardingScreen
import com.alap.mesh.ui.screens.MainAppShell
import com.alap.mesh.ui.screens.PinLockScreen
import com.alap.mesh.ui.splash.SplashScreen
import com.alap.mesh.ui.theme.AlapTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AlapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val routeStage by viewModel.routeStage.collectAsStateWithLifecycle()
            val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
            val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
            val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
            var showPermissionSheet by remember { mutableStateOf(false) }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshPermissions()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val view = LocalView.current
            val useDarkStatusBarIcons = routeStage != AppRouteStage.SPLASH &&
                (routeStage == AppRouteStage.ONBOARDING || !settings.darkThemeEnabled)
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkStatusBarIcons
                }
            }

            AlapTheme(darkTheme = settings.darkThemeEnabled && routeStage == AppRouteStage.MAIN_APP) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLocked && routeStage == AppRouteStage.MAIN_APP) {
                        PinLockScreen(onVerifyPin = viewModel::unlockWithPin)
                    } else {
                        AnimatedContent(
                            targetState = routeStage,
                            transitionSpec = {
                                fadeIn(tween(350)).togetherWith(fadeOut(tween(350)))
                            },
                            label = "appRouteStage"
                        ) { stage ->
                            when (stage) {
                                AppRouteStage.SPLASH -> SplashScreen(
                                    animationsEnabled = settings.animationsEnabled,
                                    onFinish = viewModel::onSplashFinished
                                )
                                AppRouteStage.ONBOARDING -> OnboardingScreen(
                                    onOpenPermissionsSheet = { showPermissionSheet = true }
                                )
                                AppRouteStage.MAIN_APP -> MainAppShell(
                                    viewModel = viewModel,
                                    settings = settings,
                                    permissionStatus = permissionStatus
                                )
                            }
                        }
                    }

                    if (showPermissionSheet) {
                        PermissionBottomSheet(
                            onDismiss = { showPermissionSheet = false },
                            onCompleted = { granted ->
                                showPermissionSheet = false
                                viewModel.completeOnboardingAndEnterApp(granted)
                            }
                        )
                    }

                    AlapToastBanner(
                        message = toastMessage,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
