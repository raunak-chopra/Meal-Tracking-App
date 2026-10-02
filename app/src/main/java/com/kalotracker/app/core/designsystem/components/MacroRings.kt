package com.kalotracker.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*

@Composable
fun BrandMark(modifier: Modifier=Modifier.size(32.dp)) {
    val text=KaloTextPrimary; val accent=KaloAccent
    val rim = remember { PathParser().parsePathString("M78 68 A34 34 0 1 1 66 20").toPath() }
    val portion = remember { PathParser().parsePathString("M81 30 A34 34 0 0 1 85 49").toPath() }
    Canvas(modifier) {
        scale(size.width/100f, size.height/100f, pivot=Offset.Zero) {
            drawPath(rim,text,style=Stroke(11f,cap=StrokeCap.Round))
            drawPath(portion,accent,style=Stroke(11f,cap=StrokeCap.Round))
        }
    }
}

@Composable
fun MacroSummaryCard(currentCalories:Int,targetCalories:Int,proteinGrams:Int,targetProtein:Int,
    carbsGrams:Int,targetCarbs:Int,fatGrams:Int,targetFat:Int,modifier:Modifier=Modifier) {
    val energy=EnergyDisplay(currentCalories,targetCalories)
    val large=LocalDensity.current.fontScale>=1.3f
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth().background(KaloSurface,RoundedCornerShape(20.dp)).padding(20.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).semantics(mergeDescendants=true){}) {
                Text("Energy logged",style=KaloTypography.bodyMedium,color=KaloTextSecondary)
                Text("$currentCalories kcal",style=KaloTypography.displayLarge,color=KaloTextPrimary)
                Text(if(targetCalories>0) "of $targetCalories target" else "Historical target unavailable",color=KaloTextSecondary)
                Spacer(Modifier.height(12.dp))
                Text(energy.deltaText,style=KaloTypography.bodyLarge,color=KaloAccent)
            }
            if(!large) DailyArc(energy.progress,Modifier.size(88.dp))
        }
        BoxWithConstraints {
            if(large || maxWidth<340.dp) Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                MacroPill("Protein",proteinGrams,targetProtein,KaloProtein)
                MacroPill("Carbs",carbsGrams,targetCarbs,KaloCarbs)
                MacroPill("Fat",fatGrams,targetFat,KaloFat)
            } else Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                MacroPill("Protein",proteinGrams,targetProtein,KaloProtein,Modifier.weight(1f))
                MacroPill("Carbs",carbsGrams,targetCarbs,KaloCarbs,Modifier.weight(1f))
                MacroPill("Fat",fatGrams,targetFat,KaloFat,Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DailyArc(progress:Float,modifier:Modifier) {
    val accent=KaloAccent;val track=KaloDivider;val mark=KaloTextPrimary
    Canvas(modifier.clearAndSetSemantics{}) {
        val inset=5.dp.toPx();val area=Size(size.width-inset*2,size.height-inset*2)
        val stroke=Stroke(7.dp.toPx(),cap=StrokeCap.Round)
        drawArc(track,150f,240f,false,Offset(inset,inset),area,style=stroke)
        if(progress>0f) drawArc(accent,150f,240f*progress,false,Offset(inset,inset),area,style=stroke)
        val small=size.minDimension*0.3f
        drawArc(mark,20f,285f,false,Offset((size.width-small)/2,(size.height-small)/2),Size(small,small),style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
    }
}

@Composable
fun MacroPill(label:String,current:Int,target:Int,color:Color,modifier:Modifier=Modifier) {
    Column(modifier.semantics(mergeDescendants=true){},verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(label,style=KaloTypography.bodyMedium,color=KaloTextSecondary)
        Text("${current} g",style=KaloTypography.titleMedium,color=KaloTextPrimary)
        LinearProgressIndicator(progress={ if(target>0) (current.toFloat()/target).coerceIn(0f,1f) else 0f },
            modifier=Modifier.fillMaxWidth().height(4.dp),color=color,trackColor=KaloDivider,strokeCap=StrokeCap.Round)
        Text(if(target>0) "of $target g" else "Target unavailable",style=KaloTypography.labelSmall,color=KaloTextSecondary)
    }
}
