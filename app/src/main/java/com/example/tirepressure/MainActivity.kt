package com.example.tirepressure

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private const val MATCH = -1   // MATCH_PARENT
private const val WRAP = -2    // WRAP_CONTENT

val WHEEL_NAMES = listOf("หน้าซ้าย", "หน้าขวา", "หลังซ้าย", "หลังขวา")

private val C_OK = Color.parseColor("#4CAF50")
private val C_LOW = Color.parseColor("#FF5252")
private val C_HIGH = Color.parseColor("#FFB300")
private val C_NA = Color.parseColor("#78909C")

private fun tireStatus(v: Float?): Pair<String, Int> = when {
    v == null -> Pair("ไม่มีข้อมูล", C_NA)
    v < CarProps.LOW_KPA -> Pair("แรงดันต่ำ", C_LOW)
    v > CarProps.HIGH_KPA -> Pair("แรงดันสูง", C_HIGH)
    else -> Pair("ปกติ", C_OK)
}

/** หน้าหลัก: รูปรถ + ยาง 4 ล้อ + สถานะแรงดัน + พื้นหลัง (กดปุ่มล่างเพื่อดูรายละเอียดเชิงเทคนิค) */
class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var banner: TextView
    private lateinit var carView: CarView
    private lateinit var debugText: TextView
    private lateinit var debugScroll: ScrollView
    private val tick = object : Runnable {
        override fun run() { refresh(); h.postDelayed(this, 2000) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = FrameLayout(this)
        root.addView(BackgroundView(this), FrameLayout.LayoutParams(MATCH, MATCH))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(24), dp(14), dp(24), dp(12))

        val title = TextView(this)
        title.text = "แรงดันลมยาง"
        title.setTextColor(Color.WHITE)
        title.textSize = 26f
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        col.addView(title, LinearLayout.LayoutParams(MATCH, WRAP))

        banner = TextView(this)
        banner.gravity = Gravity.CENTER
        banner.textSize = 20f
        banner.setTextColor(Color.WHITE)
        banner.setPadding(dp(16), dp(10), dp(16), dp(10))
        val bp = LinearLayout.LayoutParams(MATCH, WRAP)
        bp.topMargin = dp(8)
        col.addView(banner, bp)

        carView = CarView(this)
        col.addView(carView, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val btn = Button(this)
        btn.text = "รายละเอียดเชิงเทคนิค (ใช้ตรวจสอบ)"
        val btnLp = LinearLayout.LayoutParams(WRAP, WRAP)
        btnLp.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(btn, btnLp)

        debugText = TextView(this)
        debugText.textSize = 13f
        debugText.typeface = Typeface.MONOSPACE
        debugText.setTextColor(Color.parseColor("#CFD8DC"))
        debugScroll = ScrollView(this)
        debugScroll.visibility = View.GONE
        debugScroll.addView(debugText)
        col.addView(debugScroll, LinearLayout.LayoutParams(MATCH, dp(200)))

        btn.setOnClickListener {
            debugScroll.visibility = if (debugScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            refresh()
        }

        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)

        val perm = "android.car.permission.CAR_TIRES"
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(perm), 1)
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    private fun setBanner(text: String, color: Int) {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(14).toFloat()
        banner.background = g
        banner.text = text
    }

    private fun refresh() {
        val vals = CarProps.tires(this)
        carView.update(vals)

        val warn = ArrayList<String>()
        vals.forEachIndexed { i, v ->
            if (v != null) {
                if (v < CarProps.LOW_KPA) warn.add(WHEEL_NAMES[i] + " ต่ำ")
                else if (v > CarProps.HIGH_KPA) warn.add(WHEEL_NAMES[i] + " สูง")
            }
        }
        when {
            vals.all { it == null } -> setBanner("ยังอ่านค่าแรงดันจากรถไม่ได้", C_NA)
            warn.isNotEmpty() -> setBanner("แจ้งเตือน: " + warn.joinToString(", "), C_LOW)
            vals.any { it == null } -> setBanner("ปกติ (บางล้อไม่มีข้อมูล)", C_HIGH)
            else -> setBanner("แรงดันลมยางปกติทุกล้อ", C_OK)
        }

        if (debugScroll.visibility == View.VISIBLE) debugText.text = buildDebug(vals)
    }

    private fun buildDebug(vals: List<Float?>): String {
        val sb = StringBuilder()
        sb.append("== ค่ายาง (TIRE_PRESSURE 392168201) ==\n")
        vals.forEachIndexed { i, kpa ->
            sb.append(WHEEL_NAMES[i]).append(": ")
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
        return sb.toString()
    }
}

/** พื้นหลัง (วอลเปเปอร์): ไล่เฉดสีน้ำเงินเข้ม + แสงเรืองกลางจอ + จุดลายตาราง */
class BackgroundView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, Color.parseColor("#FF070B14"), Color.parseColor("#FF1B2A41"), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w / 2f, h * 0.5f, h * 0.9f, Color.parseColor("#553A7BD5"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.color = Color.parseColor("#14FFFFFF")
        val step = minOf(w, h) / 14f
        var y = step / 2f
        while (y < h) {
            var x = step / 2f
            while (x < w) {
                c.drawCircle(x, y, 1.6f, p)
                x += step
            }
            y += step
        }
    }
}

