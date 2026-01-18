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
    
    // Logic-only Splash Screen (Visuals handled by MainActivity Overlay)
    LaunchedEffect(Unit) {
        delay(2500) // Wait for Logo Animation in MainActivity (approx 2s) + buffer
        
        com.example.kasir.utils.SessionManager.loadSession(context)
        if (com.example.kasir.utils.SessionManager.isLoggedIn()) {
            onNavigate("dashboard")
        } else {
            onNavigate("login")
        }
    }

    // Empty container (White background is enough, Logo is in Overlay)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    )
}
