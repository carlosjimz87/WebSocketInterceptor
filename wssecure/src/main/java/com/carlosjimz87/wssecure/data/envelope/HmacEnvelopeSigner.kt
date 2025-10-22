package com.carlosjimz87.wssecure.data.envelope

import android.os.Build
import java.util.UUID

class HmacEnvelopeSigner(private val secret: () -> String?) : WsEnvelopeSigner {
    private fun hmacSHA256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    override fun wrapOutgoing(raw: String): String {
        val s = secret() ?: return raw
        val nonce = UUID.randomUUID().toString()
        val ts = System.currentTimeMillis()
        val body = """{"nonce":"$nonce","ts":$ts,"payload":$raw}"""
        val sig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.util.Base64.getEncoder()
                .encodeToString(hmacSHA256(s.toByteArray(), body.toByteArray()))
        } else {
            android.util.Base64.encodeToString(
                hmacSHA256(s.toByteArray(), body.toByteArray()),
                android.util.Base64.NO_WRAP
            )
        }
        return """{"meta":{"alg":"HS256","sig":"$sig"},"body":$body}"""
    }
    // Keep unwrap as-pass or verify if your server signs replies back
}