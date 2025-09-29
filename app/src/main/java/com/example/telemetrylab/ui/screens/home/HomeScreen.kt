package com.example.telemetrylab.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemetrylab.R
import com.example.telemetrylab.service.TelemetryService
import com.example.telemetrylab.ui.components.InfoCard
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState(initial = HomeUiState())
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isRunning) {
        if (uiState.isRunning) {
            TelemetryService.start(context, uiState.computeLoad)
        } else {
            TelemetryService.stop(context)
        }
    }
    
    LaunchedEffect(uiState.computeLoad) {
        if (uiState.isRunning) {
            TelemetryService.updateComputeLoad(context, uiState.computeLoad)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (uiState.isPowerSaveMode) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.power_save_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.telemetry_controls),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Start/Stop button
                        Button(
                            onClick = { viewModel.toggleTelemetry() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = if (uiState.isRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            val buttonText = if (uiState.isRunning) {
                                stringResource(R.string.stop_telemetry)
                            } else {
                                stringResource(R.string.start_telemetry)
                            }
                            Text(buttonText)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "${stringResource(R.string.compute_load)}: ${uiState.computeLoad}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Slider(
                            value = uiState.computeLoad.toFloat(),
                            onValueChange = { viewModel.updateComputeLoad(it.roundToInt()) },
                            valueRange = 1f..5f,
                            steps = 4,
                            enabled = !uiState.isRunning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = stringResource(R.string.performance_metrics),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InfoCard(
                        title = stringResource(R.string.frame_latency, uiState.currentFrameLatencyMs),
                        value = "${uiState.currentFrameLatencyMs.toInt()} ms",
                        modifier = Modifier.weight(1f)
                    )
                    
                    InfoCard(
                        title = stringResource(R.string.avg_latency, uiState.averageLatencyMs),
                        value = "${uiState.averageLatencyMs.toInt()} ms",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InfoCard(
                        title = stringResource(R.string.jank_percentage, uiState.jankPercentage),
                        value = "${uiState.jankPercentage.toInt()}%",
                        valueColor = when {
                            uiState.jankPercentage > 10 -> MaterialTheme.colorScheme.error
                            uiState.jankPercentage > 5 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    InfoCard(
                        title = stringResource(R.string.jank_count, uiState.jankCount),
                        value = uiState.jankCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.current_frame, uiState.currentFrameId),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        if (uiState.isRunning) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = stringResource(R.string.recent_frames),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                val frameHistory = remember { List(10) { it * 10L } }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    frameHistory.forEach { value ->
                        val height = (value % 100).toFloat() / 100f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(height)
                                .background(
                                    color = if (value > 50) MaterialTheme.colorScheme.error 
                                           else MaterialTheme.colorScheme.primary,
    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }
            }
        }
    }
}
