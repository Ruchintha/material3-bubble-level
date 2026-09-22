package com.example.bubblelevel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var pitchState = mutableStateOf(0f)
    private var rollState = mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            Material3Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        CenterAlignedTopAppBar(
                            title = { Text("Material 3 Level", style = MaterialTheme.typography.titleMedium) },
                            navigationIcon = {
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    pitchState.value = 0f
                                    rollState.value = 0f
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Calibrate")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    LevelScreen(
                        pitch = pitchState.value,
                        roll = rollState.value,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]

            val pitch = atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble())) * (180 / Math.PI)
            val roll = atan2(-ax.toDouble(), az.toDouble()) * (180 / Math.PI)

            pitchState.value = pitch.toFloat()
            rollState.value = roll.toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun LevelScreen(pitch: Float, roll: Float, modifier: Modifier = Modifier) {
    val isLevel = abs(pitch) < 0.5f && abs(roll) < 0.5f

    // Dynamic Material 3 Expressive Color System
    val bubbleColor by animateColorAsState(
        targetValue = if (isLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        animationSpec = tween(300), label = "BubbleColor"
    )
    val ringColor by animateColorAsState(
        targetValue = if (isLevel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(300), label = "RingColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Material 3 Status Badge
        Surface(
            shape = CircleShape,
            color = if (isLevel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLevel) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = if (isLevel) "PERFECTLY LEVEL" else "ALIGNING...",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLevel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Main Circular Material Level Indicator
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(ringColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.width / 2 - 16.dp.toPx()
                val targetRadius = outerRadius * 0.35f

                // Outer Guideline Circle
                drawCircle(
                    color = bubbleColor.copy(alpha = 0.2f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Inner Target Zone
                drawCircle(
                    color = bubbleColor.copy(alpha = 0.1f),
                    radius = targetRadius,
                    center = center
                )

                // Offset Calculation
                val maxOffset = outerRadius - 32.dp.toPx()
                val rawOffsetX = (-roll / 45f) * maxOffset
                val rawOffsetY = (pitch / 45f) * maxOffset

                val currentDistance = sqrt(rawOffsetX * rawOffsetX + rawOffsetY * rawOffsetY)
                val clampedDistance = currentDistance.coerceAtMost(maxOffset)

                val angle = atan2(rawOffsetY, rawOffsetX)
                val bubbleX = center.x + (clampedDistance * cos(angle))
                val bubbleY = center.y + (clampedDistance * sin(angle))

                // Floating Material Bubble
                drawCircle(
                    color = bubbleColor,
                    radius = 28.dp.toPx(),
                    center = Offset(bubbleX, bubbleY)
                )
            }
        }

        // M3 Elevation Metric Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                label = "PITCH",
                value = pitch,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "ROLL",
                value = roll,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(label: String, value: Float, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%.1f°".format(abs(value)),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun Material3Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
