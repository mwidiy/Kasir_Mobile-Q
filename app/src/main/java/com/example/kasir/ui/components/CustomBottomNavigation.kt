package com.example.kasir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kasir.TextMain
import com.example.kasir.TextMuted

@Composable
fun CustomBottomNavigation(
    currentScreen: String,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    // FIX: Apply navigationBarsPadding to lift the whole bottom nav above system buttons
    // Root: Full width, transparent, respects alignment from caller
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        // 1. Curtain: Solid background behind system navigation bars
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Color.White)
        )

        // 2. Navigation Content: Lifted UP above the system bars
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars) // Adds padding for system bars
                .height(100.dp)
        ) {
            // White Background Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(80.dp)
                    .shadow(elevation = 20.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(Icons.Filled.Dashboard, "Dasbor", currentScreen == "dashboard", Modifier.weight(1f)) { onNavigate("dashboard") }
                    NavItem(Icons.Filled.ListAlt, "Riwayat", currentScreen == "riwayat", Modifier.weight(1f)) { onNavigate("riwayat") }
                    Spacer(modifier = Modifier.weight(1f)) // Middle Space
                    NavItem(Icons.Filled.MenuBook, "Menu", currentScreen == "menu", Modifier.weight(1f)) { onNavigate("menu") }
                    NavItem(Icons.Filled.QrCode, "Meja", currentScreen == "meja", Modifier.weight(1f)) { onNavigate("meja") }
                }
            }

            // Floating Middle Button (Bayar)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-5).dp)
                    .size(70.dp)
                    .shadow(8.dp, CircleShape) // Shadow FIRST
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp) // White Border Thickness
                    .clip(CircleShape)
                    .background(Color(0xFF2C3E50)) // Brand Color
                    .clickable { onNavigate("bayar") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Bayar", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Text for Middle Button
            Text(
                text = "Bayar",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextMain),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isActive: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) TextMain else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp, 
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) TextMain else TextMuted
            )
        )
    }
}
