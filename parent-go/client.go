package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"strconv"
	"time"
)

// Client sends one request per connection and reads the single-line response,
// matching RemoteControlServer's request/response behaviour.
type Client struct {
	Host    string
	Port    int
	Timeout time.Duration
}

func (c Client) addr() string {
	return net.JoinHostPort(c.Host, strconv.Itoa(c.Port))
}

// exchange dials the Kids device, writes one newline-terminated JSON request and
// decodes the one-line JSON response.
func (c Client) exchange(req Request) (*Response, error) {
	conn, err := net.DialTimeout("tcp", c.addr(), c.Timeout)
	if err != nil {
		return nil, fmt.Errorf("tidak bisa terhubung ke %s (%v)", c.addr(), err)
	}
	defer conn.Close()

	_ = conn.SetDeadline(time.Now().Add(c.Timeout))

	payload, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("gagal menyusun permintaan: %w", err)
	}
	payload = append(payload, '\n')

	if _, err := conn.Write(payload); err != nil {
		return nil, fmt.Errorf("gagal mengirim perintah: %w", err)
	}

	line, err := bufio.NewReader(conn).ReadBytes('\n')
	if err != nil && len(line) == 0 {
		return nil, fmt.Errorf("tidak ada respons dari %s (%v)", c.addr(), err)
	}

	var resp Response
	if err := json.Unmarshal(line, &resp); err != nil {
		return nil, fmt.Errorf("respons tidak valid dari %s: %w", c.addr(), err)
	}
	return &resp, nil
}

// Hello asks for device info. It needs no token, so it doubles as a reachability
// probe for the subnet scanner.
func (c Client) Hello() (*Response, error) {
	return c.exchange(newRequest(CmdHello))
}

// Pair exchanges the 6 digit code shown on the Kids device for an auth token.
func (c Client) Pair(code, pin, parentName string) (*Response, error) {
	req := newRequest(CmdPair)
	req.Code = code
	req.Pin = pin
	req.Parent = parentName
	return c.exchange(req)
}

// Lock asks the Kids device to lock itself in the given mode.
func (c Client) Lock(token, mode, title, text, pack string) (*Response, error) {
	req := newRequest(CmdLock)
	req.Token = token
	req.Mode = mode
	req.Title = title
	req.Text = text
	req.Pack = pack
	return c.exchange(req)
}

// Unlock releases the lock without requiring the PIN on the Kids device.
func (c Client) Unlock(token string) (*Response, error) {
	req := newRequest(CmdUnlock)
	req.Token = token
	return c.exchange(req)
}

// Status reports the current lock state and battery level.
func (c Client) Status(token string) (*Response, error) {
	req := newRequest(CmdStatus)
	req.Token = token
	return c.exchange(req)
}

// SetPin replaces the secret PIN used to unlock text mode on the Kids device.
func (c Client) SetPin(token, pin string) (*Response, error) {
	req := newRequest(CmdSetPin)
	req.Token = token
	req.Pin = pin
	return c.exchange(req)
}

// Unpair revokes this parent's token on the Kids device.
func (c Client) Unpair(token string) (*Response, error) {
	req := newRequest(CmdUnpair)
	req.Token = token
	return c.exchange(req)
}
