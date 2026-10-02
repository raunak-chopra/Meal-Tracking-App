package com.kalotracker.app.core.designsystem
import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.graphics.Color
import com.kalotracker.app.core.designsystem.components.EnergyDisplay
import com.kalotracker.app.core.util.LogDate
import java.time.*
class BrandBehaviorTest {
    @Test fun aboveTargetKeepsLoggedAmountAndShowsTrueDelta() {
        val display = EnergyDisplay(2800,2200)
        assertEquals(2800,display.logged)
        assertEquals("600 kcal above target",display.deltaText)
        assertEquals(1f,display.progress,0f)
    }
    @Test fun missingHistoricalTargetDoesNotImplyBudget() {
        assertEquals("Target unavailable",EnergyDisplay(1200,0).deltaText)
        assertEquals(0f,EnergyDisplay(1200,0).progress,0f)
    }
    @Test fun exactTargetAndBelowTargetAreFactual() {
        assertEquals("At your target",EnergyDisplay(2200,2200).deltaText)
        assertEquals("400 kcal to target",EnergyDisplay(1800,2200).deltaText)
    }
    @Test fun pastDayDefaultsToSelectedLocalDay() {
        val zone=ZoneId.of("Asia/Kolkata")
        val now=Instant.parse("2026-10-02T18:35:00Z").toEpochMilli()
        val date=LocalDate.of(2026,10,1)
        assertEquals(date,Instant.ofEpochMilli(LogDate.timestamp(date,now,zone)).atZone(zone).toLocalDate())
        val today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        assertEquals(now,LogDate.timestamp(today,now,zone))
    }
    @Test fun textAndActionColoursMeetNormalTextContrastInBothModes() {
        for(p in listOf(WarmDark,WarmLight)) {
            for(bg in listOf(p.background,p.surface,p.raised)) {
                for(text in listOf(p.text,p.secondaryText,p.protein,p.carbs,p.fat))
                    assertTrue("Contrast ${contrast(text,bg)}",contrast(text,bg)>=4.5)
            }
            assertTrue(contrast(p.onAccent,p.accent)>=4.5)
        }
    }
    private fun contrast(a:Color,b:Color):Double {
        fun luminance(c:Color):Double {
            fun channel(x:Float)=if(x<=0.04045) x/12.92 else Math.pow((x+0.055)/1.055,2.4)
            return 0.2126*channel(c.red)+0.7152*channel(c.green)+0.0722*channel(c.blue)
        }
        val x=luminance(a);val y=luminance(b)
        return (maxOf(x,y)+0.05)/(minOf(x,y)+0.05)
    }
}
