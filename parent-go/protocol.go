package main

// ProtocolVersion must match PROTOCOL_VERSION in the Android app's net/Protocol.kt.
const ProtocolVersion = 1

// DefaultPort must match DEFAULT_PORT in net/Protocol.kt.
const DefaultPort = 8787

// Commands understood by the Kids device server.
const (
	CmdHello  = "hello"
	CmdPair   = "pair"
	CmdLock   = "lock"
	CmdUnlock = "unlock"
	CmdStatus = "status"
	CmdSetPin = "setpin"
	CmdUnpair = "unpair"
)

// Lock modes supported by the Kids device.
const (
	ModeQuiz = "quiz"
	ModeText = "text"
)

// Request mirrors RemoteRequest in net/Protocol.kt. Field names (JSON keys) must
// stay identical because the server decodes them with Gson.
type Request struct {
	V      int    `json:"v"`
	Cmd    string `json:"cmd"`
	Code   string `json:"code,omitempty"`
	Pin    string `json:"pin,omitempty"`
	Parent string `json:"parent,omitempty"`
	Token  string `json:"token,omitempty"`
	Mode   string `json:"mode,omitempty"`
	Title  string `json:"title,omitempty"`
	Text   string `json:"text,omitempty"`
	Pack   string `json:"pack,omitempty"`
}

// Response mirrors RemoteResponse in net/Protocol.kt. Pointer fields keep the
// difference between "absent" and "false/0".
type Response struct {
	V         int    `json:"v"`
	OK        bool   `json:"ok"`
	Err       string `json:"err,omitempty"`
	Device    string `json:"device,omitempty"`
	Paired    *bool  `json:"paired,omitempty"`
	NeedsPair *bool  `json:"needsPair,omitempty"`
	Token     string `json:"token,omitempty"`
	Locked    *bool  `json:"locked,omitempty"`
	Mode      string `json:"mode,omitempty"`
	Battery   *int   `json:"battery,omitempty"`
}

// newRequest creates a request with the current protocol version filled in.
func newRequest(cmd string) Request {
	return Request{V: ProtocolVersion, Cmd: cmd}
}
