package com.example.kasir

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.kasir.ui.components.CustomBottomNavigation

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    initialScreen: String = "dashboard",
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }

    // Logic to decide if we show bottom bar. 
    // We only show it for the main tabs.
    val mainTabs = listOf("dashboard", "riwayat", "menu", "meja")
    val showBottomBar = currentScreen in mainTabs

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                CustomBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        }
    ) { innerPadding ->
        // We apply bottom padding manually or via innerPadding to ensure content isn't covered
        
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn().with(fadeOut())
                },
                label = "MainScreenTransition"
            ) { screen ->
                when (screen) {
                    "dashboard" -> DashboardScreen(onNavigate = { target -> 
                        if (target == "login") onLogout() else currentScreen = target
                    })
                    "riwayat" -> RiwayatScreen(onNavigate = { target -> currentScreen = target })
                    "menu" -> MenuScreen(onNavigate = { target -> currentScreen = target })
                    "meja" -> TableScreen(onNavigate = { target -> currentScreen = target })
                    "bayar" -> ScanScreen(onNavigate = { target -> currentScreen = target })
                    
                    "profile" -> ProfileScreen(onNavigate = { target -> 
                         if (target == "login") onLogout() else currentScreen = target
                    })
                    else -> DashboardScreen(onNavigate = { currentScreen = it })
                }
            }
        }
    }
}
