package com.ermek.parentwellness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ermek.parentwellness.data.repository.BloodPressureData
import com.ermek.parentwellness.data.repository.BloodSugarData
import com.ermek.parentwellness.data.repository.HeartRateData
import com.ermek.parentwellness.data.repository.StepsData
import com.ermek.parentwellness.ui.theme.PrimaryRed
import java.text.SimpleDateFormat
import java.util.*

/**
 * A component for displaying empty chart placeholder
 */
@Composable
fun EmptyChartPlaceholder(message: String = "No data available") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A component that displays chart for heart rate data
 */
@Composable
fun HeartRateChart(
    data: List<HeartRateData>,
    modifier: Modifier = Modifier,
    lineColor: Color = PrimaryRed
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("No heart rate data available")
        return
    }

    // Sort data by timestamp (newest to oldest)
    val sortedData = remember(data) {
        data.sortedBy { it.timestamp }
    }

    // Find min and max values with padding
    val maxHeartRate = remember(sortedData) {
        sortedData.maxOfOrNull { it.heartRate }?.plus(10) ?: 100
    }
    val minHeartRate = remember(sortedData) {
        (sortedData.minOfOrNull { it.heartRate }?.minus(10) ?: 50).coerceAtLeast(0)
    }
    val heartRateRange = remember(maxHeartRate, minHeartRate) {
        (maxHeartRate - minHeartRate).coerceAtLeast(1)
    }

    // Date formatter
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = maxHeartRate.toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = ((maxHeartRate + minHeartRate) / 2).toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = minHeartRate.toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Line chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, bottom = 20.dp, top = 8.dp, end = 8.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw horizontal grid lines
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (i in 0..2) {
                val y = canvasHeight - (canvasHeight / 2 * i)
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f,
                    pathEffect = pathEffect
                )
            }

            if (sortedData.size > 1) {
                // Draw the connecting lines
                for (i in 0 until sortedData.size - 1) {
                    val startX = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val startY = canvasHeight - ((sortedData[i].heartRate - minHeartRate).toFloat() / heartRateRange * canvasHeight)

                    val endX = ((i + 1).toFloat() / (sortedData.size - 1)) * canvasWidth
                    val endY = canvasHeight - ((sortedData[i + 1].heartRate - minHeartRate).toFloat() / heartRateRange * canvasHeight)

                    drawLine(
                        color = lineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Draw points
                for (i in sortedData.indices) {
                    val x = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val y = canvasHeight - ((sortedData[i].heartRate - minHeartRate).toFloat() / heartRateRange * canvasHeight)

                    drawCircle(
                        color = lineColor,
                        radius = 5f,
                        center = Offset(x, y)
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            } else if (sortedData.size == 1) {
                // Draw a single point if there's only one data point
                val x = canvasWidth / 2
                val y = canvasHeight - ((sortedData[0].heartRate - minHeartRate).toFloat() / heartRateRange * canvasHeight)

                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = Offset(x, y)
                )

                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }

        // Date axis labels (X-axis)
        if (sortedData.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sortedData.size > 1) {
                    // Show first, middle and last date
                    Text(
                        text = dateFormat.format(Date(sortedData.first().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    if (sortedData.size > 2) {
                        Text(
                            text = dateFormat.format(Date(sortedData[sortedData.size / 2].timestamp)),
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(sortedData.last().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                } else {
                    // Just the single date
                    Text(
                        text = dateFormat.format(Date(sortedData.first().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * A component that displays chart for blood pressure data
 */
@Composable
fun BloodPressureChart(
    data: List<BloodPressureData>,
    modifier: Modifier = Modifier,
    systolicColor: Color = Color.Red,
    diastolicColor: Color = Color.Blue
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("No blood pressure data available")
        return
    }

    // Sort data by timestamp
    val sortedData = remember(data) {
        data.sortedBy { it.timestamp }
    }

    // Find min and max values with padding
    val maxSystolic = remember(sortedData) {
        sortedData.maxOfOrNull { it.systolic }?.plus(10) ?: 140
    }
    val minSystolic = remember(sortedData) {
        (sortedData.minOfOrNull { it.systolic }?.minus(10) ?: 90).coerceAtLeast(0)
    }
    val maxDiastolic = remember(sortedData) {
        sortedData.maxOfOrNull { it.diastolic }?.plus(10) ?: 90
    }
    val minDiastolic = remember(sortedData) {
        (sortedData.minOfOrNull { it.diastolic }?.minus(10) ?: 50).coerceAtLeast(0)
    }

    // Use a combined range for both values
    val maxValue = remember(maxSystolic, maxDiastolic) {
        maxOf(maxSystolic, maxDiastolic)
    }
    val minValue = remember(minSystolic, minDiastolic) {
        minOf(minSystolic, minDiastolic)
    }
    val valueRange = remember(maxValue, minValue) {
        (maxValue - minValue).coerceAtLeast(1)
    }

    // Date formatter
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = maxValue.toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = ((maxValue + minValue) / 2).toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = minValue.toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Line chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, bottom = 20.dp, top = 8.dp, end = 8.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw horizontal grid lines
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (i in 0..2) {
                val y = canvasHeight - (canvasHeight / 2 * i)
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f,
                    pathEffect = pathEffect
                )
            }

            if (sortedData.size > 1) {
                // Draw systolic line
                for (i in 0 until sortedData.size - 1) {
                    val startX = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val startY = canvasHeight - ((sortedData[i].systolic - minValue).toFloat() / valueRange * canvasHeight)

                    val endX = ((i + 1).toFloat() / (sortedData.size - 1)) * canvasWidth
                    val endY = canvasHeight - ((sortedData[i + 1].systolic - minValue).toFloat() / valueRange * canvasHeight)

                    drawLine(
                        color = systolicColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Draw diastolic line
                for (i in 0 until sortedData.size - 1) {
                    val startX = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val startY = canvasHeight - ((sortedData[i].diastolic - minValue).toFloat() / valueRange * canvasHeight)

                    val endX = ((i + 1).toFloat() / (sortedData.size - 1)) * canvasWidth
                    val endY = canvasHeight - ((sortedData[i + 1].diastolic - minValue).toFloat() / valueRange * canvasHeight)

                    drawLine(
                        color = diastolicColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Draw points on both lines
                for (i in sortedData.indices) {
                    val x = (i.toFloat() / (sortedData.size - 1)) * canvasWidth

                    // Systolic point
                    val systolicY = canvasHeight - ((sortedData[i].systolic - minValue).toFloat() / valueRange * canvasHeight)
                    drawCircle(
                        color = systolicColor,
                        radius = 5f,
                        center = Offset(x, systolicY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, systolicY)
                    )

                    // Diastolic point
                    val diastolicY = canvasHeight - ((sortedData[i].diastolic - minValue).toFloat() / valueRange * canvasHeight)
                    drawCircle(
                        color = diastolicColor,
                        radius = 5f,
                        center = Offset(x, diastolicY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, diastolicY)
                    )
                }
            } else if (sortedData.size == 1) {
                // Draw single points if only one data point
                val x = canvasWidth / 2

                // Systolic
                val systolicY = canvasHeight - ((sortedData[0].systolic - minValue).toFloat() / valueRange * canvasHeight)
                drawCircle(
                    color = systolicColor,
                    radius = 5f,
                    center = Offset(x, systolicY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, systolicY)
                )

                // Diastolic
                val diastolicY = canvasHeight - ((sortedData[0].diastolic - minValue).toFloat() / valueRange * canvasHeight)
                drawCircle(
                    color = diastolicColor,
                    radius = 5f,
                    center = Offset(x, diastolicY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, diastolicY)
                )
            }
        }

        // Date axis labels (X-axis)
        if (sortedData.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sortedData.size > 1) {
                    // Show first, middle and last date
                    Text(
                        text = dateFormat.format(Date(sortedData.first().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    if (sortedData.size > 2) {
                        Text(
                            text = dateFormat.format(Date(sortedData[sortedData.size / 2].timestamp)),
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(sortedData.last().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                } else {
                    // Just the single date
                    Text(
                        text = dateFormat.format(Date(sortedData.first().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Systolic legend
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(systolicColor, RoundedCornerShape(4.dp))
            )
            Text(
                text = "Systolic",
                color = Color.Gray,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Diastolic legend
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(diastolicColor, RoundedCornerShape(4.dp))
            )
            Text(
                text = "Diastolic",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * A component that displays chart for blood sugar data
 */
@Composable
fun BloodSugarChart(
    data: List<BloodSugarData>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Green
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("No blood sugar data available")
        return
    }

    // Sort data by timestamp
    val sortedData = remember(data) {
        data.sortedBy { it.timestamp }
    }

    // Find min and max values with padding
    val maxValue = remember(sortedData) {
        sortedData.maxOfOrNull { it.value }?.plus(10) ?: 200
    }
    val minValue = remember(sortedData) {
        (sortedData.minOfOrNull { it.value }?.minus(10) ?: 70).coerceAtLeast(0)
    }
    val valueRange = remember(maxValue, minValue) {
        (maxValue - minValue).coerceAtLeast(1)
    }

    // Date formatter
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = maxValue.toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = ((maxValue + minValue) / 2).toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = minValue.toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Line chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, bottom = 20.dp, top = 8.dp, end = 8.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw horizontal grid lines
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (i in 0..2) {
                val y = canvasHeight - (canvasHeight / 2 * i)
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f,
                    pathEffect = pathEffect
                )
            }

            if (sortedData.size > 1) {
                // Draw lines connecting points
                for (i in 0 until sortedData.size - 1) {
                    val startX = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val startY = canvasHeight - ((sortedData[i].value - minValue).toFloat() / valueRange * canvasHeight)

                    val endX = ((i + 1).toFloat() / (sortedData.size - 1)) * canvasWidth
                    val endY = canvasHeight - ((sortedData[i + 1].value - minValue).toFloat() / valueRange * canvasHeight)

                    drawLine(
                        color = lineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Draw points
                for (i in sortedData.indices) {
                    val x = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val y = canvasHeight - ((sortedData[i].value - minValue).toFloat() / valueRange * canvasHeight)

                    drawCircle(
                        color = lineColor,
                        radius = 5f,
                        center = Offset(x, y)
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            } else if (sortedData.size == 1) {
                // Draw a single point if there's only one data point
                val x = canvasWidth / 2
                val y = canvasHeight - ((sortedData[0].value - minValue).toFloat() / valueRange * canvasHeight)

                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = Offset(x, y)
                )

                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }

        // Date axis labels (X-axis)
        if (sortedData.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sortedData.size > 1) {
                    // Show first, middle and last date
                    Text(
                        text = dateFormat.format(Date(sortedData.first().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    if (sortedData.size > 2) {
                        Text(
                            text = dateFormat.format(Date(sortedData[sortedData.size / 2].timestamp)),
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(sortedData.last().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                } else {
                    // Just the single date
                    Text(
                        text = dateFormat.format(Date(sortedData.first().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * A component that displays chart for steps data
 */
@Composable
fun StepsChart(
    data: List<StepsData>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFFF9800) // Orange
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("No steps data available")
        return
    }

    // Sort data by timestamp
    val sortedData = remember(data) {
        data.sortedBy { it.timestamp }
    }

    // Find min and max values with padding
    val maxValue = remember(sortedData) {
        sortedData.maxOfOrNull { it.steps }?.plus(1000) ?: 10000
    }
    val minValue = remember(sortedData) {
        (sortedData.minOfOrNull { it.steps }?.minus(1000) ?: 0).coerceAtLeast(0)
    }
    val valueRange = remember(maxValue, minValue) {
        (maxValue - minValue).coerceAtLeast(1)
    }

    // Date formatter
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = maxValue.toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = ((maxValue + minValue) / 2).toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = minValue.toString(),
                color = Color.Gray,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Line chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, bottom = 20.dp, top = 8.dp, end = 8.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Draw horizontal grid lines
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (i in 0..2) {
                val y = canvasHeight - (canvasHeight / 2 * i)
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f,
                    pathEffect = pathEffect
                )
            }

            if (sortedData.size > 1) {
                // Draw lines connecting points
                for (i in 0 until sortedData.size - 1) {
                    val startX = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val startY = canvasHeight - ((sortedData[i].steps - minValue).toFloat() / valueRange * canvasHeight)

                    val endX = ((i + 1).toFloat() / (sortedData.size - 1)) * canvasWidth
                    val endY = canvasHeight - ((sortedData[i + 1].steps - minValue).toFloat() / valueRange * canvasHeight)

                    drawLine(
                        color = lineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Draw points
                for (i in sortedData.indices) {
                    val x = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val y = canvasHeight - ((sortedData[i].steps - minValue).toFloat() / valueRange * canvasHeight)

                    drawCircle(
                        color = lineColor,
                        radius = 5f,
                        center = Offset(x, y)
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            } else if (sortedData.size == 1) {
                // Draw a single point if there's only one data point
                val x = canvasWidth / 2
                val y = canvasHeight - ((sortedData[0].steps - minValue).toFloat() / valueRange * canvasHeight)

                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = Offset(x, y)
                )

                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }

        // Date axis labels (X-axis)
        if (sortedData.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sortedData.size > 1) {
                    // Show first, middle and last date
                    Text(
                        text = dateFormat.format(Date(sortedData.first().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    if (sortedData.size > 2) {
                        Text(
                            text = dateFormat.format(Date(sortedData[sortedData.size / 2].timestamp)),
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(sortedData.last().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                } else {
                    // Just the single date
                    Text(
                        text = dateFormat.format(Date(sortedData.first().timestamp)),
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Chart title
        Text(
            text = "Daily Steps",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
    }
}