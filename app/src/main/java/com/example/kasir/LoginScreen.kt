package com.example.kasir

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kasir.ui.theme.KasirTheme

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

import kotlinx.coroutines.launch
import com.example.kasir.utils.SessionManager
import com.example.kasir.data.network.RetrofitClient
import com.example.kasir.data.model.LoginRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import android.util.Log

// --- COLORS ---
val PrimaryBlue = Color(0xFF1E3A5F)
val TextDark = Color(0xFF1A2B48)
val TextGrey = Color(0xFF6C757D)
val BgAccent = Color(0xFFFFFBF2) 

// Decoration Palette
val GoogleRed = Color(0xFFEA4335).copy(alpha = 0.4f)
val GoogleBlue = Color(0xFF4285F4).copy(alpha = 0.4f)
val GoogleYellow = Color(0xFFFBBC05).copy(alpha = 0.4f)
val GoogleGreen = Color(0xFF34A853).copy(alpha = 0.4f)
val BrandDarkBlue = Color(0xFF1E3A5F).copy(alpha = 0.4f)
val BrandGrey = Color(0xFF6C757D).copy(alpha = 0.4f)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // --- LOCAL STATE (Replacement for ViewModel) ---
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val webClientId = BuildConfig.WEB_CLIENT_ID 

    // --- Google Sign In Params ---
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            
            if (idToken != null) {
                // --- INLINE API LOGIC ---
                isLoading = true
                scope.launch {
                    try {
                        val response = RetrofitClient.instance.googleLogin(LoginRequest(idToken))
                        if (response.isSuccessful) {
                             val loginResponse = response.body()
                             if (loginResponse != null && loginResponse.success) {
                                val token = loginResponse.token
                                val user = loginResponse.user
                                // SessionManager.saveSession expects (Context, String, User)
                                // Make sure 'user' matches what saveSession expects or map it if needed
                                SessionManager.saveSession(context, token, user)
                                onLoginSuccess()
                             } else {
                                errorMessage = "Invalid Server Response"
                             }
                        } else {
                            errorMessage = "Login Failed: ${response.message()}"
                        }
                    } catch (e: Exception) {
                        Log.e("LoginScreen", "API Error", e)
                        errorMessage = "Error: ${e.message}"
                        Toast.makeText(context, "API Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                    }
                }
            } else {
                Toast.makeText(context, "Google Token Missing", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("LoginScreen", "Google Sign In Failed", e)
            Toast.makeText(context, "Login Failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        SessionManager.loadSession(context)
        if (SessionManager.isLoggedIn()) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SpaceFloatingBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "QuackXel Logo",
                modifier = Modifier
                    .width(120.dp)
                    .padding(bottom = 40.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Kelola Dasbor Anda",
                color = PrimaryBlue,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            Text(
                text = "Masuk dengan akun Google Anda untuk melanjutkan.",
                color = TextGrey,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(280.dp)
                    .padding(bottom = 50.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage.toString(), 
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
 
            if (isLoading) {
                 androidx.compose.material3.CircularProgressIndicator(color = PrimaryBlue)
                 Text("Menghubungi Server...", fontSize=10.sp, color = TextGrey, modifier = Modifier.padding(top=8.dp))
            } else {
                 GoogleSignInButtonImpl(
                    onSignInClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    }
                 )
            }
        }
    }
}

// ... SpaceFloatingBackground and Helpers (No Change) ...

@Composable
fun SpaceFloatingBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "RefinedSpaceFloat")
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    SpaceFloatShape(infiniteTransition, SpaceShape.Hexagon, GoogleBlue, screenWidth * 0.1f, screenHeight * 0.2f, 40f, 8000, 60.dp, true)
    SpaceFloatShape(infiniteTransition, SpaceShape.Star, GoogleRed, screenWidth * 0.8f, screenHeight * 0.85f, -200f, 15000, 50.dp, true)
    SpaceFloatShape(infiniteTransition, SpaceShape.Circle, GoogleYellow, screenWidth * 0.9f, screenHeight * 0.4f, 30f, 6000, 40.dp)
    SpaceFloatShape(infiniteTransition, SpaceShape.Triangle, GoogleGreen, screenWidth * 0.15f, screenHeight * 0.75f, -100f, 12000, 55.dp, true)
    SpaceFloatShape(infiniteTransition, SpaceShape.Square, BrandDarkBlue, screenWidth * 0.75f, screenHeight * 0.15f, 50f, 9000, 45.dp, true)
    SpaceFloatShape(infiniteTransition, SpaceShape.Hexagon, BrandGrey, screenWidth * 0.2f, screenHeight * 0.5f, 80f, 11000, 70.dp, true)
    SpaceFloatShape(infiniteTransition, SpaceShape.Star, GoogleBlue.copy(alpha = 0.25f), screenWidth * 0.5f, screenHeight * 0.9f, -300f, 20000, 30.dp, true)
}

enum class SpaceShape { Circle, Square, Triangle, Star, Hexagon }

@Composable
fun SpaceFloatShape(
    infiniteTransition: InfiniteTransition,
    shape: SpaceShape,
    color: Color,
    startX: androidx.compose.ui.unit.Dp,
    startY: androidx.compose.ui.unit.Dp,
    animYOffset: Float,
    duration: Int,
    size: androidx.compose.ui.unit.Dp,
    rotation: Boolean = false
) {
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = animYOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yFloat"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (rotation) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Canvas(
        modifier = Modifier
            .offset(x = startX, y = startY + yOffset.dp)
            .size(size)
            .graphicsLayer {
                rotationZ = rotateAngle
            }
    ) {
        when (shape) {
            SpaceShape.Circle -> drawCircle(color = color)
            SpaceShape.Square -> drawRect(color = color)
            SpaceShape.Triangle -> {
                 val path = Path().apply {
                    moveTo(this@Canvas.size.width / 2, 0f)
                    lineTo(this@Canvas.size.width, this@Canvas.size.height)
                    lineTo(0f, this@Canvas.size.height)
                    close()
                }
                drawPath(path, color)
            }
            SpaceShape.Star -> {
                val path = createStarPath(this.size.width, this.size.height, 5)
                drawPath(path, color)
            }
            SpaceShape.Hexagon -> {
                val path = createPolygonPath(this.size.width, this.size.height, 6)
                drawPath(path, color)
            }
        }
    }
}

fun createStarPath(width: Float, height: Float, points: Int = 5): Path {
    val outerRadius = width / 2
    val innerRadius = outerRadius / 2.5f
    val centerX = width / 2
    val centerY = height / 2
    val section = 2.0 * PI / points
    val path = Path()
    path.reset()
    path.moveTo((centerX + outerRadius * cos(0.0 - PI / 2)).toFloat(), (centerY + outerRadius * sin(0.0 - PI / 2)).toFloat())
    for (i in 1..points) {
        path.lineTo((centerX + innerRadius * cos(section * i - section / 2 - PI / 2)).toFloat(), (centerY + innerRadius * sin(section * i - section / 2 - PI / 2)).toFloat())
        path.lineTo((centerX + outerRadius * cos(section * i - PI / 2)).toFloat(), (centerY + outerRadius * sin(section * i - PI / 2)).toFloat())
    }
    path.close()
    return path
}

fun createPolygonPath(width: Float, height: Float, sides: Int = 6): Path {
    val radius = width / 2
    val centerX = width / 2
    val centerY = height / 2
    val section = 2.0 * PI / sides
    val path = Path()
    path.reset()
    path.moveTo((centerX + radius * cos(0.0)).toFloat(), (centerY + radius * sin(0.0)).toFloat())
    for (i in 1 until sides) {
        path.lineTo((centerX + radius * cos(section * i)).toFloat(), (centerY + radius * sin(section * i)).toFloat())
    }
    path.close()
    return path
}

@Composable
fun GoogleSignInButtonImpl(onSignInClick: () -> Unit) {
    com.example.kasir.ui.components.GoogleButton(
        onClick = onSignInClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    KasirTheme {
        LoginScreen()
    }
}
