package com.example.telemetrylab.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemetrylab.service.TelemetryService
import com.example.telemetrylab.ui.components.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// Elevation constants
private val cardElevation = 4.dp
private val smallElevation = 2.dp
private val largeElevation = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val systemUiController = rememberSystemUiController()
    val isDarkTheme = isSystemInDarkTheme()
    val statusBarColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    }

    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = !isDarkTheme
        )
    }

    // Handle service lifecycle
    LaunchedEffect(uiState.isRunning) {
        if (uiState.isRunning) {
            TelemetryService.start(context, uiState.computeLoad)
        } else {
            TelemetryService.stop(context)
        }
    }

    // Auto-scroll to show latest metrics
    LaunchedEffect(uiState.currentFrameId) {
        if (uiState.isRunning) {
            delay(100)
            coroutineScope.launch {
                scrollState.animateScrollToItem(0)
            }
        }
    }

    // Handle system UI

    // Main container with top app bar
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Telemetry Lab",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.shadow(smallElevation)
            )
        }
    ) { innerPadding ->
        // Main content
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Power Save Banner
            if (uiState.isPowerSaveMode) {
                item {
                    AnimatedVisibility(
                        visible = uiState.isPowerSaveMode,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        PowerSaveBanner()
                    }
                }
            }

            // Main Control Card
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically { it / 2 } + fadeIn()
                ) {
                    ControlCard(
                        isRunning = uiState.isRunning,
                        computeLoad = uiState.computeLoad,
                        onToggleTelemetry = { viewModel.toggleTelemetry() },
                        onComputeLoadChange = { viewModel.updateComputeLoad(it) },
                        isPowerSaveMode = uiState.isPowerSaveMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
            }

            // Real-time Metrics Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Performance Dashboard",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // First Row - Core Metrics
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricCard(
                            title = "Frame Latency",
                            value = "${uiState.currentFrameLatencyMs.toInt()}",
                            unit = "ms",
                            subtitle = "Current",
                            icon = Icons.Outlined.Speed,
                            gradient = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                )
                            ),
                            modifier = Modifier.weight(1f),
                            isAnimated = uiState.isRunning
                        )

                        MetricCard(
                            title = "Avg Latency",
                            value = "${uiState.averageLatencyMs.toInt()}",
                            unit = "ms",
                            subtitle = "30s Average",
                            icon = Icons.Outlined.Timeline,
                            gradient = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                )
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Second Row - Performance Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricCard(
                            title = "Jank Rate",
                            value = "${uiState.jankPercentage.toInt()}",
                            unit = "%",
                            subtitle = "> 16.67ms",
                            icon = Icons.Outlined.Warning,
                            gradient = Brush.verticalGradient(
                                colors = listOf(
                                    when {
                                        uiState.jankPercentage > 10 -> MaterialTheme.colorScheme.errorContainer
                                        uiState.jankPercentage > 5 -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    when {
                                        uiState.jankPercentage > 10 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                        uiState.jankPercentage > 5 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                    }
                                )
                            ),
                            valueColor = when {
                                uiState.jankPercentage > 10 -> MaterialTheme.colorScheme.error
                                uiState.jankPercentage > 5 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.weight(1f)
                        )

                        MetricCard(
                            title = "Frame Count",
                            value = uiState.currentFrameId.toString(),
                            unit = "",
                            subtitle = "Total Processed",
                            icon = Icons.Outlined.FilterFrames,
                            gradient = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                                )
                            ),
                            modifier = Modifier.weight(1f),
                            isLoading = uiState.isRunning
                        )
                    }
                }
            }

            // Jank Warning
            if (uiState.jankPercentage > 5) {
                item {
                    AnimatedVisibility(
                        visible = uiState.jankPercentage > 5,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        JankWarningCard(
                            jankPercentage = uiState.jankPercentage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        )
                    }
                }
            }

            // Performance Visualization
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically { it / 2 } + fadeIn()
                ) {
                    PerformanceVisualization(
                        isRunning = uiState.isRunning,
                        frameRate = if (uiState.isPowerSaveMode) 10 else 20,
                        computeLoad = uiState.computeLoad,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
            }

            // Recent Frames List
            if (uiState.isRunning) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .shadow(
                                elevation = largeElevation,
                                shape = RoundedCornerShape(16.dp),
                                clip = true
                            ),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        RecentFramesList(
                            frameCount = uiState.currentFrameId,
                            averageLatency = uiState.averageLatencyMs,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}




