package com.kalotracker.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

const val TabularFontFeature = "tnum"
private fun type(size: Int,line: Int,weight: FontWeight=FontWeight.Normal,metric: Boolean=false) = TextStyle(
    fontFamily=FontFamily.Default,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight,
    fontFeatureSettings=if(metric) TabularFontFeature else null)
val KaloTypography = Typography(
    displayLarge=type(40,44,FontWeight.SemiBold,true),displayMedium=type(32,38,FontWeight.SemiBold,true),
    headlineMedium=type(28,34,FontWeight.SemiBold),titleLarge=type(22,28,FontWeight.SemiBold),
    titleMedium=type(18,24,FontWeight.SemiBold),bodyLarge=type(16,24),bodyMedium=type(14,20),
    labelLarge=type(16,20,FontWeight.SemiBold),labelMedium=type(14,20,FontWeight.Medium),labelSmall=type(12,16,FontWeight.Medium))