/** วาดรถมองจากด้านบน + ยาง 4 ล้อ พร้อมค่าแรงดันและสถานะข้างแต่ละล้อ (หัวรถอยู่ด้านบน) */
class CarView(ctx: Context) : View(ctx) {
    private var values: List<Float?> = listOf(null, null, null, null)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)

    fun update(v: List<Float?>) { values = v; invalidate() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val carH = h * 0.84f
        val carW = minOf(carH * 0.46f, w * 0.2f)
        val top = (h - carH) / 2f - h * 0.02f
        val left = cx - carW / 2f
        val right = left + carW
        val bottom = top + carH

        // แสงใต้รถ
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, h / 2f, carH * 0.7f, Color.parseColor("#403A7BD5"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx, h / 2f, carH * 0.7f, p)
        p.shader = null

        // ตัวถัง
        val r = carW * 0.3f
        val body = RectF(left, top, right, bottom)
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#2B3A55")
        c.drawRoundRect(body, r, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = carW * 0.025f
        p.color = Color.parseColor("#6F8FBF")
        c.drawRoundRect(body, r, r, p)
        p.style = Paint.Style.FILL

        // กระจกหน้า / หลังคา / กระจกหลัง
        val wsT = top + carH * 0.22f
        p.color = Color.parseColor("#7FB2E5")
        c.drawRoundRect(RectF(left + carW * 0.14f, wsT, right - carW * 0.14f, wsT + carH * 0.14f), carW * 0.1f, carW * 0.1f, p)
        p.color = Color.parseColor("#3A4C6B")
        c.drawRoundRect(RectF(left + carW * 0.16f, wsT + carH * 0.17f, right - carW * 0.16f, wsT + carH * 0.40f), carW * 0.08f, carW * 0.08f, p)
        p.color = Color.parseColor("#5E8DBD")
        c.drawRoundRect(RectF(left + carW * 0.16f, wsT + carH * 0.43f, right - carW * 0.16f, wsT + carH * 0.53f), carW * 0.1f, carW * 0.1f, p)

        // ไฟหน้า / ไฟท้าย
        p.color = Color.parseColor("#FFF4C2")
        c.drawRoundRect(RectF(left + carW * 0.1f, top + carH * 0.03f, left + carW * 0.3f, top + carH * 0.065f), 8f, 8f, p)
        c.drawRoundRect(RectF(right - carW * 0.3f, top + carH * 0.03f, right - carW * 0.1f, top + carH * 0.065f), 8f, 8f, p)
        p.color = Color.parseColor("#E53935")
        c.drawRoundRect(RectF(left + carW * 0.1f, bottom - carH * 0.065f, left + carW * 0.3f, bottom - carH * 0.03f), 8f, 8f, p)
        c.drawRoundRect(RectF(right - carW * 0.3f, bottom - carH * 0.065f, right - carW * 0.1f, bottom - carH * 0.03f), 8f, 8f, p)

        // ยาง 4 ล้อ (ลำดับ: หน้าซ้าย, หน้าขวา, หลังซ้าย, หลังขวา)
        val tw = carW * 0.2f
        val th = carH * 0.19f
        val frontY = top + carH * 0.10f
        val rearY = top + carH * 0.69f
        val valSize = h * 0.085f
        val nameSize = h * 0.042f
        val subSize = h * 0.042f
        val gap = carW * 0.12f

        for (i in 0..3) {
            val isLeft = (i % 2 == 0)
            val isFront = i < 2
            val wl = if (isLeft) left - tw * 0.5f else right - tw * 0.5f
            val wt = if (isFront) frontY else rearY
            val rect = RectF(wl, wt, wl + tw, wt + th)
            val v = values.getOrNull(i)
            val st = tireStatus(v)
            val label = st.first
            val col = st.second
            val bad = (col == C_LOW || col == C_HIGH)

            if (bad) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = tw * 0.5f
                p.color = Color.argb(90, Color.red(col), Color.green(col), Color.blue(col))
                c.drawRoundRect(rect, tw * 0.35f, tw * 0.35f, p)
            }
            p.style = Paint.Style.FILL
            p.color = Color.parseColor("#0E1116")
            c.drawRoundRect(rect, tw * 0.35f, tw * 0.35f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = tw * 0.14f
            p.color = col
            c.drawRoundRect(rect, tw * 0.35f, tw * 0.35f, p)
            p.style = Paint.Style.FILL

            // ข้อความข้างล้อ
            val cy = wt + th / 2f
            val tx = if (isLeft) rect.left - gap else rect.right + gap
            tp.textAlign = if (isLeft) Paint.Align.RIGHT else Paint.Align.LEFT
            val base = cy + valSize * 0.15f

            tp.typeface = Typeface.DEFAULT
            tp.textSize = nameSize
            tp.color = Color.parseColor("#9FB3C8")
            c.drawText(WHEEL_NAMES[i], tx, cy - valSize * 0.95f, tp)

            tp.typeface = Typeface.DEFAULT_BOLD
            tp.textSize = valSize
            tp.color = col
            c.drawText(if (v == null) "--" else "%.0f".format(v), tx, base, tp)

            tp.typeface = Typeface.DEFAULT
            tp.textSize = subSize
            tp.color = Color.parseColor("#CFD8DC")
            c.drawText(if (v == null) "kPa" else "kPa  •  %.1f psi".format(CarProps.kpaToPsi(v)), tx, base + subSize * 1.4f, tp)

            tp.typeface = Typeface.DEFAULT_BOLD
            tp.color = col
            c.drawText(label, tx, base + subSize * 2.8f, tp)
        }

        // คำอธิบายสี
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.DEFAULT
        tp.textSize = h * 0.034f
        tp.color = Color.parseColor("#8FA3B8")
        c.drawText(
            "เขียว = ปกติ    แดง = ต่ำกว่า %.0f kPa    เหลือง = สูงกว่า %.0f kPa".format(CarProps.LOW_KPA, CarProps.HIGH_KPA),
            cx, h * 0.985f, tp
        )
    }
}
