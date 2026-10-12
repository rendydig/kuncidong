package com.rendy.customlockscreen

import com.rendy.customlockscreen.net.Cmd
import com.rendy.customlockscreen.net.LockMode
import com.rendy.customlockscreen.net.PROTOCOL_VERSION
import com.rendy.customlockscreen.net.Protocol
import com.rendy.customlockscreen.net.RemoteRequest
import com.rendy.customlockscreen.net.RemoteResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {

    @Test
    fun hello_request_round_trips() {
        val decoded = Protocol.decodeRequest(Protocol.encode(RemoteRequest(cmd = Cmd.HELLO)))

        assertNotNull(decoded)
        assertEquals(Cmd.HELLO, decoded!!.cmd)
        assertEquals(PROTOCOL_VERSION, decoded.v)
    }

    @Test
    fun lock_request_round_trips_all_fields() {
        val request = RemoteRequest(
            cmd = Cmd.LOCK,
            token = "token-123",
            mode = LockMode.TEXT,
            title = "Judul",
            text = "Pesan panjang untuk anak",
            pack = "KELAS_2"
        )

        val encoded = Protocol.encode(request)
        assertTrue("messages must be newline delimited", encoded.endsWith("\n"))

        val decoded = Protocol.decodeRequest(encoded)!!
        assertEquals(Cmd.LOCK, decoded.cmd)
        assertEquals("token-123", decoded.token)
        assertEquals(LockMode.TEXT, decoded.mode)
        assertEquals("Judul", decoded.title)
        assertEquals("Pesan panjang untuk anak", decoded.text)
        assertEquals("KELAS_2", decoded.pack)
    }

    @Test
    fun pair_request_round_trips() {
        val decoded = Protocol.decodeRequest(
            Protocol.encode(RemoteRequest(cmd = Cmd.PAIR, code = "123456", pin = "4321", parent = "Ayah"))
        )!!

        assertEquals("123456", decoded.code)
        assertEquals("4321", decoded.pin)
        assertEquals("Ayah", decoded.parent)
    }

    @Test
    fun response_round_trips() {
        val response = RemoteResponse(ok = true, device = "Anak-A", paired = false, needsPair = true)
        val decoded = Protocol.decodeResponse(Protocol.encode(response))!!

        assertTrue(decoded.ok)
        assertEquals("Anak-A", decoded.device)
        assertEquals(false, decoded.paired)
        assertEquals(true, decoded.needsPair)
    }

    @Test
    fun malformed_json_returns_null() {
        assertNull(Protocol.decodeRequest("not json at all"))
        assertNull(Protocol.decodeResponse("{"))
    }

    @Test
    fun blank_or_missing_command_returns_null() {
        assertNull(Protocol.decodeRequest(""))
        assertNull(Protocol.decodeRequest("   "))
        assertNull(Protocol.decodeRequest("{\"v\":1}"))
        assertNull(Protocol.decodeRequest("{\"v\":1,\"cmd\":\"\"}"))
    }
}
