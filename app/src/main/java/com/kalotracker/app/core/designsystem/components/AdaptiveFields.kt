package com.kalotracker.app.core.designsystem.components
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
@Composable
fun AdaptiveFields(first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if(LocalDensity.current.fontScale >= 1.3f || maxWidth < 340.dp)
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { first(Modifier.fillMaxWidth()); second(Modifier.fillMaxWidth()) }
        else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { first(Modifier.weight(1f)); second(Modifier.weight(1f)) }
    }
}
