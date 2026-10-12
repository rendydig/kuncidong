package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sort"
)

// StoredToken is one paired Kids device remembered by this CLI.
type StoredToken struct {
	Name  string `json:"name"`
	Host  string `json:"host"`
	Port  int    `json:"port"`
	Token string `json:"token"`
}

// TokenStore persists auth tokens so later commands do not need --token.
type TokenStore struct {
	Path string
}

func defaultTokenFile() string {
	home, err := os.UserHomeDir()
	if err != nil || home == "" {
		return "tokens.json"
	}
	return filepath.Join(home, ".kuncidong-tokens.json")
}

func (s TokenStore) load() ([]StoredToken, error) {
	raw, err := os.ReadFile(s.Path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, fmt.Errorf("gagal membaca %s: %w", s.Path, err)
	}
	var tokens []StoredToken
	if err := json.Unmarshal(raw, &tokens); err != nil {
		return nil, fmt.Errorf("file token rusak (%s): %w", s.Path, err)
	}
	return tokens, nil
}

func (s TokenStore) save(tokens []StoredToken) error {
	raw, err := json.MarshalIndent(tokens, "", "  ")
	if err != nil {
		return err
	}
	// 0600: the token grants full control over the Kids device.
	if err := os.WriteFile(s.Path, raw, 0o600); err != nil {
		return fmt.Errorf("gagal menyimpan token ke %s: %w", s.Path, err)
	}
	return nil
}

// upsert inserts or replaces the entry matching host:port.
func (s TokenStore) upsert(entry StoredToken) error {
	tokens, err := s.load()
	if err != nil {
		return err
	}
	replaced := false
	for i, t := range tokens {
		if t.Host == entry.Host && t.Port == entry.Port {
			tokens[i] = entry
			replaced = true
			break
		}
	}
	if !replaced {
		tokens = append(tokens, entry)
	}
	return s.save(tokens)
}

// find returns the stored token for host:port, or "" when absent.
func (s TokenStore) find(host string, port int) string {
	tokens, err := s.load()
	if err != nil {
		return ""
	}
	for _, t := range tokens {
		if t.Host == host && t.Port == port {
			return t.Token
		}
	}
	return ""
}

func (s TokenStore) list() ([]StoredToken, error) {
	tokens, err := s.load()
	if err != nil {
		return nil, err
	}
	sort.Slice(tokens, func(i, j int) bool { return tokens[i].Name < tokens[j].Name })
	return tokens, nil
}
