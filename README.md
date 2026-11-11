# AutoPhoto AI

Kamera Android dengan pipeline otomatis: **Auto Crop → Auto Zoom → Auto Filter → Auto Enhance**.

## Build & Run
1. Buka folder ini di **Android Studio Koala (2024.1.1)** atau lebih baru, JDK 17.
2. Tunggu Gradle sync (Android Studio akan mengunduh Gradle 8.7 sesuai gradle-wrapper.properties).
3. Colokkan HP Android (7.0+) atau pakai emulator dengan kamera, lalu tekan Run.
4. Pertama kali dipakai, HP perlu internet sebentar agar Google Play services mengunduh model Subject Segmentation.
   Bila model belum siap, foto tetap diproses (tanpa crop, memakai center-crop 3:4).

## Kamera AI terpandu (live)
Saat preview, AI mencari subjek, menentukan tata letak (rule of thirds), lalu menampilkan:
- panah + teks arah geser kamera, target ring, dan kotak subjek
- zoom otomatis halus, filter otomatis (live di preview), jepret otomatis saat komposisi pas
- grid, level horizon, timer, tap-untuk-fokus, pinch zoom
- pengaturan pro: EV, white balance, ISO & rana manual
- thumbnail contoh pose (figur garis atau foto referensi dari galeri) + bayangan pose di preview
