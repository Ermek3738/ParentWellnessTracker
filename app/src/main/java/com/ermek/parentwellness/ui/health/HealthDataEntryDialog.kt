package com.ermek.parentwellness.ui.health

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ermek.parentwellness.ui.components.HealthDataEntry
import com.ermek.parentwellness.ui.components.HealthDataEntryForm
import com.ermek.parentwellness.ui.components.MetricType

/**
 * Dialog for entering health data manually
 */
@Composable
fun HealthDataEntryDialog(
    metricType: MetricType,
    onDismiss: () -> Unit,
    onSubmit: (HealthDataEntry) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.Transparent
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HealthDataEntryForm(
                        metricType = metricType,
                        onSubmit = { entry ->
                            onSubmit(entry)
                            onDismiss()
                        },
                        onCancel = onDismiss
                    )
                }
            }
        }
    }
}