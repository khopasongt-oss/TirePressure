TirePressure - แอปเสริมอ่านแรงดันลมยางจากรถ (Android Automotive)

1) เปิดโฟลเดอร์นี้ด้วย Android Studio แล้ว Build > Build APK
2) ติดตั้งลงหัวเครื่อง แล้วเปิดแอป "แรงดันลมยาง" หน้าทดสอบจะแสดง
   - ค่ายาง 4 ล้อสด ๆ (kPa/psi)
   - รายการ property ทั้งหมดที่รถเปิดให้แอปเห็น
3) ถ้าหน้านี้ขึ้นค่ายาง -> เพิ่มวิดเจ็ต "TirePressure" ได้เลย
   ถ้าอ่านไม่ได้ -> ถ่ายภาพหน้าจอ (โดยเฉพาะ "ข้อผิดพลาดล่าสุด" และส่วน property เกี่ยวกับยาง) ส่งกลับมา
เกณฑ์สีเตือนแก้ได้ใน CarProps.kt (LOW_KPA / HIGH_KPA)
หมายเหตุ: ยังไม่เคย build หรือทดสอบบนรถจริง

สร้าง APK โดยไม่ใช้ Android Studio:
 - อัปโหลดโปรเจกต์ขึ้น GitHub repo ของตัวเอง แล้วเข้าแท็บ Actions > Build APK > Run workflow
   เสร็จแล้วดาวน์โหลด TirePressure-debug-apk (app-debug.apk) จากหน้า run
 - หรือบนคอมที่มี Android SDK + Gradle 8.7: gradle assembleDebug
   ไฟล์อยู่ที่ app/build/outputs/apk/debug/app-debug.apk
