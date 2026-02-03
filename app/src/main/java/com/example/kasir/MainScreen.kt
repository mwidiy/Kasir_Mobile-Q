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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasir.ui.components.CustomBottomNavigation
import com.example.kasir.viewmodel.DashboardViewModel
import com.example.kasir.utils.playNotificationSound
import android.view.WindowManager

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    initialScreen: String = "dashboard",
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel() // Global ViewModel instance
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }

    // --- GLOBAL LOGIC: Always On Display ---
    val isAlwaysOn by viewModel.isAlwaysOn.collectAsState()
    val context = LocalContext.current
    val window = (context as? android.app.Activity)?.window
    
    DisposableEffect(isAlwaysOn) {
        if (window != null) {
            if (isAlwaysOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            // Optional: Clear flag when app closes? 
            // Usually not needed as Activity destruction clears window flags, 
            // but effectively here since MainScreen lives as long as App (mostly).
        }
    }

    // --- GLOBAL LOGIC: New Order Sound ---
    val orders by viewModel.orders.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    var knownPendingIds by remember { mutableStateOf(setOf<Int>()) }
    var isFirstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(orders) {
        val currentPending = orders.filter { it.status == "Pending" }
        val currentIds = currentPending.map { it.id }.toSet()
        
        if (isFirstLoad) {
            // First load: just sync, don't ring
            knownPendingIds = currentIds
            isFirstLoad = false
        } else {
            // Check for NEW pending orders
            val newOrderIds = currentIds.subtract(knownPendingIds)
            if (newOrderIds.isNotEmpty()) {
                if (isSoundEnabled) {
                    playNotificationSound(context)
                }
                // Update known IDs to current + any previous (strictly current is safer)
                knownPendingIds = currentIds
            } else {
                // Determine if we should update knownIds?
                // If an order is finished, it leaves currentIds.
                // We should update knownIds to match currentIds so we play sound if it somehow comes back or new one comes.
                knownPendingIds = currentIds
            }
        }
    }

    // --- NAVIGATION ---
    // Logic to decide if we show bottom bar. 
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
                    "dashboard" -> DashboardScreen(
                        onNavigate = { target -> 
                            if (target == "login") onLogout() else currentScreen = target
                        },
                        viewModel = viewModel // Share the instance!
                    )
                    "riwayat" -> RiwayatScreen(onNavigate = { target -> currentScreen = target })
                    "menu" -> MenuScreen(onNavigate = { target -> currentScreen = target })
                    "meja" -> TableScreen(onNavigate = { target -> currentScreen = target })
                    "bayar" -> ScanScreen(onNavigate = { target -> currentScreen = target })
                    
                    "profile" -> ProfileScreen(onNavigate = { target -> 
                         if (target == "login") onLogout() else currentScreen = target
                    })
                    else -> DashboardScreen(onNavigate = { currentScreen = it }, viewModel = viewModel)
                }
            }
        }
    }
}
