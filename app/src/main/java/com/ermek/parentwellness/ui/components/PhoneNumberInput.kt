package com.ermek.parentwellness.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.ui.theme.PrimaryRed

data class CountryCode(
    val code: String,
    val name: String,
    val regex: String,
    val maxLength: Int
)

@Composable
fun PhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCountryCode by remember { mutableStateOf(CountryCode("+996", "Kyrgyzstan", "^\\d{9}$", 9)) }
    var isError by remember { mutableStateOf(false) }

    // List of common country codes with validation patterns
    val countryCodes = remember {
        listOf(
            CountryCode("+996", "Kyrgyzstan", "^\\d{9}$", 9),
            CountryCode("+7", "Kazakhstan/Russia", "^\\d{10}$", 10),
            CountryCode("+1", "USA/Canada", "^\\d{10}$", 10),
            CountryCode("+44", "UK", "^\\d{10}$", 10),
            CountryCode("+49", "Germany", "^\\d{10,11}$", 11),
            CountryCode("+33", "France", "^\\d{9}$", 9),
            CountryCode("+86", "China", "^\\d{11}$", 11),
            CountryCode("+91", "India", "^\\d{10}$", 10),
            CountryCode("+81", "Japan", "^\\d{10}$", 10),
            CountryCode("+82", "South Korea", "^\\d{9,10}$", 10),
            CountryCode("+61", "Australia", "^\\d{9}$", 9),
            CountryCode("+55", "Brazil", "^\\d{10,11}$", 11),
            CountryCode("+52", "Mexico", "^\\d{10}$", 10),
            CountryCode("+234", "Nigeria", "^\\d{10}$", 10),
            CountryCode("+27", "South Africa", "^\\d{9}$", 9),
            // Add more country codes as needed
        )
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country code selector
            OutlinedTextField(
                value = selectedCountryCode.code,
                onValueChange = { /* Read-only field */ },
                modifier = Modifier.width(110.dp),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select country code"
                        )
                    }
                },
                label = { Text("Code") }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                countryCodes.forEach { countryCode ->
                    DropdownMenuItem(
                        text = { Text("${countryCode.code} (${countryCode.name})") },
                        onClick = {
                            selectedCountryCode = countryCode
                            // Validate with new country code
                            isError = phoneNumber.isNotEmpty() && !isPhoneNumberValid(phoneNumber, selectedCountryCode)
                            expanded = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Phone number input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    // Only allow digits and limit to the maximum length for the selected country
                    if (newValue.all { char -> char.isDigit() } && newValue.length <= selectedCountryCode.maxLength) {
                        onPhoneNumberChange(newValue)
                        isError = newValue.isNotEmpty() && !isPhoneNumberValid(newValue, selectedCountryCode)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = PrimaryRed
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                isError = isError
            )
        }

        if (isError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Invalid phone number format for ${selectedCountryCode.name}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Function to validate phone number format based on country code
private fun isPhoneNumberValid(phoneNumber: String, countryCode: CountryCode): Boolean {
    return phoneNumber.matches(Regex(countryCode.regex))
}