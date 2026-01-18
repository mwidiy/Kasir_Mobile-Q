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
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    // FIX: Apply navigationBarsPadding to lift the whole bottom nav above system buttons
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent) // Ensure background behind nav is transparent
            .navigationBarsPadding() // Adds bottom padding equal to navigation bar height
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
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Filled.Dashboard, "Dasbor", true) { onNavigate("dashboard") }
                NavItem(Icons.Filled.ListAlt, "Riwayat", false) { onNavigate("riwayat") }
                Spacer(modifier = Modifier.width(56.dp)) // Space for Middle Button
                NavItem(Icons.Filled.MenuBook, "Menu", false) { onNavigate("menu") }
                NavItem(Icons.Filled.QrCode, "Meja", false) { onNavigate("meja") } // "meja" matches Target route
            }
        }

        // Floating Middle Button (Bayar)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 10.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))))
                .clickable { onNavigate("bayar") }
                .shadow(8.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Bayar", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        
        // Text for Middle Button (Positioned manually below float)
        Text(
            text = "Bayar",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextMain),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
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
