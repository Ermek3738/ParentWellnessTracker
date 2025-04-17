package com.ermek.parentwellness.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ermek.parentwellness.ui.components.HeartLogo
import com.ermek.parentwellness.ui.components.PrimaryButton
import com.ermek.parentwellness.ui.components.SecondaryButton

@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HeartLogo()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Let's Get Started!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Let's dive in into your account",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        // Social login buttons
        SocialLoginButton(
            text = "Continue with Google",
            onClick = { /* Implement Google Sign In */ },
            icon = Icons.Default.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        SocialLoginButton(
            text = "Continue with Apple",
            onClick = { /* Implement Apple Sign In */ },
            icon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(16.dp))

        SocialLoginButton(
            text = "Continue with Facebook",
            onClick = { /* Implement Facebook Sign In */ },
            icon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(16.dp))

        SocialLoginButton(
            text = "Continue with X",
            onClick = { /* Implement X Sign In */ },
            icon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Sign Up",
            onClick = onNavigateToRegister
        )

        Spacer(modifier = Modifier.height(16.dp))

        SecondaryButton(
            text = "Sign In",
            onClick = onNavigateToLogin
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Text(
                text = " • ",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Text(
                text = "Terms of Service",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}