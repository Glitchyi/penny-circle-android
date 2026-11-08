package com.spendingbuddy.app.p2p

import computer.iroh.Endpoint
import computer.iroh.EndpointAddr
import computer.iroh.EndpointOptions
import computer.iroh.EndpointTicket
import computer.iroh.Incoming
import computer.iroh.Preset
import computer.iroh.presetMinimal
import computer.iroh.presetN0
import kotlinx.coroutines.withTimeout

/** One request/reply exchange over a single QUIC bidirectional stream. */
class IrohConnection internal constructor(private val connection: computer.iroh.Connection) {
    val remoteId: String get() = connection.remoteId().toString()
    val viaRelay: Boolean? get() = connection.paths().firstOrNull { it.isSelected }?.isRelay

    suspend fun exchange(request: ByteArray, timeoutMs: Long = 20_000L): ByteArray = withTimeout(timeoutMs) {
        val stream = connection.openBi()
        stream.send().writeAll(request)
        stream.send().finish()
        stream.recv().readToEnd(MAX_MESSAGE_BYTES.toUInt())
    }

    suspend fun serve(handler: suspend (ByteArray) -> ByteArray): ByteArray {
        val stream = connection.acceptBi()
        val request = stream.recv().readToEnd(MAX_MESSAGE_BYTES.toUInt())
        val reply = handler(request)
        stream.send().writeAll(reply)
        stream.send().finish()
        return request
    }

    fun close() = runCatching { connection.close(0, ByteArray(0)) }
}

/** Thin wrapper around the official iroh Kotlin bindings; no Android APIs are used here. */
class IrohTransport private constructor(val endpoint: Endpoint) {
    val endpointId: String get() = endpoint.id().toString()
    val endpointIdShort: String get() = endpoint.id().fmtShort()
    val ticket: String get() = EndpointTicket.fromAddr(endpoint.addr()).toString()
    val relayUrl: String? get() = endpoint.addr().relayUrl()
    val directAddresses: List<String> get() = endpoint.addr().directAddresses()

    suspend fun online() = endpoint.online()
    suspend fun acceptNext(): Incoming? = endpoint.acceptNext()
    suspend fun connect(ticketOrAddress: String): IrohConnection {
        val address = runCatching { EndpointTicket.fromString(ticketOrAddress).endpointAddr() }
            .getOrElse {
                val id = computer.iroh.EndpointId.fromString(ticketOrAddress)
                EndpointAddr(id, null, emptyList())
            }
        return IrohConnection(endpoint.connect(address, SYNC_ALPN))
    }
    suspend fun accept(incoming: Incoming): IrohConnection = IrohConnection(incoming.accept().connect())
    suspend fun shutdown() = endpoint.shutdown()

    companion object {
        suspend fun bind(secretKey: ByteArray? = null, minimal: Boolean = false): IrohTransport {
            val preset: Preset = if (minimal) presetMinimal() else presetN0()
            return IrohTransport(Endpoint.bind(EndpointOptions(preset = preset, secretKey = secretKey, alpns = listOf(SYNC_ALPN))))
        }
    }
}
