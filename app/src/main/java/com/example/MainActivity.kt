package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthManager
import com.example.ui.AgroViewModel
import com.example.ui.screens.CalibrationGuideScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.ImpactScreen
import com.example.ui.screens.SemaforoScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.AgroVigiaTheme
import com.example.ui.theme.SemaforoGreen
import kotlinx.coroutines.launch

enum class AppNavDestination(val label: String, val icon: ImageVector) {
    MONITOR("Monitor", Icons.Default.Sensors),
    CALIBRATE("Montaje", Icons.Default.CompassCalibration),
    DIAGNOSTICS("Historial", Icons.Default.Assessment),
    IMPACT("Impacto", Icons.Default.Eco)
}

class MainActivity : ComponentActivity() {

    private val authManager by lazy { AuthManager() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AgroVigiaTheme(darkTheme = true) {
                RootAuthGate(authManager = authManager)
            }
        }
    }
}

@Composable
fun RootAuthGate(authManager: AuthManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by authManager.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = authManager.currentUser)

    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }

    val user = currentUser
    if (user == null) {
        val dummyDeviceManager = remember { com.example.device.DeviceManager(context) }
        SignInScreen(
            deviceId = dummyDeviceManager.deviceId,
            deviceModel = dummyDeviceManager.hardwareModel,
            isLoading = isSigningIn,
            errorMessage = signInError,
            onSignInClick = {
                isSigningIn = true
                signInError = null
                coroutineScope.launch {
                    val result = authManager.signInWithGoogle(context)
                    isSigningIn = false
                    result.onFailure { exception ->
                        signInError = exception.localizedMessage ?: "Error al iniciar sesión con Google"
                    }
                }
            }
        )
    } else {
        MainApp(
            authenticatedUserId = user.uid,
            onSignOut = {
                authManager.signOut()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    authenticatedUserId: String,
    onSignOut: () -> Unit,
    viewModel: AgroViewModel = viewModel()
) {
    var currentDestination by remember { mutableStateOf(AppNavDestination.MONITOR) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authenticatedUserId) {
        viewModel.setAuthenticatedUser(authenticatedUserId)
    }

    val currentPump by viewModel.currentPump.collectAsState()

    // BackHandler: return to Monitor tab if in sub-screen
    BackHandler(enabled = currentDestination != AppNavDestination.MONITOR) {
        currentDestination = AppNavDestination.MONITOR
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BOMBAVIGIA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${currentPump?.name ?: "Bomba de Riego"} • ${viewModel.deviceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("btn_top_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D140F)
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D140F),
                contentColor = Color(0xFFE2E8F0),
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                AppNavDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = SemaforoGreen,
                            indicatorColor = SemaforoGreen,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF090D0A))
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppNavDestination.MONITOR -> SemaforoScreen(viewModel = viewModel)
                    AppNavDestination.CALIBRATE -> CalibrationGuideScreen(viewModel = viewModel)
                    AppNavDestination.DIAGNOSTICS -> DiagnosticsScreen(viewModel = viewModel)
                    AppNavDestination.IMPACT -> ImpactScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentPump = currentPump,
            deviceId = viewModel.deviceId,
            deviceModel = viewModel.deviceModel,
            telegramTokenInit = viewModel.telegramToken,
            telegramChatInit = viewModel.telegramChatId,
            webhookUrlInit = viewModel.webhookUrl,
            autoSmsInit = viewModel.autoSmsEnabled,
            onTestAutonomousDispatch = { viewModel.testAutonomousDispatch() },
            onSignOut = {
                showSettingsDialog = false
                onSignOut()
            },
            onDismiss = { showSettingsDialog = false },
            onSave = { name, sector, phone, yellowThresh, redThresh, tgToken, tgChat, webhook, autoSms ->
                viewModel.updatePumpSettings(name, sector, phone, yellowThresh, redThresh, tgToken, tgChat, webhook, autoSms)
            }
        )
    }
}
