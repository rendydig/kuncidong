package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"strings"
	"time"
)

// commonOpts holds the flags shared by most commands.
type commonOpts struct {
	host      string
	port      int
	timeout   time.Duration
	token     string
	tokenFile string
	jsonOut   bool
}

func addCommon(fs *flag.FlagSet, o *commonOpts, needToken bool) {
	fs.StringVar(&o.host, "host", "127.0.0.1", "alamat IP HP anak (127.0.0.1 untuk adb forward)")
	fs.IntVar(&o.port, "port", DefaultPort, "port kontrol HP anak")
	fs.DurationVar(&o.timeout, "timeout", 2*time.Second, "timeout koneksi per perintah")
	fs.StringVar(&o.tokenFile, "token-file", defaultTokenFile(), "file penyimpanan token")
	fs.BoolVar(&o.jsonOut, "json", false, "cetak respons JSON mentah")
	if needToken {
		fs.StringVar(&o.token, "token", "", "token autentikasi (opsional bila sudah tersimpan)")
	}
}

func (o commonOpts) client() Client {
	return Client{Host: o.host, Port: o.port, Timeout: o.timeout}
}

func (o commonOpts) store() TokenStore {
	return TokenStore{Path: o.tokenFile}
}

// resolveToken prefers --token, then the saved token for this host:port.
func (o commonOpts) resolveToken() (string, error) {
	if t := strings.TrimSpace(o.token); t != "" {
		return t, nil
	}
	if t := o.store().find(o.host, o.port); t != "" {
		return t, nil
	}
	return "", fmt.Errorf(
		"belum ada token untuk %s:%d — jalankan `pair` dulu atau berikan --token",
		o.host, o.port,
	)
}

func main() {
	if len(os.Args) < 2 {
		usage()
		os.Exit(2)
	}

	cmd := os.Args[1]
	args := os.Args[2:]

	var err error
	switch cmd {
	case "scan":
		err = cmdScan(args)
	case "hello":
		err = cmdHello(args)
	case "pair":
		err = cmdPair(args)
	case "lock":
		err = cmdLock(args)
	case "unlock":
		err = cmdUnlock(args)
	case "status":
		err = cmdStatus(args)
	case "setpin":
		err = cmdSetPin(args)
	case "unpair":
		err = cmdUnpair(args)
	case "devices":
		err = cmdDevices(args)
	case "help", "-h", "--help":
		usage()
		return
	default:
		fmt.Fprintf(os.Stderr, "perintah tidak dikenal: %q\n\n", cmd)
		usage()
		os.Exit(2)
	}

	if err != nil {
		fmt.Fprintln(os.Stderr, "❌ "+err.Error())
		os.Exit(1)
	}
}

func usage() {
	fmt.Fprint(os.Stderr, `kuncidong-parent — client "Orang Tua" untuk mengunci HP anak dari PC

Pemakaian:
  kuncidong-parent <perintah> [opsi]

Perintah:
  scan      Cari HP anak di subnet Wi-Fi lokal (kirim hello ke 1-254)
  hello     Tampilkan info HP anak (nama, sudah dipasangkan?)
  pair      Pasangkan dengan HP anak memakai kode 6 digit dari layar HP
  lock      Kunci HP anak (--mode quiz|text)
  unlock    Buka kunci HP anak dari jarak jauh
  status    Tampilkan status kunci & baterai HP anak
  setpin    Ganti PIN rahasia untuk membuka mode teks
  unpair    Hapus pasangan ini dari HP anak
  devices   Tampilkan token yang tersimpan

Opsi umum:
  --host <ip>         alamat HP anak (default 127.0.0.1, cocok untuk adb forward)
  --port <n>          port kontrol (default 8787)
  --timeout <durasi>  timeout koneksi (default 2s)
  --token <str>       token autentikasi (opsional bila sudah tersimpan)
  --token-file <p>    file penyimpanan token (default ~/.kuncidong-tokens.json)
  --json              cetak respons JSON mentah

Contoh:
  adb forward tcp:8787 tcp:8787
  kuncidong-parent hello  --host 127.0.0.1
  kuncidong-parent pair   --host 127.0.0.1 --code 123456 --pin 4321 --name Ayah
  kuncidong-parent lock   --host 127.0.0.1 --mode text --text "Belajar dulu ya!"
  kuncidong-parent lock   --host 127.0.0.1 --mode quiz
  kuncidong-parent unlock --host 127.0.0.1
`)
}

// region output helpers

func printRaw(resp *Response, jsonOut bool) {
	if !jsonOut {
		return
	}
	out, err := json.MarshalIndent(resp, "", "  ")
	if err != nil {
		return
	}
	fmt.Println(string(out))
}

