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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasirTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var currentScreen by remember { mutableStateOf("login") }

                    when (currentScreen) {
                        "login" -> LoginScreen(onGoogleLoginClick = { currentScreen = "dashboard" })
                        "dashboard" -> DashboardScreen(onNavigate = { screen -> currentScreen = screen })
                        "riwayat" -> RiwayatScreen(onNavigate = { screen -> currentScreen = screen })
                        "menu" -> MenuScreen(onNavigate = { screen -> currentScreen = screen })
                        "meja" -> TableScreen(onNavigate = { screen -> currentScreen = screen })
                        "bayar" -> ScanScreen(onNavigate = { screen -> currentScreen = screen })
                        "profile" -> ProfileScreen(onNavigate = { screen -> currentScreen = screen })
                        else -> DashboardScreen(onNavigate = { screen -> currentScreen = screen }) 
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