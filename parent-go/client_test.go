package main

import (
	"bufio"
	"encoding/json"
	"net"
	"path/filepath"
	"testing"
	"time"
)

// mockServer starts a one-command-per-connection TCP server that mirrors the
// Android RemoteControlServer behaviour, then returns its address.
func mockServer(t *testing.T, handler func(Request) Response) (string, int, func()) {
	t.Helper()

	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("listen: %v", err)
	}

	go func() {
		for {
			conn, err := ln.Accept()
			if err != nil {
				return
			}
			go func(c net.Conn) {
				defer c.Close()
				line, err := bufio.NewReader(c).ReadBytes('\n')
				if err != nil {
					return
				}
				var req Request
				if err := json.Unmarshal(line, &req); err != nil {
					return
				}
				out, err := json.Marshal(handler(req))
				if err != nil {
					return
				}
				_, _ = c.Write(append(out, '\n'))
			}(conn)
		}
	}()

	addr := ln.Addr().(*net.TCPAddr)
	return "127.0.0.1", addr.Port, func() { _ = ln.Close() }
}

func newTestClient(host string, port int) Client {
	return Client{Host: host, Port: port, Timeout: 2 * time.Second}
}

func TestHelloParsesDeviceInfo(t *testing.T) {
	paired, needs := false, true
	host, port, stop := mockServer(t, func(req Request) Response {
		if req.Cmd != CmdHello {
			t.Errorf("expected hello, got %q", req.Cmd)
		}
		if req.V != ProtocolVersion {
			t.Errorf("expected version %d, got %d", ProtocolVersion, req.V)
		}
		return Response{V: ProtocolVersion, OK: true, Device: "Anak-Pixel-3a", Paired: &paired, NeedsPair: &needs}
	})
	defer stop()

	resp, err := newTestClient(host, port).Hello()
	if err != nil {
		t.Fatalf("hello: %v", err)
	}
	if !resp.OK || resp.Device != "Anak-Pixel-3a" {
		t.Fatalf("unexpected response: %+v", resp)
	}
	if resp.Paired == nil || *resp.Paired {
		t.Errorf("expected paired=false")
	}
	if resp.NeedsPair == nil || !*resp.NeedsPair {
		t.Errorf("expected needsPair=true")
	}
}

func TestLockSendsAllFields(t *testing.T) {
	var got Request
	host, port, stop := mockServer(t, func(req Request) Response {
		got = req
		locked := true
		return Response{V: ProtocolVersion, OK: true, Locked: &locked, Mode: req.Mode}
	})
	defer stop()

	resp, err := newTestClient(host, port).
		Lock("tok-1", ModeText, "Judul", "Belajar dulu ya!", "")
	if err != nil {
		t.Fatalf("lock: %v", err)
	}
	if !resp.OK {
		t.Fatalf("expected ok, got %+v", resp)
	}

	if got.Cmd != CmdLock {
		t.Errorf("cmd = %q, want lock", got.Cmd)
	}
	if got.Token != "tok-1" {
		t.Errorf("token = %q", got.Token)
	}
	if got.Mode != ModeText {
		t.Errorf("mode = %q", got.Mode)
	}
	if got.Title != "Judul" || got.Text != "Belajar dulu ya!" {
		t.Errorf("title/text = %q / %q", got.Title, got.Text)
	}
}

func TestPairReturnsTokenAndRejectionIsReported(t *testing.T) {
	host, port, stop := mockServer(t, func(req Request) Response {
		if req.Code == "000000" {
			return Response{V: ProtocolVersion, OK: false, Err: "bad_code"}
		}
		return Response{V: ProtocolVersion, OK: true, Device: "Anak-A", Token: "abc123"}
	})
	defer stop()

	client := newTestClient(host, port)

	ok, err := client.Pair("123456", "4321", "Ayah")
	if err != nil {
		t.Fatalf("pair: %v", err)
	}
	if !ok.OK || ok.Token != "abc123" {
		t.Fatalf("unexpected pair response: %+v", ok)
	}

	bad, err := client.Pair("000000", "4321", "Ayah")
	if err != nil {
		t.Fatalf("pair: %v", err)
	}
	if bad.OK || bad.Err != "bad_code" {
		t.Fatalf("expected bad_code rejection, got %+v", bad)
	}
}

func TestExchangeFailsWhenNothingListens(t *testing.T) {
	// Port 1 is privileged and nothing listens there in tests.
	_, err := Client{Host: "127.0.0.1", Port: 1, Timeout: 200 * time.Millisecond}.Hello()
	if err == nil {
		t.Fatal("expected a connection error")
	}
}

func TestTokenStoreUpsertAndFind(t *testing.T) {
	store := TokenStore{Path: filepath.Join(t.TempDir(), "tokens.json")}

	if got := store.find("192.168.1.20", 8787); got != "" {
		t.Fatalf("expected empty token, got %q", got)
	}

	entry := StoredToken{Name: "Anak-A", Host: "192.168.1.20", Port: 8787, Token: "tok-a"}
	if err := store.upsert(entry); err != nil {
		t.Fatalf("upsert: %v", err)
	}
	if got := store.find("192.168.1.20", 8787); got != "tok-a" {
		t.Fatalf("find = %q, want tok-a", got)
	}

	// Re-pairing the same address must replace, not duplicate.
	entry.Token = "tok-b"
	if err := store.upsert(entry); err != nil {
		t.Fatalf("upsert: %v", err)
	}
	all, err := store.list()
	if err != nil {
		t.Fatalf("list: %v", err)
	}
	if len(all) != 1 || all[0].Token != "tok-b" {
		t.Fatalf("expected single replaced entry, got %+v", all)
	}
}
