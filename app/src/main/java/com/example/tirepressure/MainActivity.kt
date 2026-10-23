package com.example.tirepressure

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView

/** หน้าทดสอบ: แสดงค่ายางสด + รายการ property ทั้งหมดของรถ (ใช้ยืนยันว่ารถส่งค่ายางมาไหม) */
class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var tv: TextView
    private val tick = object : Runnable {
        override fun run() { render(); h.postDelayed(this, 2000) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        tv = TextView(this).apply { textSize = 16f; typeface = Typeface.MONOSPACE; setPadding(24, 24, 24, 24) }
        setContentView(ScrollView(this).apply {
            addView(tv, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
        val perm = "android.car.permission.CAR_TIRES"
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(perm), 1)
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    private fun render() {
        val sb = StringBuilder()
        val names = listOf("หน้าซ้าย", "หน้าขวา", "หลังซ้าย", "หลังขวา")
        sb.append("== ค่ายาง (TIRE_PRESSURE 392168201) ==\n")
        CarProps.tires(this).forEachIndexed { i, kpa ->
            sb.append(names[i]).append(": ")
            sb.append(if (kpa == null) "อ่านไม่ได้" else "%.0f kPa / %.1f psi".format(kpa, CarProps.kpaToPsi(kpa))).append('\n')
        }
        sb.append("\nข้อผิดพลาดล่าสุด: ").append(CarProps.lastError ?: "-").append('\n')
        val props = CarProps.listProps(this)
        sb.append("\n== property ที่เกี่ยวกับยาง ==\n")
        val tire = props.filter { it.second.contains("TIRE", true) || it.second.contains("TPMS", true) }
        if (tire.isEmpty()) sb.append("(ไม่พบ)\n")
        tire.forEach { sb.append("%s  id=%d areas=%s\n".format(it.second, it.first, it.third.joinToString(","))) }
        sb.append("\n== property ทั้งหมด (${props.size}) ==\n")
        props.forEach { sb.append("%s  id=%d\n".format(it.second, it.first)) }
        tv.text = sb
    }
}
