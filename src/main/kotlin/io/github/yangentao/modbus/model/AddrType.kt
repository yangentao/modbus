package io.github.yangentao.modbus.model

import io.github.yangentao.types.*
import org.kerbaya.ieee754lib.BitUtils
import org.kerbaya.ieee754lib.IEEE754
import org.kerbaya.ieee754lib.IEEE754Format

//@OptionList("0:默认", "N2:整型-2字节", "N01:整型-2字节", "N10:整型-2字节", "N4:整型-4字节", "N0123:整型-4字节", "N1032:整型-4字节", "F4:浮点-4字节", "F0123:浮点-4字节", "F1032:浮点-4字节")
@Suppress("unused")
object AddrType {
    const val DEFAULT = "0"
    const val m = "m" //触发 1
    const val M = "M" //交替 01
    const val N2 = "N2"
    const val N01 = "N01"
    const val N10 = "N10"
    const val N4 = "N4"
    const val N0123 = "N0123"
    const val N1032 = "N1032"
    const val F4 = "F4"
    const val F0123 = "F0123"
    const val F1032 = "F1032"

    const val F754 = "F754"

    private val b4List = listOf(N4, N0123, N1032, F4, F0123, F1032, F754)
    private val int4List = listOf(N4, N0123, N1032)
    private val float4List = listOf(F4, F0123, F1032, F754)

    fun isFloat4(type: String?): Boolean {
        return type in float4List
    }

    fun isInt4(type: String?): Boolean {
        return type in int4List
    }

    fun isByte4(type: String?): Boolean {
        return type in b4List
    }
}

interface Int2Address {
    fun toBytes(value: Int): ByteArray
    fun toInt(a: Byte, b: Byte): Int
}

//低字节在前
object Int01 : Int2Address {
    override fun toBytes(value: Int): ByteArray {
        return byteArrayOf(value.low0, value.low1)
    }

    override fun toInt(a: Byte, b: Byte): Int {
        return bytes2Int(a.toInt(), b.toInt(), 0, 0)
    }
}

//高字节在前
object Int10 : Int2Address {
    override fun toBytes(value: Int): ByteArray {
        return byteArrayOf(value.low1, value.low0)
    }

    override fun toInt(a: Byte, b: Byte): Int {
        return bytes2Int(b.toInt(), a.toInt(), 0, 0)
    }
}

interface Int4Address {
    fun toBytes(value: Int): ByteArray
    fun toInt(a: Byte, b: Byte, c: Byte, d: Byte): Int
}

object Int0123 : Int4Address {
    override fun toBytes(value: Int): ByteArray {
        return byteArrayOf(value.low0, value.low1, value.low2, value.low3)
    }

    override fun toInt(a: Byte, b: Byte, c: Byte, d: Byte): Int {
        return bytes2Int(a.toInt(), b.toInt(), c.toInt(), d.toInt())
    }
}

object Int1032 : Int4Address {
    override fun toBytes(value: Int): ByteArray {
        return byteArrayOf(value.low1, value.low0, value.low3, value.low2)
    }

    override fun toInt(a: Byte, b: Byte, c: Byte, d: Byte): Int {
        return bytes2Int(b.toInt(), a.toInt(), d.toInt(), c.toInt())
    }
}

interface Float4Address {
    fun toBytes(value: Float): ByteArray
    fun toFloat(a: Byte, b: Byte, c: Byte, d: Byte): Float
}

object Float754 : Float4Address {
    override fun toBytes(value: Float): ByteArray {
        val ls = ByteArray(4) { 0 }
        IEEE754.valueOf(value).toBits(IEEE754Format.SINGLE, BitUtils.wrapSink(ls))
        return ls
    }

    override fun toFloat(a: Byte, b: Byte, c: Byte, d: Byte): Float {
        val x = IEEE754.decode(IEEE754Format.SINGLE, BitUtils.wrapSource(byteArrayOf(a, b, c, d)))
        return x.toFloat()
    }

}

object Float0123 : Float4Address {
    override fun toBytes(value: Float): ByteArray {
        val v = java.lang.Float.floatToIntBits(value)
        return byteArrayOf(v.low0, v.low1, v.low2, v.low3)
    }

    override fun toFloat(a: Byte, b: Byte, c: Byte, d: Byte): Float {
        return bytes2Float(a.toInt(), b.toInt(), c.toInt(), d.toInt())
    }

}

object Float1032 : Float4Address {
    override fun toBytes(value: Float): ByteArray {
        val v = java.lang.Float.floatToIntBits(value)
        return byteArrayOf(v.low1, v.low0, v.low3, v.low2)
    }

    override fun toFloat(a: Byte, b: Byte, c: Byte, d: Byte): Float {
        return bytes2Float(b.toInt(), a.toInt(), d.toInt(), c.toInt())
    }

}

fun addressType2Bytes(addrType: String, floatValue: Float, intVal: Int): ByteArray {
    return when (addrType) {
        AddrType.F754 -> Float754.toBytes(floatValue)
        AddrType.F0123 -> Float0123.toBytes(floatValue)
        AddrType.F1032 -> Float1032.toBytes(floatValue)
        AddrType.F4 -> Float1032.toBytes(floatValue)
        AddrType.N0123 -> Int0123.toBytes(intVal)
        AddrType.N1032 -> Int1032.toBytes(intVal)
        AddrType.N4 -> Int1032.toBytes(intVal)
        AddrType.N01 -> Int01.toBytes(intVal)
        AddrType.N10 -> Int10.toBytes(intVal)
        AddrType.N2 -> Int10.toBytes(intVal)
        else -> Int10.toBytes(intVal)
    }
}

fun bytes2AddressValue(type: String, a: Byte, b: Byte): Int {
    return when (type) {
        AddrType.N01 -> bytes2Int(a.toInt(), b.toInt(), 0, 0)
        else -> bytes2Int(b.toInt(), a.toInt(), 0, 0) //N2, N10
    }
}

fun bytes2AddressValue(type: String, a: Byte, b: Byte, c: Byte, d: Byte): Number {
    return when (type) {
        AddrType.N0123 -> Int0123.toInt(a, b, c, d)
        AddrType.N1032 -> Int1032.toInt(a, b, c, d)
        AddrType.N4 -> Int1032.toInt(a, b, c, d)
        AddrType.F0123 -> Float0123.toFloat(a, b, c, d)
        AddrType.F1032 -> Float1032.toFloat(a, b, c, d)
        AddrType.F4 -> Float1032.toFloat(a, b, c, d)
        AddrType.F754 -> Float754.toFloat(a, b, c, d)
        else -> error("bad type: $type")
    }
}
