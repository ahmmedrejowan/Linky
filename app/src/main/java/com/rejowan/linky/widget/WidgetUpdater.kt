package com.rejowan.linky.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility object to trigger widget updates when link data changes
 */
object WidgetUpdater {

    /**
     * Trigger update for all Linky widgets
     * Call this after link operations (save, delete, update, restore)
     */
    fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LinkyWidget().updateAll(context)
            } catch (e: Exception) {
                // Widget might not be added, ignore errors
            }
        }
    }
}
