package com.ermek.parentwellness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ermek.parentwellness.data.model.HealthData
import com.ermek.parentwellness.ui.theme.PrimaryRed
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple line chart component for visualizing health data.
 *
 * @param data List of health data points to display
 * @param modifier Modifier for the chart
 * @param lineColor Color of the chart line
 * @param labelColor Color for the axis labels
 * @param showPoints Whether to show points on the line
 */
@Composable
fun SimpleLineChart(
    data: List<HealthData>,
    modifier: Modifier = Modifier,
    lineColor: Color = PrimaryRed,
    labelColor: Color = Color.Gray,
    showPoints: Boolean = true
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available to display",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    // Sort data by timestamp (oldest to newest)
    val sortedData = data.sortedBy { it.timestamp }

    // Find min and max values
    val maxValue = sortedData.maxByOrNull { it.primaryValue }?.primaryValue?.toInt() ?: 0
    val minValue = sortedData.minByOrNull { it.primaryValue }?.primaryValue?.toInt() ?: 0
    val valueRange = (maxValue - minValue).coerceAtLeast(1).toFloat()

    // Format for date labels
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        // Value axis labels (Y-axis)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = maxValue.toString(),
                color = labelColor,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = ((maxValue + minValue) / 2).toString(),
                color = labelColor,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = minValue.toString(),
                color = labelColor,
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

            // Only draw the chart if we have at least 2 data points
            if (sortedData.size > 1) {
                // Draw the line connecting all points
                for (i in 0 until sortedData.size - 1) {
                    val startX = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                    val startValue = sortedData[i].primaryValue.toFloat()
                    val startY = canvasHeight - ((startValue - minValue) / valueRange * canvasHeight)

                    val endX = ((i + 1).toFloat() / (sortedData.size - 1)) * canvasWidth
                    val endValue = sortedData[i + 1].primaryValue.toFloat()
                    val endY = canvasHeight - ((endValue - minValue) / valueRange * canvasHeight)

                    drawLine(
                        color = lineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Draw points on the line
                if (showPoints) {
                    for (i in sortedData.indices) {
                        val x = (i.toFloat() / (sortedData.size - 1)) * canvasWidth
                        val value = sortedData[i].primaryValue.toFloat()
                        val y = canvasHeight - ((value - minValue) / valueRange * canvasHeight)

                        // Draw point circle
                        drawCircle(
                            color = lineColor,
                            radius = 5f,
                            center = Offset(x, y)
                        )

                        // Draw white inner circle to create a ring effect
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(x, y)
                        )
                    }
                }
            } else {
                // Draw a single point if there's only one data point
                val x = canvasWidth / 2
                val value = sortedData[0].primaryValue.toFloat()
                val y = canvasHeight - ((value - minValue) / valueRange * canvasHeight)

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
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 40.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (sortedData.size > 1) {
                // Show first, middle and last date if we have enough data points
                Text(
                    text = dateFormat.format(Date(sortedData.first().timestamp)),
                    color = labelColor,
                    fontSize = 10.sp
                )

                if (sortedData.size > 2) {
                    Text(
                        text = dateFormat.format(Date(sortedData[sortedData.size / 2].timestamp)),
                        color = labelColor,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = dateFormat.format(Date(sortedData.last().timestamp)),
                    color = labelColor,
                    fontSize = 10.sp
                )
            } else {
                // Just show the single date if only one data point
                Text(
                    text = dateFormat.format(Date(sortedData.first().timestamp)),
                    color = labelColor,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A dual line chart component for visualizing blood pressure data with
 * systolic and diastolic values.
 */
@Composable
fun DualLineChart(
    data: List<HealthData>,
    modifier: Modifier = Modifier,
    primaryLineColor: Color = Color.Red,
    secondaryLineColor: Color = Color.Blue,
    labelColor: Color = Color.Gray
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available to display",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    // Ensure we have secondary values and sort data by timestamp
    val validData = data.filter { it.secondaryValue != null }.sortedBy { it.timestamp }

    if (validData.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No blood pressure data available",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    // Find min and max values for both lines
    val maxPrimary = validData.maxByOrNull { it.primaryValue }?.primaryValue?.toInt() ?: 0
    val minPrimary = validData.minByOrNull { it.primaryValue }?.primaryValue?.toInt() ?: 0
    val maxSecondary = validData.maxByOrNull { it.secondaryValue ?: 0.0 }?.secondaryValue?.toInt() ?: 0
    val minSecondary = validData.minByOrNull { it.secondaryValue ?: 0.0 }?.secondaryValue?.toInt() ?: 0

    // Use combined range for better visualization
    val overallMax = maxOf(maxPrimary, maxSecondary)
    val overallMin = minOf(minPrimary, minSecondary)
    val valueRange = (overallMax - overallMin).coerceAtLeast(1).toFloat()

    // Format for date labels
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        // Value axis labels (Y-axis)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = overallMax.toString(),
                color = labelColor,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = ((overallMax + overallMin) / 2).toString(),
                color = labelColor,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = overallMin.toString(),
                color = labelColor,
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

            // Only draw the chart if we have at least 2 data points
            if (validData.size > 1) {
                // Draw the primary line (systolic)
                for (i in 0 until validData.size - 1) {
                    val startX = (i.toFloat() / (validData.size - 1)) * canvasWidth
                    val startValue = validData[i].primaryValue.toFloat()
                    val startY = canvasHeight - ((startValue - overallMin) / valueRange * canvasHeight)

                    val endX = ((i + 1).toFloat() / (validData.size - 1)) * canvasWidth
                    val endValue = validData[i + 1].primaryValue.toFloat()
                    val endY = canvasHeight - ((endValue - overallMin) / valueRange * canvasHeight)

                    drawLine(
                        color = primaryLineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Draw the secondary line (diastolic)
                for (i in 0 until validData.size - 1) {
                    val startX = (i.toFloat() / (validData.size - 1)) * canvasWidth
                    val startValue = validData[i].secondaryValue?.toFloat() ?: 0f
                    val startY = canvasHeight - ((startValue - overallMin) / valueRange * canvasHeight)

                    val endX = ((i + 1).toFloat() / (validData.size - 1)) * canvasWidth
                    val endValue = validData[i + 1].secondaryValue?.toFloat() ?: 0f
                    val endY = canvasHeight - ((endValue - overallMin) / valueRange * canvasHeight)

                    drawLine(
                        color = secondaryLineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }

                // Draw points on both lines
                for (i in validData.indices) {
                    val x = (i.toFloat() / (validData.size - 1)) * canvasWidth

                    // Primary value point (systolic)
                    val primaryValue = validData[i].primaryValue.toFloat()
                    val primaryY = canvasHeight - ((primaryValue - overallMin) / valueRange * canvasHeight)

                    drawCircle(
                        color = primaryLineColor,
                        radius = 5f,
                        center = Offset(x, primaryY)
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, primaryY)
                    )

                    // Secondary value point (diastolic)
                    val secondaryValue = validData[i].secondaryValue?.toFloat() ?: 0f
                    val secondaryY = canvasHeight - ((secondaryValue - overallMin) / valueRange * canvasHeight)

                    drawCircle(
                        color = secondaryLineColor,
                        radius = 5f,
                        center = Offset(x, secondaryY)
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(x, secondaryY)
                    )
                }
            } else {
                // Draw single points if only one data point
                val x = canvasWidth / 2

                // Primary value point
                val primaryValue = validData[0].primaryValue.toFloat()
                val primaryY = canvasHeight - ((primaryValue - overallMin) / valueRange * canvasHeight)

                drawCircle(
                    color = primaryLineColor,
                    radius = 5f,
                    center = Offset(x, primaryY)
                )

                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, primaryY)
                )

                // Secondary value point
                val secondaryValue = validData[0].secondaryValue?.toFloat() ?: 0f
                val secondaryY = canvasHeight - ((secondaryValue - overallMin) / valueRange * canvasHeight)

                drawCircle(
                    color = secondaryLineColor,
                    radius = 5f,
                    center = Offset(x, secondaryY)
                )

                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, secondaryY)
                )
            }
        }

        // Date axis labels (X-axis)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 40.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (validData.size > 1) {
                // Show first, middle and last date if we have enough data points
                Text(
                    text = dateFormat.format(Date(validData.first().timestamp)),
                    color = labelColor,
                    fontSize = 10.sp
                )

                if (validData.size > 2) {
                    Text(
                        text = dateFormat.format(Date(validData[validData.size / 2].timestamp)),
                        color = labelColor,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = dateFormat.format(Date(validData.last().timestamp)),
                    color = labelColor,
                    fontSize = 10.sp
                )
            } else {
                // Just show the single date if only one data point
                Text(
                    text = dateFormat.format(Date(validData.first().timestamp)),
                    color = labelColor,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Primary line legend
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(primaryLineColor, RoundedCornerShape(4.dp))
            )

            Text(
                text = "Systolic",
                color = labelColor,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Secondary line legend
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(secondaryLineColor, RoundedCornerShape(4.dp))
            )

            Text(
                text = "Diastolic",
                color = labelColor,
                fontSize = 10.sp
            )
        }
    }
}