# KaloriKu

Aplikasi Android untuk mencatat kalori dan gizi makanan. Pengguna memfoto makanan,
lalu AI memperkirakan jenis makanan, porsi, kalori, serta gizinya. Ada juga beberapa
cara pencatatan lain supaya aplikasi tetap berguna saat AI tidak bisa dipakai.

Dibuat untuk tugas mata kuliah Pemrograman Mobile.

## Fitur

- **Catat dengan 4 cara**: foto/galeri (analisis AI), cari makanan dari 180 makanan
  Indonesia (TKPI Kemenkes), scan barcode kemasan (Open Food Facts), atau input manual.
- **Hasil + edit porsi**: ubah berat dalam gram, pilih porsi (½, 1, 1½, 2), hapus item,
  dan pilih waktu makan sebelum disimpan.
- **Beranda**: sapaan dan tanggal, cincin kalori (sisa kalori sebagai angka besar),
  tiga bar makro mini, dan daftar makanan hari ini per waktu makan.
- **Riwayat**: ringkasan 7 hari (rata-rata kalori, hari melebihi target, grafik batang),
  dikelompokkan per tanggal.
- **Detail catatan**: waktu makan dan jam, total kalori dan makro, gizi per item,
  serta tombol hapus.
- **Profil**: target harian dan IMT, data tubuh, aktivitas & tujuan, target gizi, dan
  pengingat makan (pagi 08.00, siang 13.00, malam 19.00 via WorkManager).

## Arsitektur

```
UI (Compose)  →  MainViewModel  →  Repository  →  Retrofit/OkHttp (jaringan)
                                               →  SharedPreferences (lokal)
```

- `MainViewModel` menyatukan status layar dan memanggil repository. Status asinkron
  memakai sealed interface `Idle/Loading/Success/Error`.
- Repository jaringan: `FoodAnalysisRepository` (foto, Retrofit + kotlinx.serialization)
  dan `ProductRepository` (barcode, Open Food Facts). `NetworkResult` + `safeApiCall`
  memetakan kegagalan jaringan ke pesan Indonesia; `CancellationException` dilempar ulang.
- Penyimpanan lokal: `MealLogRepository` di atas SharedPreferences `kaloriku`
  (key `entries`, `profile`, `water`, `weights`, `goals`, `reminders_enabled`) dengan
  JSON. `AppContainer` (DI manual) di `KaloriKuApp.kt` menyediakan objek bersama.

## Materi kuliah → implementasi

| Materi | Implementasi (file:baris) |
|---|---|
| Retrofit – interface & anotasi (Pertemuan 6) | `data/remote/AiApiService.kt` (`@POST("chat/completions")`), `data/remote/OpenFoodFactsApiService.kt` (`@GET("api/v2/product/{barcode}.json")`, `@Path`) |
| Retrofit – DTO & serialisasi | `data/remote/AiDtos.kt` (`@Serializable`, `@SerialName`; setara `@SerializedName` di Gson pada slide) |
| Retrofit – konfigurasi klien | `data/remote/NetworkModule.kt` (OkHttp, timeout, header `Authorization`, `User-Agent`, `Json { ignoreUnknownKeys... }`) |
| Retrofit – pemanggilan + error handling | `data/NetworkResult.kt` (`safeApiCall`, `HttpException`/`IOException`/`SerializationException`) |
| Type-Safe Navigation – rute `@Serializable` (Pertemuan 7) | `ui/navigation/AppRoutes.kt` (`data object HomeRoute` … `data class SearchRoute`, `MealDetailRoute`) |
| Type-Safe Navigation – `composable<T>` & argumen | `MainActivity.kt` (`composable<SearchRoute>`, `composable<MealDetailRoute>`) |
| Type-Safe Navigation – baca argumen | `ui/screen/MealDetailScreen.kt` (`backStackEntry.toRoute<MealDetailRoute>()`) |
| Type-Safe Navigation – tab terpilih | `MainActivity.kt` (`destination.hierarchy.any { it.hasRoute(...) }`) |

## Tampilan

Antarmuka memakai Material 3 dengan bahasa desain bersih, tenang, dan jelas:

- **Satu warna aksen**: hijau. `Green600 #16A34A` untuk cincin kalori, ikon aktif, dan
  aksen non-teks; `Green700 #13804F` untuk teks hijau dan tombol berteks putih di mode
  terang supaya kontrasnya lolos (4,97:1). Warna lain hanya untuk data makro
  (protein, karbo, lemak) dan status error.
- **Skala spasi 4 dp** (`ui/theme/Spacing.kt`), padding layar 16 dp.
- **Kartu tonal** (`surfaceContainerLow/High`) tanpa border dan tanpa bayangan, radius
  24 dp; input 16 dp; chip penuh. Bayangan hanya pada bar simpan yang menempel.
- **Navigasi bawah Material 3** (Beranda, Riwayat, Profil) dengan tombol
  `FloatingActionButton` "Catat" dan lembar `AddMealSheet` berisi 4 baris `ListItem`
  (baris Foto ditonjolkan dengan `primaryContainer`).
