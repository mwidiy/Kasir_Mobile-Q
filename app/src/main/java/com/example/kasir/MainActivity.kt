package com.example.kasir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.kasir.ui.theme.KasirTheme
import androidx.compose.animation.with
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import android.Manifest
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.util.Log
import com.example.kasir.service.OrderNotificationService
import androidx.lifecycle.ProcessLifecycleOwner

@androidx.compose.animation.ExperimentalAnimationApi
class MainActivity : ComponentActivity() {

    private val REQUEST_PERMISSION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- LIFECYCLE OBSERVER ---
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(com.example.kasir.utils.AppLifecycleObserver)
        
        // --- NATIVE SERVICE START ---
        checkAndStartService()

        setContent {
            // --- ALWAYS ON LOGIC ---
            val context = androidx.compose.ui.platform.LocalContext.current
            val alwaysOn by com.example.kasir.utils.SessionManager.getAlwaysOn(context).collectAsState(initial = false)
            
            LaunchedEffect(alwaysOn) {
                if (alwaysOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            KasirTheme {
                    // --- STATE ---
                    var currentScreen by remember { mutableStateOf("splash") }
                    val context = androidx.compose.ui.platform.LocalContext.current

                    // Note: Service logic moved to Native onCreate. 
                    // Compose side-effects for service removed.

                    // --- ANIMATION STATES FOR PERSISTENT LOGO ---
                    val logoSize by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (currentScreen == "splash") 160.dp else 120.dp,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f),
                        label = "logoSize"
                    )

                    val logoBias by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (currentScreen == "splash") 0f else -0.7f,
                        animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "logoBias"
                    )
                    
                    val showLogo = currentScreen == "splash"

                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        
                        // LAYER 1: CONTENT
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            // Smooth Transitions
                            androidx.compose.animation.AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    if (targetState == "login" && initialState == "splash") {
                                        // Splash -> Login: Slide Up + Fade In (CONTENT ONLY)
                                        (androidx.compose.animation.slideInVertically { height -> height / 2 } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(800, delayMillis = 300)))
                                            .with(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(800)))
                                    } else if (targetState == "dashboard") {
                                        androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(800))
                                            .with(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(800)))
                                    } else {
                                        androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(500))
                                            .with(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(500)))
                                    }
                                },
                                label = "ScreenTransition"
                            ) { targetScreen ->
                                when (targetScreen) {
                                    "splash" -> SplashScreen(onNavigate = { screen -> currentScreen = screen })
                                    "login" -> LoginScreen(onLoginSuccess = { 
                                        checkAndStartService()
                                        currentScreen = "main" 
                                    })
                                    "main" -> MainScreen(onLogout = { 
                                        stopOrderService()
                                        currentScreen = "login" 
                                    })
                                    else -> MainScreen(onLogout = { 
                                        stopOrderService()
                                        currentScreen = "login" 
                                    }) 
                                }
                            }
                        }

                        // LAYER 2: PERSISTENT LOGO (Only visible on Splash)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showLogo,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut(),
                            modifier = Modifier.align(BiasAlignment(0f, logoBias))
                        ) {
                             com.example.kasir.ui.components.AnimatedLogo(
                                modifier = Modifier.size(logoSize)
                             )
                        }
                    }
                }
            }
        }
    
    // --- NATIVE PERMISSION & SERVICE HANDLING ---

    private fun checkAndStartService() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startOrderService()
            } else {
                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_PERMISSION_CODE
                )
            }
        } else {
            startOrderService()
        }
    }

    private fun startOrderService() {
        try {
            val intent = Intent(this, OrderNotificationService::class.java)
            Log.d("MainActivity", "Starting OrderNotificationService")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start service", e)
        }
    }

    private fun stopOrderService() {
        try {
            val intent = Intent(this, OrderNotificationService::class.java)
            stopService(intent)
            Log.d("MainActivity", "Stopped OrderNotificationService")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to stop service", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permission GRANTED")
                startOrderService()
            } else {
                Log.e("MainActivity", "Permission DENIED")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KasirTheme {
        Greeting("Android")
    }
}