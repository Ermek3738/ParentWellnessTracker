package com.ermek.parentwellness.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.ui.components.HeartLogo
import com.ermek.parentwellness.ui.components.PrimaryButton
import com.ermek.parentwellness.ui.theme.PrimaryRed

@Composable
fun SetupWelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HeartLogo()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Parent Wellness! 👋",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Before we begin, please provide us with some personal information to ensure accurate monitoring",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        PrimaryButton(
            text = "OK, Let's Start",
            onClick = onContinue
        )
    }
}

@Composable
fun SetupNameScreen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val userProfile by viewModel.user.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val totalSteps by viewModel.totalSteps.collectAsState()

    var name by remember { mutableStateOf(userProfile.fullName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with back button and progress indicator
        SetupHeader(
            currentStep = currentStep,
            totalSteps = totalSteps,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "What's your name?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = null,
            placeholder = { Text("Enter your name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryRed,
                cursorColor = PrimaryRed
            ),
            textStyle = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "Continue",
            onClick = {
                viewModel.updateProfile { it.copy(fullName = name) }
                onContinue()
            },
            enabled = name.isNotBlank()
        )
    }
}

@Composable
fun SetupGenderScreen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val userProfile by viewModel.user.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val totalSteps by viewModel.totalSteps.collectAsState()

    var selectedGender by remember { mutableStateOf(userProfile.gender.ifEmpty { "" }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with back button and progress indicator
        SetupHeader(
            currentStep = currentStep,
            totalSteps = totalSteps,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "What's your gender?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GenderOption(
                gender = "Male",
                isSelected = selectedGender == "Male",
                onClick = { selectedGender = "Male" },
                modifier = Modifier.weight(1f)
            )

            GenderOption(
                gender = "Female",
                isSelected = selectedGender == "Female",
                onClick = { selectedGender = "Female" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { selectedGender = "Prefer not to say" },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selectedGender == "Prefer not to say") PrimaryRed.copy(alpha = 0.1f) else Color.Transparent,
                contentColor = if (selectedGender == "Prefer not to say") PrimaryRed else Color.Gray
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (selectedGender == "Prefer not to say") PrimaryRed else Color.Gray
            )
        ) {
            Text(
                text = "Prefer not to say",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "Continue",
            onClick = {
                viewModel.updateProfile { it.copy(gender = selectedGender) }
                onContinue()
            },
            enabled = selectedGender.isNotBlank()
        )
    }
}

@Composable
fun GenderOption(
    gender: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) PrimaryRed.copy(alpha = 0.1f) else Color.Transparent
    val borderColor = if (isSelected) PrimaryRed else Color.Gray
    val contentColor = if (isSelected) PrimaryRed else Color.Gray

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = backgroundColor,
                contentColor = contentColor
            ),
            border = BorderStroke(
                width = 1.dp,
                color = borderColor
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Use appropriate gender icons here
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = gender,
                    tint = contentColor,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = gender,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun SetupBirthdayScreen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val userProfile by viewModel.user.collectAsState() // Changed from userProfile to user
    val currentStep by viewModel.currentStep.collectAsState()
    val totalSteps by viewModel.totalSteps.collectAsState()

    // For date selection
    var selectedMonth by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("") }

    // Parse existing birthdate if available
    LaunchedEffect(userProfile.birthDate) {
        if (userProfile.birthDate.isNotEmpty()) {
            val parts = userProfile.birthDate.split("-")
            if (parts.size == 3) {
                selectedMonth = parts[0]
                selectedDay = parts[1]
                selectedYear = parts[2]
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with back button and progress indicator
        SetupHeader(
            currentStep = currentStep,
            totalSteps = totalSteps,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "When is your birthday?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Date input fields - horizontal row with 3 sections
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month input
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Month",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = {
                        // Only allow numbers and limit to 2 digits
                        if (it.all { char -> char.isDigit() } && it.length <= 2) {
                            selectedMonth = it
                        }
                    },
                    placeholder = { Text("MM") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(  // Fixed to OutlinedTextFieldDefaults.colors
                        focusedBorderColor = PrimaryRed,
                        cursorColor = PrimaryRed
                    )
                )
            }

            // Day input
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Day",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = selectedDay,
                    onValueChange = {
                        // Only allow numbers and limit to 2 digits
                        if (it.all { char -> char.isDigit() } && it.length <= 2) {
                            selectedDay = it
                        }
                    },
                    placeholder = { Text("DD") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(  // Fixed to OutlinedTextFieldDefaults.colors
                        focusedBorderColor = PrimaryRed,
                        cursorColor = PrimaryRed
                    )
                )
            }

            // Year input
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Year",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = selectedYear,
                    onValueChange = {
                        // Only allow numbers and limit to 4 digits
                        if (it.all { char -> char.isDigit() } && it.length <= 4) {
                            selectedYear = it
                        }
                    },
                    placeholder = { Text("YYYY") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(  // Fixed to OutlinedTextFieldDefaults.colors
                        focusedBorderColor = PrimaryRed,
                        cursorColor = PrimaryRed
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Continue button
        Button(
            onClick = {
                // Format as MM-DD-YYYY
                val birthDate = "$selectedMonth-$selectedDay-$selectedYear"
                viewModel.updateProfile { it.copy(birthDate = birthDate) }
                onContinue()
            },
            enabled = selectedMonth.isNotEmpty() && selectedDay.isNotEmpty() && selectedYear.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryRed,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun SetupHeader(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        // Progress indicator
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .weight(1f)
                .height(4.dp),
            color = PrimaryRed,
            trackColor = Color.LightGray
        )

        Text(
            text = "$currentStep/$totalSteps",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}