// errText turns a protocol error code into a readable Indonesian message.
func errText(code string) string {
	switch code {
	case "bad_request":
		return "permintaan tidak dikenal"
	case "bad_code":
		return "kode pairing salah atau sudah kedaluwarsa (buka layar Pairing di HP anak)"
	case "bad_pin":
		return "PIN minimal 4 angka"
	case "needs_token":
		return "perintah ini butuh token — jalankan `pair` dulu"
	case "bad_token":
		return "token tidak valid (mungkin sudah di-unpair)"
	case "not_paired":
		return "HP anak belum dipasangkan dengan Anda"
	case "":
		return "tidak ada keterangan"
	default:
		return code
	}
}

func checkOK(resp *Response, jsonOut bool) error {
	printRaw(resp, jsonOut)
	if !resp.OK {
		return fmt.Errorf("ditolak server: %s", errText(resp.Err))
	}
	return nil
}

// endregion

// region commands

func cmdScan(args []string) error {
	fs := flag.NewFlagSet("scan", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, false)
	concurrency := fs.Int("concurrency", 32, "jumlah koneksi paralel saat scan")
	if err := fs.Parse(args); err != nil {
		return err
	}

	fmt.Printf("🔎 Memindai subnet lokal pada port %d…\n", o.port)
	results := ScanSubnet(o.port, o.timeout, *concurrency)

	if len(results) == 0 {
		fmt.Println("Tidak ada HP anak yang menjawab. Pastikan:")
		fmt.Println("  • HP anak & PC berada di Wi-Fi yang sama (bukan AP isolation)")
		fmt.Println("  • layanan 'Kontrol Orang Tua' aktif di app anak")
		fmt.Println("  • atau pakai `adb forward tcp:8787 tcp:8787` lalu --host 127.0.0.1")
		return nil
	}

	fmt.Printf("Ditemukan %d HP anak:\n", len(results))
	for _, r := range results {
		status := "belum dipasangkan"
		if r.Paired {
			status = "sudah dipasangkan"
		}
		fmt.Printf("  • %-15s  %-24s  [%s]\n", r.Host, r.Device, status)
	}
	return nil
}

func cmdHello(args []string) error {
	fs := flag.NewFlagSet("hello", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, false)
	if err := fs.Parse(args); err != nil {
		return err
	}

	resp, err := o.client().Hello()
	if err != nil {
		return err
	}
	if err := checkOK(resp, o.jsonOut); err != nil {
		return err
	}

	fmt.Printf("📱 %s di %s:%d\n", resp.Device, o.host, o.port)
	fmt.Printf("   Dipasangkan : %s\n", yesNo(resp.Paired))
	fmt.Printf("   Perlu PIN   : %s\n", yesNo(resp.NeedsPair))
	return nil
}

func cmdPair(args []string) error {
	fs := flag.NewFlagSet("pair", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, false)
	code := fs.String("code", "", "kode 6 digit dari layar Pairing di HP anak (wajib)")
	pin := fs.String("pin", "", "PIN rahasia baru untuk membuka mode teks (min. 4 angka, wajib)")
	name := fs.String("name", "Ayah", "nama Anda yang tampil di HP anak")
	save := fs.Bool("save", true, "simpan token ke file agar perintah berikutnya tidak perlu --token")
	if err := fs.Parse(args); err != nil {
		return err
	}

	if len(strings.TrimSpace(*code)) != 6 {
		return fmt.Errorf("--code harus 6 digit (lihat layar Pairing di HP anak)")
	}
	if len(strings.TrimSpace(*pin)) < 4 {
		return fmt.Errorf("--pin minimal 4 angka")
	}

	resp, err := o.client().Pair(strings.TrimSpace(*code), strings.TrimSpace(*pin), strings.TrimSpace(*name))
	if err != nil {
		return err
	}
	if err := checkOK(resp, o.jsonOut); err != nil {
		return err
	}
	if strings.TrimSpace(resp.Token) == "" {
		return fmt.Errorf("server tidak mengirim token")
	}

	device := resp.Device
	if device == "" {
		device = o.host
	}

	if *save {
		entry := StoredToken{Name: device, Host: o.host, Port: o.port, Token: resp.Token}
		if err := o.store().upsert(entry); err != nil {
			return err
		}
		fmt.Printf("✅ Terpasang dengan %s. Token disimpan di %s\n", device, o.tokenFile)
	} else {
		fmt.Printf("✅ Terpasang dengan %s\n", device)
	}
	fmt.Printf("   Token: %s\n", resp.Token)
	fmt.Println("   (PIN rahasia disimpan di HP anak — catat bila perlu)")
	return nil
}

