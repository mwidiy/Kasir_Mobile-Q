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

@androidx.compose.animation.ExperimentalAnimationApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableEdgeToEdge()
        setContent {
            // --- ALWAYS ON LOGIC ---
            val context = androidx.compose.ui.platform.LocalContext.current
            // Observe the "Always On" preference
            // We use LaunchedEffect because we need a coroutine scope, but actually
            // setting the window flag requires the Activity context.
            // A simpler way in Compose for Side Effects:
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

                    // --- ANIMATION STATES FOR PERSISTENT LOGO ---
                    // Size: 160dp (Splash) -> 120dp (Login)
                    val logoSize by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (currentScreen == "splash") 160.dp else 120.dp,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f),
                        label = "logoSize"
                    )

                    // Position (Vertical Bias): 0f (Center) -> -0.7f (Top)
                    // -0.7f roughly puts it at the top 15% of the screen
                    val logoBias by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (currentScreen == "splash") 0f else -0.7f,
                        animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "logoBias"
                    )

                    // Fade Out Logo when moving to Login or any other screen
                    // User wants animation ONLY for splash, then it should disappear or stay if we move to Login who knows?
                    // Based on source, it seems to stay for Login?
                    // Source Logic: "visible = showLogo" where "val showLogo = currentScreen == "splash""
                    // WAIT: If showLogo is ONLY for splash, then it disappears on Login. But Main Activity overlay implies it stays or transitions?
                    // Source MainActivity says:
                    // val showLogo = currentScreen == "splash"
                    // And transitions: Splash -> Login (Slide Up + Fade In Content)
                    
                    // Actually, let's look closer at source MainActivity. 
                    // It says: "visible = showLogo" where showLogo = currentScreen == "splash".
                    // So the overlay logo DISAPPEARS when state changes to Login. 
                    // BUT LoginScreen has its OWN static logo.
                    // The "Animation" effect is likely: 
                    // 1. Splash: Logo Center -> Logo Top (Animation?)
                    // 2. Logic: Wait 2.5s.
                    // 3. Navigate -> Login.
                    // Re-reading Source MainActivity: 
                    // val logoBias target = if (splash) 0f else -0.7f.
                    // val showLogo = currentScreen == "splash".
                    // So when switching to 'login', 'showLogo' becomes false, so it fades out.
                    // BUT 'logoBias' animates to -0.7f (top).
                    // This implies the INTENTION is for the logo to move up to matching position of Login Screen's static logo, then crossfades?
                    // OR: showLogo should actually be true for Login too? 
                    // Checking source login screen: It has a static Image logo. 
                    // If visual consistency is key, let's copy source EXACTLY.
                    
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
                                    "login" -> LoginScreen(onLoginSuccess = { currentScreen = "main" })
                                    "main" -> MainScreen(onLogout = { currentScreen = "login" })
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
                             com.example.kasir.ui.components.AnimatedLogo(
                                modifier = Modifier.size(logoSize)
                             )
                        }
                    }
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