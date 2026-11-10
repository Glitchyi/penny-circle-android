package com.spendingbuddy.app.p2p

import computer.iroh.EndpointId
import computer.iroh.SecretKey
import computer.iroh.Signature
import java.util.Base64

/**
 * [MessageSigner] over the iroh 1.2.0 bindings: SecretKey.sign / EndpointId.verify (ed25519, the same key that is the
 * endpoint's identity). Signatures travel as unpadded URL-safe base64 (64 raw bytes).
 */
class IrohMessageSigner(secretKey: ByteArray) : MessageSigner {
    private val key = SecretKey.fromBytes(secretKey)

    override fun sign(payload: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(key.sign(payload).toBytes())

    override fun verify(endpointId: String, payload: ByteArray, signature: String): Boolean = try {
        EndpointId.fromString(endpointId).verify(payload, Signature.fromBytes(Base64.getUrlDecoder().decode(signature)))
        true
    } catch (_: Throwable) { false }
}
