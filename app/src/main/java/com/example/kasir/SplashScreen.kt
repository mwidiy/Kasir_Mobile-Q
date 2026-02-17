package com.example.kasir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(false) }
    
    // Logic-only Splash Screen (Visuals handled by MainActivity Overlay)
    LaunchedEffect(isChecking) {
        if (isChecking) {
            delay(2500) // Wait for Logo Animation in MainActivity (approx 2s) + buffer
            
            if (com.example.kasir.utils.NetworkUtils.isNetworkAvailable(context)) {
                // Online -> Proceed
                com.example.kasir.utils.SessionManager.loadSession(context)
                if (com.example.kasir.utils.SessionManager.isLoggedIn()) {
                    onNavigate("dashboard")
                } else {
                    onNavigate("login")
                }
            } else {
                // Offline -> Show Dialog
                isOffline = true
                isChecking = false
            }
        }
    }

    if (isOffline) {
        com.example.kasir.ui.components.NoInternetDialog(
            onRetry = {
                isOffline = false
                isChecking = true // Trigger Re-check
            },
            onCancel = {
                (context as? android.app.Activity)?.finishAffinity()
            }
        )
    }

    // Empty container (White background is enough, Logo is in Overlay)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    )
}
