package com.carlosjimz87.wssecure.data.envelope

interface WsEnvelopeSigner {
    fun wrapOutgoing(raw: String): String = raw
    fun unwrapIncoming(text: String): String = text
}
