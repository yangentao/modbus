@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.github.yangentao.modbus.proto


import io.github.yangentao.modbus.busAreaFromAction
import io.github.yangentao.modbus.fillCRC16
import io.github.yangentao.types.Hex
import io.github.yangentao.types.low0
import io.github.yangentao.types.low1
import io.github.yangentao.types.uintValue

/**
 * BusRequest
 */
abstract class BusRequest(val bytes: ByteArray) {
    val slave: Int get() = bytes[0].uintValue
    val action: Int get() = bytes[1].uintValue
    val area: Int get() = busAreaFromAction(action)

    override fun toString(): String {
        return Hex.encode(bytes)
    }
}

class BusWriteOneRequest(val address: BusAddress, val value: Int) : BusRequest(ByteArray(8)) {
    init {
        bytes[0] = address.slave.low0
        bytes[1] = address.writeOneAction.low0
        bytes[2] = address.register.low1
        bytes[3] = address.register.low0
        bytes[4] = value.low1
        bytes[5] = value.low0
        bytes.fillCRC16()
    }
}

class BusWriteRequest(val address: BusAddress, val addressCount: Int, buf: ByteArray) : BusRequest(ByteArray(8 + 1 + buf.size)) {
    init {
        bytes[0] = address.slave.low0
        bytes[1] = address.writeAction.low0
        bytes[2] = address.register.low1
        bytes[3] = address.register.low0
        bytes[4] = addressCount.low1
        bytes[5] = addressCount.low0
        bytes[6] = buf.size.low0
        for (i in buf.indices) {
            bytes[7 + i] = buf[i]
        }
        bytes.fillCRC16()
    }
}

class BusReadRequest(val address: BusAddress, val addressCount: Int) : BusRequest(ByteArray(8)) {
    init {
        bytes[0] = address.slave.low0
        bytes[1] = address.readAction.low0
        bytes[2] = address.register.low1
        bytes[3] = address.register.low0
        bytes[4] = addressCount.low1
        bytes[5] = addressCount.low0
        bytes.fillCRC16()
    }
}
