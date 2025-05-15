package com.ermek.parentwellness.ui.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.data.model.EmergencyContact
import com.ermek.parentwellness.ui.theme.PrimaryRed

@Composable
fun EmergencyButton(
    modifier: Modifier = Modifier,
    viewModel: EmergencyContactViewModel = viewModel()
) {
    val context = LocalContext.current
    val contactsState by viewModel.contactsState.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    // Animate pulse effect for emergency button
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Load emergency contacts when the button is expanded
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            viewModel.loadEmergencyContacts()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Emergency Button
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(PrimaryRed)
                .scale(if (isExpanded) 1f else scale)
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Call,
                contentDescription = "Emergency",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Emergency Contact List
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(300.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Emergency Title
                    Text(
                        text = "Emergency Contacts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryRed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (val state = contactsState) {
                        is EmergencyContactState.Loading -> {
                            CircularProgressIndicator(
                                color = PrimaryRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        is EmergencyContactState.Success -> {
                            if (state.contacts.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No emergency contacts",
                                        color = Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(
                                        onClick = {
                                            // Navigate to emergency contacts screen
                                            isExpanded = false
                                            // Call a navigation function here
                                        }
                                    ) {
                                        Text("Add Contacts")
                                    }
                                }
                            } else {
                                // Show the first 3 emergency contacts
                                state.contacts.take(3).forEach { contact ->
                                    EmergencyContactQuickItem(
                                        contact = contact,
                                        onCallClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${contact.phoneNumber}")
                                            }
                                            context.startActivity(dialIntent)
                                            isExpanded = false
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Call emergency services button
                                Button(
                                    onClick = {
                                        val emergencyIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:911") // Use appropriate emergency number
                                        }
                                        context.startActivity(emergencyIntent)
                                        isExpanded = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryRed
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Call Emergency Services (911)")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(
                                    onClick = {
                                        // Navigate to emergency contacts screen
                                        isExpanded = false
                                        // Call a navigation function here
                                    }
                                ) {
                                    Text("Manage Contacts")
                                }
                            }
                        }

                        is EmergencyContactState.Error -> {
                            Text(
                                text = "Error loading contacts",
                                color = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyContactQuickItem(
    contact: EmergencyContact,
    onCallClick: () -> Unit
) {
    Button(
        onClick = onCallClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = PrimaryRed
        ),
        border = BorderStroke(1.dp, PrimaryRed),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = PrimaryRed
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = contact.relationship,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}