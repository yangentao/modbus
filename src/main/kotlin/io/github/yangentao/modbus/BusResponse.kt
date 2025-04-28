@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.yangentao.modbus


import io.github.yangentao.modbus.BusAddress
import io.github.yangentao.types.Hex
import io.github.yangentao.types.bytes2Int
import io.github.yangentao.types.uintValue

/*
 * 一次最多可读取125个地址, 读取(3,4区)时, 响应的数据包最大 125 * 2 + 5 = 255 字节
 * 错误帧最小, 5字节.
 *
 */


class BusReadResponse(slave: Int, action: Int, val size: Int, val data: ByteArray) : BusResponse(slave, action) {

    override fun toString(): String {
        return "BusReadResponse slave:" + slave.toString() + " aciton:" + action.toString() + " size:" + size.toString() + " data(X):" + Hex.encode(data)
    }
}

class BusWriteResponse(slave: Int, action: Int, val register: Int, val size: Int) : BusResponse(slave, action) {
    val address: BusAddress get() = BusAddress(area * 10000 + register + 1, slave)

    override fun toString(): String {
        return "BusWriteResponse slave:$slave aciton:$action register:$register size:$size"
    }
}

class BusWriteOneResponse(slave: Int, action: Int, val register: Int, val value: Int) : BusResponse(slave, action) {
    val address: BusAddress get() = BusAddress(area * 10000 + register + 1, slave)

    override fun toString(): String {
        return "BusWriteOneResponse slave:" + slave.toString() + " aciton:" + action.toString() + " address:" + register.toString() + " value(X):" + value.toString(16)
    }
}

class BusError(slave: Int, action: Int, val code: Int) : BusResponse(slave, action) {
    val requestAction: Int get() = action - 0x80
    val message: String get() = errorCodeMessages[code and 0xff] ?: "未知错误"

    override fun toString(): String {
        return "BusError id:" + slave.toString() + " aciton:" + action.toString() + " code(X):" + code.toString(16) + " message:" + message
    }

    companion object {
        val errorActions: List<Int> = listOf(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x8F, 0x90)
        val errorCodes: List<Int> = listOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x0A, 0x0B)
        val errorCodeMessages: HashMap<Int, String> = hashMapOf(
            0x01 to "非法功能",
            0x02 to "非法数据地址",
            0x03 to "非法数据值",
            0x04 to "从站设备故障",
            0x05 to "确认",
            0x06 to "从属设备忙",
            0x0A to "不可用网关路径",
            0x0B to "网关目标设备响应失败",
        )

    }
}

abstract class BusResponse(val slave: Int, val action: Int) {

    val area: Int = busAreaFromAction(action)

    companion object {
        private val writeActions: List<Int> = listOf(5, 6, 15, 16)
        private val readActions: List<Int> = listOf(1, 2, 3, 4)
        private val errorActions: List<Int> = listOf(0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x8F, 0x90)
        private val responseActions: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 15, 16, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x8F, 0x90)

        //错误帧最小, 5字节
        fun parse(buf: ByteArray): BusResponse? {
            if (buf.size !in 5..255) return null
            val slave: Int = buf[0].uintValue
            if (slave > 247) return null
            val action = buf[1].uintValue
            val length: Int = when (action) {
                0x81, 0x82, 0x84, 0x85, 0x86, 0x8F, 0x90 -> 5
                1, 2, 3, 4 -> buf[2].uintValue + 5
                5, 6, 15, 16 -> 8
                else -> return null
            }
            if (buf.size != length) return null

            if (!buf.checkCRC16()) return null
            if (action in errorActions) {
                val code = buf[2].uintValue
                return BusError(slave, action, code)
            }
            if (action in readActions) {
                val size: Int = buf[2].uintValue
                return BusReadResponse(slave, action, size, buf.sliceArray(3 until buf.size - 2))
            }
            if (action in 5..6) {
                val address: Int = bytes2Int(buf[3].uintValue, buf[2].uintValue, 0, 0)
                val value: Int = bytes2Int(buf[5].uintValue, buf[4].uintValue, 0, 0)
                return BusWriteOneResponse(slave, action, address, value)
            }
            if (action in 15..16) {
                val address: Int = bytes2Int(buf[3].uintValue, buf[2].uintValue, 0, 0)
                val size: Int = bytes2Int(buf[5].uintValue, buf[4].uintValue, 0, 0)
                return BusWriteResponse(slave, action, address, size)
            }
            return null
        }

    }

}


