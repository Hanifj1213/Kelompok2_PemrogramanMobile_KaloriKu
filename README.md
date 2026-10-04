# KaloriKu

Aplikasi Android untuk mencatat kalori dan gizi makanan. Pengguna memfoto makanan,
lalu AI memperkirakan jenis makanan, porsi, kalori, serta gizinya. Ada juga beberapa
cara pencatatan lain supaya aplikasi tetap berguna saat AI tidak bisa dipakai.

Dibuat untuk tugas mata kuliah Pemrograman Mobile.

## Fitur

### Mencatat makanan dengan 4 cara
1. **Foto** — ambil foto dari kamera atau pilih dari galeri, lalu AI menganalisisnya.
2. **Cari makanan** — cari dari 180 makanan Indonesia (Tabel Komposisi Pangan Indonesia /
   TKPI Kemenkes), lengkap dengan takaran rumah tangga seperti "1 piring = 150 g".
   Ini jalan keluar kalau AI salah menebak atau internet mati.
3. **Scan barcode** — pindai barcode kemasan, data gizi diambil otomatis dari
   Open Food Facts.
4. **Input suara** — sebutkan makananmu, misalnya "saya makan nasi goreng dan telur
   dadar", lalu AI mengubahnya menjadi daftar makanan.

Hasil dari cara mana pun bisa disunting dulu: ubah berat dalam gram, pilih porsi
(½×, 1×, 1½×, 2×), hapus item yang salah, dan pilih waktu makan (sarapan, makan siang,
makan malam, camilan).

### Pemantauan harian
- Ringkasan kalori dan capaian protein, karbohidrat, lemak dengan progress bar.
- Pencatat air minum (target 8 gelas, ada tombol batal).
- Rentetan hari mencatat (streak).
- Target kalori dihitung dari profil memakai rumus Mifflin-St Jeor.
- Target gizi bisa disesuaikan, dengan saran otomatis dari target kalori.
- Status gizi (IMT/BMI) dengan ambang batas untuk orang Indonesia.

### Riwayat dan laporan
- Riwayat makan per hari dengan grafik 7 hari, bisa dihapus.
- Catatan berat badan beserta grafik progres.
- Ringkasan mingguan otomatis: rata-rata gizi, jumlah hari melebihi target,
  dan catatan seperti "protein kurang dari 80% target pada 3 hari".
- Ekspor catatan ke berkas CSV (untuk lampiran laporan).
- Bagikan ringkasan harian sebagai gambar 1080x1350.

### Lainnya
- Pengingat makan pagi (08.00), siang (13.00), dan malam (19.00). Notifikasi hanya
  dikirim kalau hari itu belum ada catatan untuk waktu makan tersebut. Sakelar di
  Profil langsung disimpan, termasuk ketika profil belum lengkap. WorkManager
  mengikuti pembatasan baterai Android, jadi notifikasi tidak dijamin tepat menitnya.
- Widget layar utama: sisa kalori dan air minum tanpa membuka aplikasi.
- Health Connect: membaca jumlah langkah harian dan menuliskan data gizi
  (opsional, aplikasi tetap berjalan normal kalau tidak tersedia).

## Tampilan

Antarmuka memakai Material 3 dengan bahasa desain khusus aplikasi ini:

- **Navigasi bawah** berisi empat tab (Beranda, Riwayat, Laporan, Profil) plus satu
  tombol bulat di tengah untuk membuka lembar "Catat makanan". Layar detail
  (hasil, pencarian, barcode, suara, berat, target) menyembunyikan bilah ini
  supaya ruangnya lebih lega.
- **Cincin kalori** sebagai angka utama Beranda: sisa kalori hari ini tampil besar di
  tengah busur kemajuan yang beranimasi. Layar hasil memakai cincin serupa.
- **Cincin makro** untuk capaian protein, karbohidrat, dan lemak dengan warna tetap
  (biru, jingga, ungu) yang konsisten di seluruh aplikasi.
