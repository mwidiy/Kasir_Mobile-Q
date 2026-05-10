package id.quacxel.mejapesan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
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
import id.quacxel.mejapesan.ui.theme.MejaPesanTheme
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
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessaging
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import id.quacxel.mejapesan.utils.SocketHandler
import id.quacxel.mejapesan.utils.LocalEventBus
import org.json.JSONObject

@androidx.compose.animation.ExperimentalAnimationApi
class MainActivity : ComponentActivity() {

    private val REQUEST_PERMISSION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- KRITIS: Load session SEBELUM semua logic yang butuh auth ---
        // Ini memastikan jwtToken tersedia untuk FCM check & auth interceptor
        kotlinx.coroutines.runBlocking {
            id.quacxel.mejapesan.utils.SessionManager.loadSession(this@MainActivity)
        }

        // --- LIFECYCLE OBSERVER ---
        ProcessLifecycleOwner.get().lifecycle.addObserver(id.quacxel.mejapesan.utils.AppLifecycleObserver)
        
        // --- REQUEST POST_NOTIFICATIONS PERMISSION ---
        requestNotificationPermission()

        // --- CREATE NOTIFICATION CHANNEL WITH CUSTOM SOUND ---
        createOrderNotificationChannel()

        // --- FETCH FCM TOKEN ---
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            Log.d("MainActivity", "FCM Registration Token: $token")
            
            // Send token to backend if logged in
            if (id.quacxel.mejapesan.utils.SessionManager.isLoggedIn()) {
                val userId = id.quacxel.mejapesan.utils.SessionManager.currentUser?.id?.toString()
                if (userId != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val request = id.quacxel.mejapesan.data.network.FcmTokenRequest(userId, token)
                            val response = id.quacxel.mejapesan.data.network.RetrofitClient.instance.updateFcmToken(request)
                            if (response.isSuccessful) {
                                Log.d("MainActivity", "FCM Token successfully sent to backend.")
                            } else {
                                Log.e("MainActivity", "Failed to send FCM token. Code: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Exception sending FCM token", e)
                        }
                    }
                } else {
                    Log.d("MainActivity", "FCM Token not sent: User ID is null.")
                }
            } else {
                Log.d("MainActivity", "FCM Token not sent: User is not logged in.")
            }
        }

        // --- GLOBAL SOCKET LISTENERS (Always On) ---
        setupGlobalSocketListeners()

        setContent {
            // --- ALWAYS ON LOGIC ---
            val context = androidx.compose.ui.platform.LocalContext.current
            val alwaysOn by id.quacxel.mejapesan.utils.SessionManager.getAlwaysOn(context).collectAsState(initial = false)
            
            LaunchedEffect(alwaysOn) {
                if (alwaysOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            MejaPesanTheme {
                    // --- STATE ---
                    var currentScreen by remember { mutableStateOf("splash") }

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
                                    "login" -> LoginScreen(onLoginSuccess = { currentScreen = "main" }) // REMOVED SERVICE TRIGGER
                                    "main" -> MainScreen(onLogout = { currentScreen = "login" }) // REMOVED SERVICE TRIGGER
                                    else -> MainScreen(onLogout = { currentScreen = "login" }) 
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
                             id.quacxel.mejapesan.ui.components.AnimatedLogo(
                                modifier = Modifier.size(logoSize)
                             )
                        }
                    }
                }
            }
        }
    
    // --- NOTIFICATION CHANNEL WITH CUSTOM SOUND ---
    private fun createOrderNotificationChannel() {
        lifecycleScope.launch {
            val customPath = id.quacxel.mejapesan.utils.SessionManager.getCustomSoundPath(this@MainActivity).first()
            id.quacxel.mejapesan.utils.NotificationUtils.createOrderChannel(this@MainActivity, customPath)
            Log.d("MainActivity", "NotificationChannel initialized via NotificationUtils")
        }
    }

    // --- NATIVE PERMISSION HANDLING ---

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_PERMISSION_CODE
                )
            }
        }
    }

    private fun setupGlobalSocketListeners() {
        try {
            SocketHandler.setSocket()
            val socket = SocketHandler.getSocket()
            
            // Re-establish if disconnected
            if (!socket.connected()) {
                SocketHandler.establishConnection()
            }

            // --- WhatsApp Bot Global Listeners ---
            socket.on("wa_qr_code") { args ->
                try {
                    val data = args[0] as JSONObject
                    val qr = data.getString("qr")
                    Log.d("MainActivity", "Global Socket: wa_qr_code received")
                    LocalEventBus.emitWaQr(qr)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing global wa_qr_code: ${e.message}")
                }
            }

            socket.on("wa_pairing_code") { args ->
                try {
                    val data = args[0] as JSONObject
                    val code = data.getString("code")
                    Log.d("MainActivity", "Global Socket: wa_pairing_code received -> $code")
                    LocalEventBus.emitWaPairingCode(code)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing global wa_pairing_code: ${e.message}")
                }
            }

            socket.on("wa_status") { args ->
                try {
                    val data = args[0] as JSONObject
                    val status = data.getString("status")
                    Log.d("MainActivity", "Global Socket: wa_status received -> $status")
                    LocalEventBus.emitWaStatus(status)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing global wa_status: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to setup global socket listeners: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification Permission GRANTED")
            } else {
                Log.e("MainActivity", "Notification Permission DENIED")
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
    MejaPesanTheme {
        Greeting("Android")
    }
}
