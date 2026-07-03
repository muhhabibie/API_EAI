# LAPORAN ESSAI: DEKOMPOSISI SISTEM ENTERPRISE UNIVERSITY
## Value Stream: Student Journey — Universitas Brawijaya (UB)
**Mata Kuliah:** Integrasi Aplikasi Perusahaan (Enterprise Application Integration - EAI)  
**Fakultas Ilmu Komputer, Universitas Brawijaya**

---

### Kelompok Pengerjaan:
1. **Muhammad Habibi** (NIM: 235150201111063)
2. **Harry Phalosa Telaumbanua** (NIM: 235150200111052)
3. **Maulana Aryan Wicaksana Sabandar** (NIM: 235150201111056)
4. **Sindu Sanova** (NIM: 235150207111057)

---

## 1. Pemahaman Terhadap Sistem Akademik yang Berlaku di UB Saat Ini (As-Is)

Universitas Brawijaya (UB) mengelola ribuan mahasiswa aktif menggunakan ekosistem aplikasi seperti **SIAM** (Sistem Informasi Akademik Mahasiswa), **SABDA** (Sistem Akademik Brawijaya Baru), **Gapura UB** (Portal SSO), **SIADO** (Sistem Informasi Anggota Dosen), serta **VLM/BRONE** (Virtual Learning Management). 

Namun, berdasarkan analisis perspektif EAI, sistem **As-Is** saat ini memiliki keterbatasan integrasi:
1. **Sinkronisasi Batch / Latensi Data**: Integrasi antara sistem perbankan mitra (pembayaran UKT) dengan SIAM sering kali menggunakan pemrosesan berkala (batch processing) harian. Hal ini menyebabkan status registrasi mahasiswa tertunda (misalnya, mahasiswa sudah membayar UKT di bank, namun status di SIAM masih "Non-Aktif" hingga beberapa jam atau hari berikutnya).
2. **Ketergantungan Sinkron (Tight Coupling)**: Beberapa sub-sistem berkomunikasi secara langsung menggunakan pemanggilan API sinkron. Jika SIAM mengalami gangguan atau beban tinggi, sistem pendukung seperti VLM/LMS atau portal pengisian KRS dosen wali ikut terganggu karena tidak adanya *message broker* untuk mengantre request.
3. **Penurunan Performa saat Beban Puncak (High-Load Bottleneck)**: Periode pengisian KRS (Kartu Rencana Studi) selalu memicu kepadatan trafik yang melumpuhkan SIAM. Hal ini terjadi karena arsitektur basis data dan perutean request tidak dirancang untuk memisahkan domain baca (*read*) dan domain tulis (*write*), serta belum didukung oleh *auto-scaling instance* yang elastis.

---

## 2. Context System Berdasarkan Student Journey yang Diharapkan (To-Be)

Menggunakan prinsip **Enterprise Application Integration (EAI)** yang dipelajari dalam perkuliahan (seperti Event-Driven Architecture, Kafka Message Broker, API Gateway, dan Saga Pattern), kami merancang arsitektur **To-Be** untuk menunjang **Student Journey** (sejak mahasiswa diterima, menjalani studi akademik, hingga lulus/alumni) secara dinamis dan *fault-tolerant*.

### A. CONTEXT (Konteks Sistem)
Sistem Integrasi Student Journey UB dirancang sebagai platform microservices berbasis event (*event-driven microservices*). Seluruh sistem akademik dihubungkan oleh **Apache Kafka** sebagai tulang punggung integrasi data asinkron, dengan **Spring Cloud Gateway** sebagai gerbang pengamanan JWT terpusat (*Edge Auth*) dan **Eureka Server** sebagai *dynamic service discovery*. Setiap transisi status dalam *student journey* (misal: pembayaran UKT lunas, KRS disetujui, kelulusan sidang) diterbitkan (*publish*) sebagai *event* sehingga sub-sistem lain dapat bereaksi secara instan untuk mencapai konsistensi data akhir (*eventual consistency*).

```
[ Mahasiswa / Client ]
         │ (HTTPS + JWT)
         ▼
[ Spring Cloud Gateway ] ─── (Dynamic Query) ───► [ Eureka Discovery Server ]
         │
         ├──► [ Auth & Profile Service ]
         ├──► [ Academic & KRS Service ] ──(Sync)──► [ local_krs_db ]
         │                                               │
         │                                        (Publish: krs.approved)
         │                                               ▼
         ▼                                         [ Apache Kafka ]
[ VLM / LMS Service ] ◄──(Consume: krs.approved)────────┘
```

---

### B. Capabilities (Kapabilitas Sistem)