- **Grafik digambar sendiri** memakai Canvas: batang tujuh hari dengan garis target
  putus-putus, serta grafik garis untuk perkembangan berat badan.
- **Font Plus Jakarta Sans** (lisensi SIL Open Font License) dalam satu berkas
  variable font, jadi semua ketebalan tersedia tanpa membengkakkan APK.
- **Tema terang dan gelap** keduanya lengkap, termasuk warna latar jendela.
- **Ikon vektor** dari Material Symbols (Rounded), tersedia versi isi dan garis.
- Kartu makanan bisa **diketuk untuk diperluas** dan menampilkan rincian gizi per item.

## Perbaikan keandalan dan optimasi

- Semua tombol Foto memakai jalur izin yang sama. Izin kamera yang ditolak tidak
  membuat aplikasi tertutup; ada penjelasan serta pilihan membuka Pengaturan.
- Target gizi langsung mengikuti perubahan profil dan makro yang disimpan. Kalori
  tetap dihitung dari profil; makro khusus tidak direset ketika profil berubah.
- Input berat dan target bisa dikosongkan saat disunting, tetapi tombol simpan/tambah
  tidak aktif sampai input valid. Koma desimal didukung dan angka input tidak memakai
  pemisah ribuan. Tombol porsi menyelaraskan berat dan kalori.
- Tombol Kembali membatalkan draf/permintaan yang ditinggalkan. Pencarian baru tidak
  mencampurkan draf lama; menambahkan item pada hasil yang sama tetap mempertahankannya.
- Pemindai barcode memproses satu frame pada satu waktu, memakai format barcode produk
  saja, dan melepas kamera serta scanner ketika layar ditutup.
- Permintaan jaringan yang dibatalkan menutup koneksinya. Teks pencarian makanan
  dinormalisasi sekali agar tidak diulang untuk setiap pencarian.
- Release mengaktifkan R8 untuk optimasi kode dan resource. APK unsigned turun dari
  sekitar 59,3 MiB menjadi 26,4 MiB (ukuran dapat sedikit berubah setelah build).
- Layar barcode, suara, dan lembar pilihan bisa digulir; tab navigasi memiliki semantik
  pilihan untuk pembaca layar. Tidak ada upgrade dependency besar dalam perbaikan ini.

## Cara menjalankan

1. **Pastikan server AI menyala.** Aplikasi memanggil server berformat OpenAI-compatible.
   Secara bawaan diarahkan ke 9router di `http://10.0.2.2:20128/v1`
   (alamat komputer dilihat dari dalam emulator Android).

2. **Atur alamat server AI** di `local.properties`:
   ```properties
   # Emulator Android Studio
   AI_BASE_URL=http://10.0.2.2:20128/v1
   # HP lewat USB: jalankan "adb reverse tcp:20128 tcp:20128" lalu pakai alamat ini
   # AI_BASE_URL=http://127.0.0.1:20128/v1
   # HP satu Wi-Fi dengan komputer
   # AI_BASE_URL=http://192.168.1.10:20128/v1

   AI_API_KEY=isi_api_key_kamu
   AI_MODEL=ag/gemini-3.8-flash-high
   ```

3. **Jalankan aplikasi** lewat Android Studio (tombol ▶), atau build dari terminal:
   ```bash
   ./gradlew assembleDebug     # APK debug
   ./gradlew assembleRelease   # APK release (lebih mulus, tanpa debugger)
   ```

## Catatan penting

- **Nilai gizi dari AI adalah perkiraan**, terutama bagian porsi. Pengguna dianjurkan
  menyesuaikan beratnya. Data dari TKPI (pencarian manual) lebih akurat karena memakai
  angka resmi per 100 gram.
