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

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    initialScreen: String = "dashboard",
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel() // Global ViewModel instance
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var backPressedTime by remember { mutableLongStateOf(0L) }
    
    // --- GLOBAL LOGIC: Always On Display ---
    val isAlwaysOn by viewModel.isAlwaysOn.collectAsState()
    val context = LocalContext.current
    val window = (context as? android.app.Activity)?.window
    val activity = (context as? android.app.Activity)

    // HANDLE BACK PRESS
    BackHandler {
        if (currentScreen != "dashboard") {
            // If on another tab, go back to Dashboard
            currentScreen = "dashboard"
        } else {
            // If on Dashboard, check for double press
            if (System.currentTimeMillis() - backPressedTime < 2000) {
                activity?.finish()
            } else {
                backPressedTime = System.currentTimeMillis()
                Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
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
    // --- GLOBAL LOGIC: New Order & Cancellation Sound ---
    val orders by viewModel.orders.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    var knownPendingIds by remember { mutableStateOf(setOf<Int>()) }
    var knownCancellationIds by remember { mutableStateOf(setOf<Int>()) }
    var isFirstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(orders) {
        // 1. New Order Logic (Pending)
        val currentPending = orders.filter { it.status == "Pending" }
        val currentPendingIds = currentPending.map { it.id }.toSet()
        
        // 2. Cancellation Request Logic (Requested)
        val currentCancellation = orders.filter { it.cancellationStatus == "Requested" }
        val currentCancellationIds = currentCancellation.map { it.id }.toSet()
        
        if (isFirstLoad) {
            // First load: just sync, don't ring
            knownPendingIds = currentPendingIds
            knownCancellationIds = currentCancellationIds
            isFirstLoad = false
        } else {
            // Check for NEW pending orders
            val newOrderIds = currentPendingIds.subtract(knownPendingIds)
            if (newOrderIds.isNotEmpty()) {
                if (isSoundEnabled) {
                    com.example.kasir.utils.playOrderSound(context)
                }
                knownPendingIds = currentPendingIds
            } else {
                // If ids are removed (processed/cancelled), update strictly to current
                knownPendingIds = currentPendingIds
            }

            // Check for NEW cancellation requests
            val newCancellationIds = currentCancellationIds.subtract(knownCancellationIds)
            if (newCancellationIds.isNotEmpty()) {
                if (isSoundEnabled) {
                    com.example.kasir.utils.playCancellationSound(context)
                }
                knownCancellationIds = currentCancellationIds
            } else {
                knownCancellationIds = currentCancellationIds
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
