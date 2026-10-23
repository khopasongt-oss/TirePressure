package com.example.tirepressure

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder

/** อ่านค่าจากรถผ่าน reflection เพื่อ build ได้ด้วย Android SDK ปกติ (ไม่ต้องใช้ car-lib) */
object CarProps {
    const val TIRE_PRESSURE = 392168201            // VehicleProperty.TIRE_PRESSURE (หน่วย kPa)
    val WHEELS = intArrayOf(1, 2, 4, 8)            // หน้าซ้าย, หน้าขวา, หลังซ้าย, หลังขวา

    // เกณฑ์เตือน (kPa) ปรับได้
    const val LOW_KPA = 200f   // ~29 psi
    const val HIGH_KPA = 300f  // ~43.5 psi

    @Volatile var lastError: String? = null
    @Volatile private var mgr: Any? = null
    @Volatile private var connecting = false
    private var carObj: Any? = null

    private fun manager(ctx: Context): Any? {
        mgr?.let { return it }
        val app = ctx.applicationContext
        try {
            val carCls = Class.forName("android.car.Car")
            // วิธีใหม่ (Android 10 ขึ้นไป)
            try {
                val car = carCls.getMethod("createCar", Context::class.java).invoke(null, app)
                mgr = carCls.getMethod("getCarManager", String::class.java).invoke(car, "property")
                if (mgr != null) return mgr
            } catch (e: NoSuchMethodException) {
                // เครื่องเก่า (Android 9) ลองวิธีถัดไป
            }
            // วิธีเก่า (Android 9): เชื่อมต่อแบบ async แล้วคืนค่าในรอบโพลถัดไป (ห้ามบล็อก main thread)
            if (!connecting) {
                connecting = true
                val conn = object : ServiceConnection {
                    override fun onServiceConnected(n: ComponentName?, s: IBinder?) {
                        try {
                            mgr = carCls.getMethod("getCarManager", String::class.java).invoke(carObj, "property")
                            if (mgr == null) lastError = "getCarManager(property) = null"
                        } catch (t: Throwable) { lastError = t.toString() }
                    }
                    override fun onServiceDisconnected(n: ComponentName?) { mgr = null; connecting = false }
                }
                carObj = carCls.getMethod("createCar", Context::class.java, ServiceConnection::class.java)
                    .invoke(null, app, conn)
                carCls.getMethod("connect").invoke(carObj)
            }
            if (mgr == null && lastError == null) lastError = "กำลังเชื่อมต่อ Car service..."
        } catch (t: Throwable) {
            lastError = t.toString(); connecting = false
        }
        return mgr
    }

    /** รายการ property ทั้งหมดที่รถเปิดให้แอปเห็น: (id, ชื่อ, area ids) */
    fun listProps(ctx: Context): List<Triple<Int, String, IntArray>> {
        val m = manager(ctx) ?: return emptyList()
        return try {
            val list = m.javaClass.getMethod("getPropertyList").invoke(m) as List<*>
            list.mapNotNull { cfg ->
                cfg ?: return@mapNotNull null
                val id = cfg.javaClass.getMethod("getPropertyId").invoke(cfg) as Int
                val areas = cfg.javaClass.getMethod("getAreaIds").invoke(cfg) as IntArray
                Triple(id, nameOf(id), areas)
            }.sortedBy { it.first }
        } catch (t: Throwable) { lastError = t.toString(); emptyList() }
    }

    fun nameOf(id: Int): String = try {
        Class.forName("android.car.VehiclePropertyIds")
            .getMethod("toString", Int::class.javaPrimitiveType).invoke(null, id) as String
    } catch (t: Throwable) { "0x%08X".format(id) }

    fun getFloat(ctx: Context, prop: Int, area: Int): Float? {
        val m = manager(ctx) ?: return null
        return try {
            m.javaClass.getMethod("getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(m, prop, area) as Float
        } catch (t: Throwable) { lastError = t.toString(); null }
    }

    fun tires(ctx: Context): List<Float?> = WHEELS.map { getFloat(ctx, TIRE_PRESSURE, it) }

    fun kpaToPsi(k: Float) = k * 0.145038f
}
