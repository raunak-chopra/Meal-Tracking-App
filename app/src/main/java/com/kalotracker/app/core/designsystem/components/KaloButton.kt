package com.kalotracker.app.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*

@Composable
fun KaloButton(text: String,onClick: () -> Unit,modifier: Modifier=Modifier,enabled: Boolean=true,
    loading: Boolean=false,containerColor: Color=KaloAccent,contentColor: Color=KaloOnAccent) {
    Button(onClick=onClick,enabled=enabled && !loading,shape=RoundedCornerShape(16.dp),
        colors=ButtonDefaults.buttonColors(containerColor=containerColor,contentColor=contentColor),
        modifier=modifier.fillMaxWidth().heightIn(min=56.dp),contentPadding=PaddingValues(16.dp)) {
        if(loading) { CircularProgressIndicator(Modifier.size(20.dp),color=contentColor,strokeWidth=2.dp); Spacer(Modifier.width(12.dp)) }
        Text(if(loading) "Saving…" else text,style=KaloTypography.labelLarge)
    }
}
