package com.spendingbuddy.app.p2p

import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.Group
import com.spendingbuddy.app.Member
import com.spendingbuddy.app.PeerEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class IrohTransportIntegrationTest {
    @Test fun minimalEndpointsExchangePairAndSyncAndRefuseThirdPeer() = runBlocking<Unit> {
        val left = IrohTransport.bind(minimal = true)
        val right = IrohTransport.bind(minimal = true)
        try {
            val server = async {
                val incoming = right.acceptNext() ?: error("right endpoint closed")
                val conn = right.accept(incoming)
                conn.serve { bytes ->
                    val request = decodeMessage(bytes)
                    assertEquals("join_request", request.type)
                    assertEquals("join-nonce-0123456789", request.joinNonce)
                    encodeMessage(SyncMessage(type = "join_pending", sender = "Ravi", groupId = "g1", expiresAt = 5))
                }
            }
            val client = left.connect(right.ticket)
            val pairReply = decodeMessage(client.exchange(encodeMessage(SyncMessage(type = "join_request", sender = "Asha", groupId = "g1", joinNonce = "join-nonce-0123456789", members = listOf(Member("b2", "Asha"))))))
            assertEquals("join_pending", pairReply.type)
            server.await()
            client.close()

            val syncServer = async {
                val incoming = right.acceptNext() ?: error("right endpoint closed")
                val conn = right.accept(incoming)
                conn.serve { bytes ->
                    val request = decodeMessage(bytes)
                    assertEquals("nudge", request.events.single().type)
                    encodeMessage(SyncMessage(type = "sync", sender = "Ravi", acks = listOf(request.events.single().id)))
                }
            }
            val syncClient = left.connect(right.ticket)
            val event = PeerEvent(id = "event-1", type = "nudge", minutes = 30, target = "a1")
            val asha = BuddyState(name = "Asha", selfId = "b2", group = Group("g1", "Crew", "s"), members = listOf(Member("b2", "Asha"), Member("a1", "Ravi")), outbox = listOf(event))
            val syncReply = decodeMessage(syncClient.exchange(encodeMessage(fanOut(asha).single().message)))
            assertEquals(listOf("event-1"), syncReply.acks)
            syncServer.await()
            syncClient.close()

            val third = IrohTransport.bind(minimal = true)
            try {
                val refusal = async {
                    val incoming = right.acceptNext() ?: return@async
                    val conn = right.accept(incoming)
                    conn.close()
                }
                val thirdConn = third.connect(right.ticket)
                assertThrows(Exception::class.java) { runBlocking { thirdConn.exchange(encodeMessage(SyncMessage(type = "sync", sender = "Third")), 2_000) } }
                refusal.await()
            } finally { third.shutdown() }
        } finally {
            left.shutdown(); right.shutdown()
        }
    }

    @Test fun n0OnlineIsOptIn() {
        assumeTrue("irohOnline=true", System.getProperty("irohOnline", "false") == "true")
        runBlocking {
            val endpoint = IrohTransport.bind(minimal = false)
            try { endpoint.online() } finally { endpoint.shutdown() }
        }
    }
}