- **Tanpa gradasi dekoratif**; satu-satunya gradasi adalah scrim gelap di bawah foto hasil.
- **Angka kalori besar** memakai `fontFeatureSettings = "tnum"` dan beranimasi singkat
  (≤ 300 ms). Transisi navigasi berupa fade 150–220 ms.
- **Font Plus Jakarta Sans** (SIL Open Font License) dalam satu berkas variable font.
- **Aksesibilitas**: komponen interaktif memakai `Card(onClick)`/`clickable`, target sentuh
  minimal 48 dp, dan ikon tombol punya `contentDescription`.

## Perbaikan keandalan dan optimasi

- Semua tombol Foto memakai jalur izin yang sama. Izin kamera yang ditolak tidak
  membuat aplikasi tertutup; ada penjelasan serta pilihan membuka Pengaturan.
- Target gizi langsung mengikuti perubahan profil. Input berat dan target bisa
  dikosongkan saat disunting; tombol simpan nonaktif sampai input valid. Koma desimal
  didukung dan pemilih porsi menyelaraskan berat dan kalori.
- Tombol Kembali membatalkan draf/permintaan yang ditinggalkan. Menutup layar
  membatalkan permintaan jaringan yang masih berjalan.
- R8 aktif di release. Aturan `keepRules/rules.keep` menahan registrar ML Kit supaya
  barcode tetap berfungsi; `MealType` diberi `@Keep` karena Navigation mencarinya lewat
  `Class.forName`.
- APK release sekitar 26,7 MiB (ukuran bisa berubah setelah build).

## Cara menjalankan

1. **Pastikan server AI menyala.** Aplikasi memanggil server berformat OpenAI-compatible.
   Secara bawaan diarahkan ke 9router di `http://10.0.2.2:20128/v1`
   (alamat komputer dilihat dari dalam emulator Android).

2. **Atur alamat server AI** di `local.properties`:
   ```properties
   AI_BASE_URL=http://10.0.2.2:20128/v1
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
- **API key tertanam di dalam APK.** Wajar untuk tugas kuliah, tapi jangan bagikan APK-nya
  ke publik dan jangan unggah `local.properties` ke Git (sudah masuk `.gitignore`).
- Aplikasi mengizinkan HTTP polos (bukan HTTPS) **hanya** untuk alamat server lokal
  yang terdaftar di `res/xml/network_security_config.xml`. Alamat lain tetap wajib HTTPS.

## Struktur kode

```
app/src/main/java/com/hajun/kaloriku/
├── KaloriKuApp.kt              Container bersama (repositori, service jaringan)
├── MainActivity.kt             AppRoot: NavHost type-safe, bottom bar, BackHandler
├── data/                       Model, MealLogRepository, Stats, Profile,
│   ├── remote/                 AI + Open Food Facts (Retrofit), NetworkModule
│   ├── FoodAnalysisRepository / ProductRepository / NetworkResult
│   └── MealJson / ExtraJson / MealLookup
├── ui/
│   ├── MainViewModel.kt        Logika layar (Idle/Loading/Success/Error)
│   ├── navigation/AppRoutes.kt Rute @Serializable + logika pemilihan draf
│   ├── screen/                 Home, Result, FoodSearch, Barcode, History,
│   │                           Goals, Profile, MealDetail
│   ├── charts/                 Cincin kalori, bar makro, grafik batang
│   ├── components/             Kartu, tombol, NavigationBar, AddMealSheet, angka animasi
│   └── theme/                  Color, Type, Theme, Spacing
├── notification/               Pengingat makan (WorkManager)
└── util/                       Format, gambar, NumericInput, ReminderTiming

app/src/main/assets/foods.csv   180 makanan Indonesia (TKPI Kemenkes)
app/src/test/                   88 unit test (JVM)
app/src/androidTest/            5 test instrumented (regresi penyimpanan + UI detail)
```

## Pengujian

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest  # perlu emulator/HP aktif
./gradlew lintDebug
```

88 unit test mencakup kalkulator kalori, statistik, parser AI/barcode, JSON, validasi
input angka, pembaruan target lewat Flow, waktu pengingat, pencarian catatan dan
pemilihan waktu makan dari `SearchRoute`, serta mock server jaringan tanpa memanggil API
sungguhan. Test Android memeriksa target/penyimpanan pengingat dan layar detail catatan.

## Sumber data

- **Tabel Komposisi Pangan Indonesia (TKPI)** — Kementerian Kesehatan RI, https://panganku.org
- **Open Food Facts** — data gizi makanan kemasan, https://world.openfoodfacts.org
- **Rumus Mifflin-St Jeor** — untuk menghitung kebutuhan kalori harian
- **Material Symbols (Rounded)** — ikon antarmuka, lisensi Apache 2.0
- **Plus Jakarta Sans** — font antarmuka, lisensi SIL Open Font License
