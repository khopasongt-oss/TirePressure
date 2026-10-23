package com.example.tirepressure

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews

/** โพลค่ายางทุก 3 วินาทีแล้วอัปเดตวิดเจ็ต หยุดเองเมื่อไม่มีวิดเจ็ตเหลือ */
class TirePollService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private val ids = intArrayOf(R.id.fl, R.id.fr, R.id.rl, R.id.rr)
    private val loop = object : Runnable {
        override fun run() {
            val awm = AppWidgetManager.getInstance(this@TirePollService)
            val cn = ComponentName(this@TirePollService, TireWidgetProvider::class.java)
            val widgets = awm.getAppWidgetIds(cn)
            if (widgets.isEmpty()) { stopSelf(); return }
            val rv = RemoteViews(packageName, R.layout.widget_tire)
            CarProps.tires(this@TirePollService).forEachIndexed { i, kpa ->
                if (kpa == null) {
                    rv.setTextViewText(ids[i], "--"); rv.setTextColor(ids[i], 0xFF9AA4AF.toInt())
                } else {
                    rv.setTextViewText(ids[i], "%.0f".format(kpa) + "\n" + "%.1f psi".format(CarProps.kpaToPsi(kpa)))
                    val c = when {
                        kpa < CarProps.LOW_KPA -> 0xFFFF5252.toInt()   // ต่ำ = แดง
                        kpa > CarProps.HIGH_KPA -> 0xFFFFB300.toInt()  // สูง = เหลือง
                        else -> 0xFF4CAF50.toInt()                     // ปกติ = เขียว
                    }
                    rv.setTextColor(ids[i], c)
                }
            }
            awm.updateAppWidget(widgets, rv)
            h.postDelayed(this, 3000)
        }
    }
    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int { h.removeCallbacks(loop); h.post(loop); return START_STICKY }
    override fun onDestroy() { h.removeCallbacks(loop); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
