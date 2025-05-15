package com.ermek.parentwellness.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ermek.parentwellness.data.model.EmergencyContact
import com.ermek.parentwellness.ui.theme.PrimaryRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EmergencyContactViewModel = viewModel()
) {
    val contactsState by viewModel.contactsState.collectAsState()
    var showAddContactDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<EmergencyContact?>(null) }

    // Load contacts when the screen appears
    LaunchedEffect(Unit) {
        viewModel.loadEmergencyContacts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Contacts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = PrimaryRed
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Contact",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = contactsState) {
                is EmergencyContactState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryRed
                    )
                }

                is EmergencyContactState.Success -> {
                    if (state.contacts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ContactPhone,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No Emergency Contacts Yet",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Add emergency contacts to quickly reach out for help",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showAddContactDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryRed
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Contact")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = PrimaryRed.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = PrimaryRed
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(
                                            text = "In case of emergency, tap a contact to call them. Contacts are ordered by priority.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            items(state.contacts) { contact ->
                                EmergencyContactItem(
                                    contact = contact,
                                    onEditClick = {
                                        contactToEdit = contact
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteEmergencyContact(contact.id)
                                    },
                                    onCallClick = {
                                        viewModel.callEmergencyContact(contact)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                            }
                        }
                    }
                }

                is EmergencyContactState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading contacts: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.loadEmergencyContacts() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryRed
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Add or Edit Contact Dialog
    if (showAddContactDialog || contactToEdit != null) {
        EmergencyContactDialog(
            contact = contactToEdit,
            onDismiss = {
                showAddContactDialog = false
                contactToEdit = null
            },
            onSave = { name, phone, relationship, notifyOnEmergency ->
                if (contactToEdit != null) {
                    viewModel.updateEmergencyContact(
                        contactToEdit!!.copy(
                            name = name,
                            phoneNumber = phone,
                            relationship = relationship,
                            isNotified = notifyOnEmergency
                        )
                    )
                } else {
                    viewModel.addEmergencyContact(
                        name = name,
                        phoneNumber = phone,
                        relationship = relationship,
                        isNotified = notifyOnEmergency
                    )
                }
                showAddContactDialog = false
                contactToEdit = null
            }
        )
    }
}

@Composable
fun EmergencyContactItem(
    contact: EmergencyContact,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onCallClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact avatar or priority circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (contact.priority + 1).toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = contact.relationship,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Column {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.Gray
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, relationship: String, notifyOnEmergency: Boolean) -> Unit
) {
    val isEditMode = contact != null
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(contact?.phoneNumber ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    var notifyOnEmergency by remember { mutableStateOf(contact?.isNotified ?: true) }

    // Validate inputs
    val isNameValid = name.isNotBlank()
    val isPhoneValid = phoneNumber.isNotBlank() && phoneNumber.length >= 10

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Edit Emergency Contact" else "Add Emergency Contact") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    isError = !isNameValid && name.isNotEmpty(),
                    supportingText = {
                        if (!isNameValid && name.isNotEmpty()) {
                            Text("Name is required")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    isError = !isPhoneValid && phoneNumber.isNotEmpty(),
                    supportingText = {
                        if (!isPhoneValid && phoneNumber.isNotEmpty()) {
                            Text("Enter a valid phone number")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g., Spouse, Child)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = notifyOnEmergency,
                        onCheckedChange = { notifyOnEmergency = it }
                    )

                    Text(
                        text = "Notify this contact in emergencies",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, phoneNumber, relationship, notifyOnEmergency)
                },
                enabled = isNameValid && isPhoneValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryRed
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}