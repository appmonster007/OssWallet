package com.osscabinet.ossWallet.data.local.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())
    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}

object NullableBigDecimalSerializer : KSerializer<BigDecimal?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal?", PrimitiveKind.STRING)
    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: BigDecimal?) {
        if (value == null) encoder.encodeNull()
        else encoder.encodeString(value.toPlainString())
    }
    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): BigDecimal? {
        return if (decoder.decodeNotNullMark()) BigDecimal(decoder.decodeString())
        else decoder.decodeNull()
    }
}
