package com.noorpro.app.calls

import org.junit.Assert.*
import org.junit.Test

class IceConfigTest {
    private val cf = """{"iceServers":[{"urls":["stun:stun.cloudflare.com:3478"]},
        {"urls":["turn:turn.cloudflare.com:3478?transport=udp","turns:turn.cloudflare.com:443?transport=tcp"],
         "username":"u","credential":"c"}]}"""

    @Test fun missingOrBadJsonFallsBackToStunDefault() {
        assertEquals(IceConfig.DEFAULT, IceConfig.merge(null, null, 0))
        assertEquals(IceConfig.DEFAULT, IceConfig.merge("", null, 0))
        assertEquals(IceConfig.DEFAULT, IceConfig.merge("{not json", null, 0))
        assertEquals(IceConfig.DEFAULT, IceConfig.merge("[]", null, 0))
        assertFalse(IceConfig.DEFAULT.any { it.isTurn })
    }

    @Test fun defaultJsonMatchesDefaultList() {
        assertEquals(IceConfig.DEFAULT, IceConfig.parse(IceConfig.DEFAULT_ICE_SERVERS_JSON))
    }

    @Test fun parsesArrayAndObjectAndStringUrl() {
        val a = IceConfig.parse("""[{"urls":"stun:a:1"},{"url":"stun:b:2"}]""")!!
        assertEquals(listOf(listOf("stun:a:1"), listOf("stun:b:2")), a.map { it.urls })
        val o = IceConfig.parse(cf)!!
        assertEquals(2, o.size); assertTrue(o[1].isTurn); assertEquals("u", o[1].username)
    }

    @Test fun dropsInvalidUrlsAndTurnWithoutCredentials() {
        val r = IceConfig.parse("""[{"urls":["http://x","stun:ok:1"]},{"urls":"turn:t:3478"},{"urls":5}]""")!!
        assertEquals(listOf(IceSpec(listOf("stun:ok:1"))), r)
    }

    @Test fun credsKeepOnlyTurnAndHonourTtl() {
        val c = IceConfig.parseCreds(cf.replace("{\"iceServers\"", "{\"ttl\":600,\"iceServers\""), 1_000)!!
        assertEquals(1, c.servers.size); assertEquals(601_000L, c.expiresAtMs)
        val d = IceConfig.parseCreds(cf, 0)!!
        assertEquals(IceConfig.DEFAULT_TTL_MS, d.expiresAtMs)
        assertNull(IceConfig.parseCreds("""{"iceServers":[{"urls":"stun:x:1"}]}""", 0))
        assertNull(IceConfig.parseCreds("oops", 0))
    }

    @Test fun mergeAddsCredsOnlyWhileValid() {
        val c = IceConfig.parseCreds(cf, 0)!!
        assertTrue(IceConfig.merge(null, c, 0).any { it.isTurn })
        assertFalse(IceConfig.merge(null, c, c.expiresAtMs - IceConfig.REFRESH_MARGIN_MS).any { it.isTurn })
    }
}