Sistem Student Journey didekomposisi menjadi beberapa kapabilitas inti:
1. **Identity & Edge Security Capability** (Autentikasi & Keamanan Gerbang Utama)
2. **Enrollment & Academic Planning Capability** (Registrasi & Pengisian KRS)
3. **Financial Reconciliation Capability** (Sinkronisasi Keuangan & UKT)
4. **Learning Environment Synchronization Capability** (Integrasi LMS & VLM)
5. **Graduation & Clearance Capability** (Orkestrasi Kelulusan & Yudisium)
6. **Alumni Transition & Tracking Capability** (Manajemen Alumni & Tracer Study)

---

### C. Penjelasan Setiap Capability (Deliverable)

#### 1. Identity & Edge Security Capability
* **Deliverable / Penjelasan**: Kapabilitas ini menyediakan mekanisme masuk tunggal (Single Sign-On/SSO) yang aman. API Gateway bertindak sebagai *verifikator JWT*. Ketika token valid, identitas mahasiswa diteruskan sebagai header HTTP (`X-Student-Id`, `X-Student-Role`) ke downstream service. Dosen dan mahasiswa tidak perlu melakukan autentikasi berulang di SIAM atau VLM.

#### 2. Enrollment & Academic Planning Capability
* **Deliverable / Penjelasan**: Mesin pengisian KRS berbasis kuota dinamis. Kapabilitas ini menerapkan teknik reservasi stok asinkron (mirip pada *inventory service*). Ketika mahasiswa mengeklik "Ambil Kelas", sistem akan mengunci satu slot kuota secara asinkron di database lokal `KRS Service`. Mahasiswa tidak perlu menunggu proses database utama selesai (*non-blocking request*), sehingga SIAM tetap responsif saat perang KRS.

#### 3. Financial Reconciliation Capability
* **Deliverable / Penjelasan**: Sistem rekonsiliasi UKT otomatis. Bank mitra mempublikasikan event `payment.ukt.received` ke Kafka. `Academic Service` langsung mengonsumsi event ini dan secara instan mengubah status registrasi mahasiswa menjadi **`ACTIVE`** tanpa menunggu rekonsiliasi manual harian.

#### 4. Learning Environment Synchronization Capability
* **Deliverable / Penjelasan**: Integrasi otomatis kelas kuliah. Begitu KRS disetujui dosen wali, `Academic Service` mempublikasikan event `krs.approved`. `VLM/LMS Service` mengonsumsi event ini dan secara otomatis memasukkan mahasiswa ke kanal kelas kuliah bersangkutan di platform pembelajaran digital (BRONE/VLM) secara real-time.

#### 5. Graduation & Clearance Capability
* **Deliverable / Penjelasan**: Bebas tanggungan kelulusan berbasis **Saga Choreography**. Saat mahasiswa mendaftar Yudisium, `Graduation Service` menerbitkan event `yudisium.applied`. Event ini memicu pengecekan paralel secara asinkron:
  - `Library Service` (memeriksa pinjaman buku).
  - `Finance Service` (memeriksa tunggakan UKT).
  - `Department Service` (memeriksa syarat kelulusan akademik).
  Jika semua layanan membalas sukses (`clearance.approved`), mahasiswa otomatis dinyatakan berstatus `LULUS`.

#### 6. Alumni Transition & Tracking Capability
* **Deliverable / Penjelasan**: Transisi alumni otomatis. Begitu status kelulusan disematkan, event `student.graduated` diterbitkan. `Alumni Portal` dan `Tracer Study Service` secara otomatis membuat profil alumni baru menggunakan data historis akademik mahasiswa tanpa perlu input ulang manual.

---

## 3. Uraian atau Feedback dari Mata Kuliah EAI

Sebagai kelompok mahasiswa yang sedang menggarap proyek integrasi microservices pada mata kuliah EAI, berikut adalah umpan balik kami:
* **Pengalaman Praktis**: Pembelajaran EAI memberikan wawasan yang sangat relevan dengan kebutuhan industri saat ini. Konsep-konsep teoritis seperti *decoupling*, *loose coupling*, dan *scalability* terasa nyata ketika kami mengimplementasikan **Saga Choreography Pattern** menggunakan Apache Kafka di Spring Boot.
* **Observability & Monitoring**: Kami belajar bahwa membangun sistem terdistribusi tidak hanya soal menulis kode, tetapi juga tentang cara memantaunya. Implementasi **OpenTelemetry, Jaeger, Prometheus, dan Grafana** pada proyek kami membuka mata kami tentang pentingnya pelacakan jejak request (*distributed tracing*) untuk men-debug error transaksi terdistribusi.
* **Saran untuk Kelas Selanjutnya**: Kami menyarankan agar modul perkuliahan masa depan menambahkan studi kasus atau materi khusus mengenai **Refactoring Monolith to Microservices** secara bertahap, serta strategi migrasi data (*zero-downtime database migration*), karena skenario dunia nyata paling sering melibatkan sistem warisan (*legacy system*) yang harus dimigrasi secara hati-hati.
