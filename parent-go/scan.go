package main

import (
	"net"
	"sort"
	"strconv"
	"sync"
	"time"
)

// ScanResult is a Kids device that answered a hello probe.
type ScanResult struct {
	Host    string
	Port    int
	Device  string
	Paired  bool
	NeedsPA bool
}

// localIPv4Prefixes returns the /24 prefixes of every site-local IPv4 address,
// e.g. "192.168.1". Loopback and link-local interfaces are skipped.
func localIPv4Prefixes() []string {
	var prefixes []string
	seen := map[string]bool{}

	interfaces, err := net.Interfaces()
	if err != nil {
		return nil
	}
	for _, iface := range interfaces {
		if iface.Flags&net.FlagUp == 0 || iface.Flags&net.FlagLoopback != 0 {
			continue
		}
		addrs, err := iface.Addrs()
		if err != nil {
			continue
		}
		for _, addr := range addrs {
			ipNet, ok := addr.(*net.IPNet)
			if !ok {
				continue
			}
			ip4 := ipNet.IP.To4()
			if ip4 == nil || !ip4.IsPrivate() {
				continue
			}
			prefix := ip4.Mask(net.CIDRMask(24, 32)).String()
			if !seen[prefix] {
				seen[prefix] = true
				prefixes = append(prefixes, prefix)
			}
		}
	}
	sort.Strings(prefixes)
	return prefixes
}

// ScanSubnet probes every host in the local /24 subnet(s) on the control port
// and returns the devices that answer hello. Concurrency is capped so the scan
// stays quick without exhausting sockets.
func ScanSubnet(port int, timeout time.Duration, concurrency int) []ScanResult {
	prefixes := localIPv4Prefixes()
	if len(prefixes) == 0 {
		return nil
	}
	if concurrency <= 0 {
		concurrency = 32
	}

	type job struct {
		host string
	}
	jobs := make(chan job)
	var (
		mu      sync.Mutex
		results []ScanResult
		wg      sync.WaitGroup
	)

	worker := func() {
		defer wg.Done()
		for j := range jobs {
			client := Client{Host: j.host, Port: port, Timeout: timeout}
			resp, err := client.Hello()
			if err != nil || resp == nil || !resp.OK {
				continue
			}
			mu.Lock()
			results = append(results, ScanResult{
				Host:    j.host,
				Port:    port,
				Device:  resp.Device,
				Paired:  resp.Paired != nil && *resp.Paired,
				NeedsPA: resp.NeedsPair != nil && *resp.NeedsPair,
			})
			mu.Unlock()
		}
	}

	for i := 0; i < concurrency; i++ {
		wg.Add(1)
		go worker()
	}

	for _, prefix := range prefixes {
		for host := 1; host <= 254; host++ {
			jobs <- job{host: net.JoinHostPort(prefix, strconv.Itoa(host))}
		}
	}
	close(jobs)
	wg.Wait()

	sort.Slice(results, func(i, j int) bool { return results[i].Host < results[j].Host })
	return results
}
