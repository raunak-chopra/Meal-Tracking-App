package com.kalotracker.app.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object KaloWidgetUpdater {
    fun update(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                KaloWidget().updateAll(context.applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
