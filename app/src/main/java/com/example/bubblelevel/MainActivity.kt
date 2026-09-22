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
            PurpleMaterial3Theme {
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
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
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
    val isHorizontalLevel = abs(roll) < 0.5f
    val isVerticalLevel = abs(pitch) < 0.5f

    val mainAccent by animateColorAsState(
        targetValue = if (isLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        animationSpec = tween(300), label = "MainAccent"
    )

    val horizAccent by animateColorAsState(
        targetValue = if (isHorizontalLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        animationSpec = tween(300), label = "HorizAccent"
    )

    val vertAccent by animateColorAsState(
        targetValue = if (isVerticalLevel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        animationSpec = tween(300), label = "VertAccent"
    )

    val containerBg = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status Badge
        Surface(
            shape = CircleShape,
            color = if (isLevel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
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

        // Horizontal Tubular Level (Top Bar)
        HorizontalTubularLevel(
            roll = roll,
            color = horizAccent,
            backgroundColor = containerBg
        )

        // Middle Section: Vertical Level (Left) + Surface Level (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical Tubular Level
            VerticalTubularLevel(
                pitch = pitch,
                color = vertAccent,
                backgroundColor = containerBg
            )

            // Circular Surface Level
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val outerRadius = size.width / 2 - 16.dp.toPx()
                    val targetRadius = outerRadius * 0.35f

                    drawCircle(
                        color = mainAccent.copy(alpha = 0.2f),
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    drawCircle(
                        color = mainAccent.copy(alpha = 0.1f),
                        radius = targetRadius,
                        center = center
                    )

                    val maxOffset = outerRadius - 28.dp.toPx()
                    val rawOffsetX = (-roll / 45f) * maxOffset
                    val rawOffsetY = (pitch / 45f) * maxOffset

                    val currentDistance = sqrt(rawOffsetX * rawOffsetX + rawOffsetY * rawOffsetY)
                    val clampedDistance = currentDistance.coerceAtMost(maxOffset)

                    val angle = atan2(rawOffsetY, rawOffsetX)
                    val bubbleX = center.x + (clampedDistance * cos(angle))
                    val bubbleY = center.y + (clampedDistance * sin(angle))

                    drawCircle(
                        color = mainAccent,
                        radius = 24.dp.toPx(),
                        center = Offset(bubbleX, bubbleY)
                    )
                }
            }
        }

        // Bottom Metrics Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(label = "ROLL (HORIZ)", value = roll, modifier = Modifier.weight(1f))
            MetricCard(label = "PITCH (VERT)", value = pitch, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun HorizontalTubularLevel(roll: Float, color: Color, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxOffset = (size.width / 2) - 28.dp.toPx()
            val bubbleOffsetX = ((-roll / 45f) * maxOffset).coerceIn(-maxOffset, maxOffset)

            // Target notch marks
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = Offset(center.x - 20.dp.toPx(), 0f),
                end = Offset(center.x - 20.dp.toPx(), size.height),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = Offset(center.x + 20.dp.toPx(), 0f),
                end = Offset(center.x + 20.dp.toPx(), size.height),
                strokeWidth = 2.dp.toPx()
            )

            // Bubble
            drawCircle(
                color = color,
                radius = 16.dp.toPx(),
                center = Offset(center.x + bubbleOffsetX, center.y)
            )
        }
    }
}

@Composable
fun VerticalTubularLevel(pitch: Float, color: Color, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxOffset = (size.height / 2) - 28.dp.toPx()
            val bubbleOffsetY = ((pitch / 45f) * maxOffset).coerceIn(-maxOffset, maxOffset)

            // Target notch marks
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = Offset(0f, center.y - 20.dp.toPx()),
                end = Offset(size.width, center.y - 20.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = Offset(0f, center.y + 20.dp.toPx()),
                end = Offset(size.width, center.y + 20.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Bubble
            drawCircle(
                color = color,
                radius = 16.dp.toPx(),
                center = Offset(center.x, center.y + bubbleOffsetY)
            )
        }
    }
}

@Composable
fun MetricCard(label: String, value: Float, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Material 3 Purple Theme Palette
@Composable
fun PurpleMaterial3Theme(content: @Composable () -> Unit) {
    val purpleColorScheme = lightColorScheme(
        primary = Color(0xFF6750A4),            // M3 Deep Violet/Purple (Level state)
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),   // Soft Lavender container
        onPrimaryContainer = Color(0xFF21005D),
        tertiary = Color(0xFF7D5260),           // Soft Muted Plum (Unlevel state)
        surface = Color(0xFFFEF7FF),            // Light Purple tinted background
        onSurface = Color(0xFF1D1B20),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0E9),
        onSurfaceVariant = Color(0xFF49454F)
    )

    MaterialTheme(
        colorScheme = purpleColorScheme,
        content = content
    )
}