# AutoPhoto AI

Kamera Android dengan pipeline otomatis: **Auto Crop → Auto Zoom → Auto Filter → Auto Enhance**.

## Build & Run
1. Buka folder ini di **Android Studio Koala (2024.1.1)** atau lebih baru, JDK 17.
2. Tunggu Gradle sync (Android Studio akan mengunduh Gradle 8.7 sesuai gradle-wrapper.properties).
3. Colokkan HP Android (7.0+) atau pakai emulator dengan kamera, lalu tekan Run.
4. Pertama kali dipakai, HP perlu internet sebentar agar Google Play services mengunduh model Subject Segmentation.
   Bila model belum siap, foto tetap diproses (tanpa crop, memakai center-crop 3:4).
