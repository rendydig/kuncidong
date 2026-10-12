# kuncidong-parent (Go)

CLI **"Orang Tua"** untuk mengunci HP anak dari PC. Tool ini berbicara protokol
TCP-JSON yang sama dengan app **kunci dong** (peran Anak), jadi bisa dipakai untuk
menguji fitur remote lock tanpa memakai app Parent Android.

Ditulis dengan **Go standard library saja** — tidak ada dependency eksternal.

## Cara kerja

App di HP anak (peran **Anak**) membuka server TCP di port `8787` dan mengiklankan
diri lewat mDNS/NSD. Tool ini mengirim satu perintah JSON per koneksi:

```
Request : {"v":1,"cmd":"...", ...}
Response: {"v":1,"ok":true, ...}
```

Perintah `hello` dan `pair` tidak butuh token; sisanya wajib menyertakan token hasil pairing.

## Build

```bash
cd parent-go
go build -o kuncidong-parent .
```

Atau langsung jalan tanpa build: `go run . <perintah> [opsi]`.

## Perintah

| Perintah | Keterangan |
|---|---|
| `scan` | Cari HP anak di subnet Wi-Fi lokal |
| `hello` | Info HP anak (nama, sudah dipasangkan?) |
| `pair` | Pasangkan dengan kode 6 digit dari layar HP anak |
| `lock` | Kunci HP anak (`--mode quiz` atau `--mode text`) |
| `unlock` | Buka kunci dari jarak jauh |
| `status` | Status kunci & baterai |
| `setpin` | Ganti PIN rahasia mode teks |
| `unpair` | Hapus pasangan ini dari HP anak |
| `devices` | Tampilkan token tersimpan |

Opsi umum: `--host`, `--port` (default `8787`), `--timeout` (default `2s`),
`--token`, `--token-file` (default `~/.kuncidong-tokens.json`), `--json`.

## Contoh: uji cepat lewat USB (tanpa Wi-Fi)

Cara tercepat — pakai `adb forward` sehingga PC mengakses port HP lewat `127.0.0.1`:

```bash
# 1. HP anak terhubung USB + USB debugging aktif
adb forward tcp:8787 tcp:8787

# 2. Di HP: buka app -> pilih peran "Anak" -> aktifkan izin & layanan
#    -> buka layar "Pairing" (kode 6 digit muncul)

# 3. Cek koneksi
go run . hello --host 127.0.0.1

# 4. Pairing (ganti 123456 dengan kode di layar HP, 4321 = PIN rahasia)
go run . pair --host 127.0.0.1 --code 123456 --pin 4321 --name Ayah

# 5. Kunci dengan pesan teks
go run . lock --host 127.0.0.1 --mode text --text "Belajar dulu ya!"

# 6. Kunci dengan kuis
go run . lock --host 127.0.0.1 --mode quiz

# 7. Buka kunci
go run . unlock --host 127.0.0.1

# 8. Cek status
go run . status --host 127.0.0.1
```

## Contoh: lewat Wi-Fi

Pastikan PC dan HP anak di Wi-Fi yang sama (dan jaringan tidak memakai
*AP/client isolation*).

```bash
go run . scan                                  # temukan IP HP anak
go run . pair --host 192.168.1.20 --code 123456 --pin 4321 --name Ayah
go run . lock --host 192.168.1.20 --mode text --text "Waktunya tidur 🌙"
```

Setelah pairing, token disimpan sehingga perintah berikutnya cukup memakai `--host`
(mis. `go run . unlock --host 192.168.1.20`).

## Catatan

- **PIN rahasia** yang dimasukkan saat pairing disimpan (ter-hash) di **HP anak**,
  bukan di tool ini. PIN itu dipakai anak untuk membuka mode teks.
- Token memberi kendali penuh atas HP anak; file token disimpan dengan izin `0600`.
- `scan` memindai 254 alamat per subnet — bisa memakan waktu beberapa detik.
- Perintah `unlock` membuka kunci **tanpa** PIN (ditujukan untuk orang tua).
