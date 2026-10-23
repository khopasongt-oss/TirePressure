package com.example.tirepressure

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class TireWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) { c.startService(Intent(c, TirePollService::class.java)) }
    override fun onEnabled(c: Context) { c.startService(Intent(c, TirePollService::class.java)) }
}