- **Build release lebih tepat untuk menilai performa** daripada build debug. Pengukuran
  sebelum perbaikan terakhir menunjukkan debug 7,3% frame tersendat dan release 1,5%
  saat menggulir 45 catatan. Angka tersebut bukan benchmark ulang versi terbaru;
  optimasi terakhir diverifikasi lewat ukuran APK dan pengujian alur, bukan klaim FPS baru.
- **API key tertanam di dalam APK.** Wajar untuk tugas kuliah, tapi jangan bagikan APK-nya
  ke publik dan jangan unggah `local.properties` ke Git (sudah masuk `.gitignore`).
- Aplikasi mengizinkan HTTP polos (bukan HTTPS) **hanya** untuk alamat server lokal
  yang terdaftar di `res/xml/network_security_config.xml`. Alamat lain tetap wajib HTTPS.

## Struktur kode

```
app/src/main/java/com/hajun/kaloriku/
├── KaloriKuApp.kt              Container bersama (repositori, klien AI)
├── MainActivity.kt             Navigasi semua layar
├── data/
│   ├── FoodDatabase.kt         Baca TKPI dari assets/foods.csv + pencarian
│   ├── Stats.kt                Ringkasan harian, streak, laporan mingguan
│   ├── AiClient.kt             Kirim foto/teks ke server AI (format OpenAI)
│   ├── AiParser.kt             Baca jawaban AI
│   ├── BarcodeClient.kt        Cari produk di Open Food Facts
│   ├── FoodModels.kt           Model makanan dan catatan harian
│   ├── Profile.kt              Profil + rumus Mifflin-St Jeor
│   ├── MealLogRepository.kt    Penyimpanan lokal
│   └── MealJson.kt, ExtraJson.kt   Konversi JSON
├── ui/
│   ├── MainViewModel.kt        Logika layar
│   ├── screen/                 Home, Result, FoodSearch, Barcode, Voice,
│   │                           History, Weight, Weekly, Goals, Profile
│   ├── charts/                 Cincin kalori, ring makro, grafik batang dan garis
│   ├── components/             Kartu, tombol, bilah navigasi, lembar catat
│   └── theme/                  Warna, tipografi, dan bentuk
├── notification/               Pengingat makan (WorkManager)
├── widget/                     Widget layar utama (Glance)
├── health/                     Health Connect
└── util/                       Format angka, CSV, gambar, berbagi

app/src/main/assets/foods.csv   180 makanan Indonesia (TKPI Kemenkes)
app/src/main/res/font/          Plus Jakarta Sans (variable font)
app/src/test/                   63 unit test
app/src/androidTest/            4 test regresi penyimpanan di perangkat
```

## Pengujian

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest  # perlu emulator/HP aktif
./gradlew lintDebug
```

63 unit test mencakup kalkulator kalori, statistik, parser AI/barcode, CSV/JSON,
validasi input angka, pembaruan target lewat Flow, waktu pengingat, serta pembatalan
koneksi jaringan tanpa memanggil API sungguhan. Empat test Android memakai berkas
preferensi terpisah untuk memeriksa target dan pengingat tanpa menghapus data pengguna.

Pengujian interaksi juga dilakukan di emulator: penolakan izin kamera, target gizi
langsung berubah, input kosong/berat 1000 g, pilihan 2 porsi, pembatalan draf, dan
pengingat yang tetap mati sesudah aplikasi dibuka ulang. Pemindaian barcode fisik,
suara dari mikrofon, dan Health Connect tetap membutuhkan validasi pada HP nyata.

## Sumber data

- **Tabel Komposisi Pangan Indonesia (TKPI)** — Kementerian Kesehatan RI,
  https://panganku.org
- **Open Food Facts** — data gizi makanan kemasan, https://world.openfoodfacts.org
- **Rumus Mifflin-St Jeor** — untuk menghitung kebutuhan kalori harian
- **Material Symbols (Rounded)** — ikon antarmuka, lisensi Apache 2.0
- **Plus Jakarta Sans** — font antarmuka, lisensi SIL Open Font License
