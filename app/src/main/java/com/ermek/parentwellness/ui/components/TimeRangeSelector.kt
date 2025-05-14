package com.ermek.parentwellness.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.ui.health.TimeRange
import com.ermek.parentwellness.ui.theme.PrimaryRed

@Composable
fun TimeRangeSelector(
    onRangeSelected: (TimeRange) -> Unit,
    initialSelection: TimeRange = TimeRange.WEEK
) {
    var selectedRange by remember { mutableStateOf(initialSelection) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TimeRangeButton(
            text = "Week",
            isSelected = selectedRange == TimeRange.WEEK,
            onClick = {
                selectedRange = TimeRange.WEEK
                onRangeSelected(TimeRange.WEEK)
            }
        )

        TimeRangeButton(
            text = "Month",
            isSelected = selectedRange == TimeRange.MONTH,
            onClick = {
                selectedRange = TimeRange.MONTH
                onRangeSelected(TimeRange.MONTH)
            }
        )

        TimeRangeButton(
            text = "Year",
            isSelected = selectedRange == TimeRange.YEAR,
            onClick = {
                selectedRange = TimeRange.YEAR
                onRangeSelected(TimeRange.YEAR)
            }
        )
    }
}

@Composable
private fun TimeRangeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isSelected) PrimaryRed else Color.Gray
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
            else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}