func cmdLock(args []string) error {
	fs := flag.NewFlagSet("lock", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, true)
	mode := fs.String("mode", ModeQuiz, "mode kunci: quiz | text")
	title := fs.String("title", "", "judul pesan (mode text, opsional)")
	text := fs.String("text", "", "isi pesan untuk anak (mode text)")
	pack := fs.String("pack", "", "paket soal (mode quiz, opsional)")
	if err := fs.Parse(args); err != nil {
		return err
	}

	m := strings.ToLower(strings.TrimSpace(*mode))
	if m != ModeQuiz && m != ModeText {
		return fmt.Errorf("--mode harus 'quiz' atau 'text'")
	}
	if m == ModeText && strings.TrimSpace(*text) == "" {
		return fmt.Errorf("mode text butuh --text (pesan untuk anak)")
	}

	token, err := o.resolveToken()
	if err != nil {
		return err
	}

	resp, err := o.client().Lock(token, m, strings.TrimSpace(*title), strings.TrimSpace(*text), strings.TrimSpace(*pack))
	if err != nil {
		return err
	}
	if err := checkOK(resp, o.jsonOut); err != nil {
		return err
	}

	if m == ModeText {
		fmt.Println("🔒 HP anak terkunci dengan pesan teks.")
	} else {
		fmt.Println("🔒 HP anak terkunci dengan soal kuis.")
	}
	return nil
}

func cmdUnlock(args []string) error {
	fs := flag.NewFlagSet("unlock", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, true)
	if err := fs.Parse(args); err != nil {
		return err
	}

	token, err := o.resolveToken()
	if err != nil {
		return err
	}

	resp, err := o.client().Unlock(token)
	if err != nil {
		return err
	}
	if err := checkOK(resp, o.jsonOut); err != nil {
		return err
	}
	fmt.Println("🔓 HP anak dibuka.")
	return nil
}

func cmdStatus(args []string) error {
	fs := flag.NewFlagSet("status", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, true)
	if err := fs.Parse(args); err != nil {
		return err
	}

	token, err := o.resolveToken()
	if err != nil {
		return err
	}

	resp, err := o.client().Status(token)
	if err != nil {
		return err
	}
	if err := checkOK(resp, o.jsonOut); err != nil {
		return err
	}

	state := "terbuka"
	if resp.Locked != nil && *resp.Locked {
		state = "terkunci"
	}
	fmt.Printf("📊 Status HP anak (%s:%d): %s", o.host, o.port, state)
	if resp.Mode != "" {
		fmt.Printf(" (mode: %s)", resp.Mode)
	}
	if resp.Battery != nil && *resp.Battery >= 0 {
		fmt.Printf(", baterai %d%%", *resp.Battery)
	}
	fmt.Println()
	return nil
}

func cmdSetPin(args []string) error {
	fs := flag.NewFlagSet("setpin", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, true)
	pin := fs.String("pin", "", "PIN rahasia baru (min. 4 angka)")
	if err := fs.Parse(args); err != nil {
		return err
	}
	if len(strings.TrimSpace(*pin)) < 4 {
		return fmt.Errorf("--pin minimal 4 angka")
	}

	token, err := o.resolveToken()
	if err != nil {
		return err
	}

	resp, err := o.client().SetPin(token, strings.TrimSpace(*pin))
	if err != nil {
		return err
	}
	if err := checkOK(resp, o.jsonOut); err != nil {
		return err
	}
	fmt.Println("🔑 PIN rahasia diperbarui di HP anak.")
	return nil
}

func cmdUnpair(args []string) error {
	fs := flag.NewFlagSet("unpair", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, true)
	if err := fs.Parse(args); err != nil {
		return err
	}

	token, err := o.resolveToken()
	if err != nil {
		return err
	}

	resp, err := o.client().Unpair(token)
	if err != nil {
		return err
	}
	if err := checkOK(resp, o.jsonOut); err != nil {
		return err
	}
	fmt.Println("🗑️  Pasangan dihapus dari HP anak.")
	fmt.Println("   (token lokal masih tersimpan; hapus manual bila perlu)")
	return nil
}

func cmdDevices(args []string) error {
	fs := flag.NewFlagSet("devices", flag.ContinueOnError)
	var o commonOpts
	addCommon(fs, &o, false)
	if err := fs.Parse(args); err != nil {
		return err
	}

	tokens, err := o.store().list()
	if err != nil {
		return err
	}
	if len(tokens) == 0 {
		fmt.Printf("Belum ada HP anak tersimpan di %s\n", o.tokenFile)
		return nil
	}
	fmt.Printf("HP anak tersimpan (%s):\n", o.tokenFile)
	for _, t := range tokens {
		fmt.Printf("  • %-24s %s:%d\n", t.Name, t.Host, t.Port)
	}
	return nil
}

// endregion

func yesNo(v *bool) string {
	if v == nil {
		return "tidak diketahui"
	}
	if *v {
		return "ya"
	}
	return "tidak"
}
