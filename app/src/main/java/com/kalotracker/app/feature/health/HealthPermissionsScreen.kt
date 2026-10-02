package com.kalotracker.app.feature.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.KaloButton

@Composable
fun HealthPermissionsScreen(
    onRequestPermissions: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = KaloBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(KaloSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = KaloTextPrimary
                    )
                }

                Text(
                    text = "HEALTH CONNECT",
                    style = KaloTypography.labelSmall,
                    color = KaloTextSecondary
                )

                Box(modifier = Modifier.size(48.dp))
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KaloButton(
                    text = "Grant Health Connect Access",
                    onClick = onRequestPermissions
                )

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip for Now",
                        style = KaloTypography.bodyLarge,
                        color = KaloTextSecondary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Sync Your Daily Movement",
                style = KaloTypography.headlineMedium,
                color = KaloTextPrimary
            )

            Text(
                text = "Kalo connects to Android Health Connect to read daily steps and active calorie expenditure directly from Samsung Health, Google Fit, Garmin, or your smartwatch.",
                style = KaloTypography.bodyLarge,
                color = KaloTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionFeatureCard(
                icon = Icons.Default.DirectionsWalk,
                title = "Daily Step Count",
                description = "Automatically updates your step count gauge to ensure you hit your daily movement goals."
            )

            PermissionFeatureCard(
                icon = Icons.Default.Favorite,
                title = "Active Calorie Burn",
                description = "Factors in energy burned from walking and running into your daily calorie balance equation."
            )

            PermissionFeatureCard(
                icon = Icons.Default.Security,
                title = "On-Device Privacy First",
                description = "All health data is processed entirely on your device and is never shared or sold."
            )
        }
    }
}

@Composable
fun PermissionFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(KaloSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KaloSteps,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = KaloTypography.titleMedium,
                color = KaloTextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = KaloTypography.bodyMedium,
                color = KaloTextSecondary
            )
        }
    }
}
