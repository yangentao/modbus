@file:OptIn(ExperimentalStdlibApi::class)

package io.github.yangentao.modbus.emulator


import io.github.yangentao.modbus.BusAddress
import io.github.yangentao.modbus.busAreaFromAction
import io.github.yangentao.modbus.checkCRC16
import io.github.yangentao.modbus.fillCRC16
import io.github.yangentao.types.Hex
import io.github.yangentao.types.low0
import io.github.yangentao.types.low1
import io.github.yangentao.types.printX
import io.github.yangentao.types.uintValue

class BusEmulator(private val addressSizePerArea: Int = 128) {
    val valueList: ArrayList<BusAddressValue> = ArrayList()

    init {
        for (i in 1..<addressSizePerArea) {
            valueList += BusAddressValue(BusAddress(i))
        }
        for (i in 1..<addressSizePerArea) {
            valueList += BusAddressValue(BusAddress(10000 + i))
        }
        for (i in 1..<addressSizePerArea) {
            valueList += BusAddressValue(BusAddress(30000 + i))
        }
        for (i in 1..<addressSizePerArea) {
            valueList += BusAddressValue(BusAddress(40000 + i))
        }
    }

    operator fun get(address: Int): BusAddressValue {
        return valueList.first { it.addr.address == address  }
    }

    operator fun set(address: Int, byte0: Int, byte1: Int) {
        val item = this[address]
        item.byte0 = byte0
        item.byte1 = byte1
    }

    fun processRequest(data: ByteArray): ByteArray {
        if (!data.checkCRC16(1024)) error("Invalid request ")
        val slave: Int = data[0].uintValue
        val action: Int = data[1].uintValue
        val addr1: Int = data[2].uintValue
        val addr0: Int = data[3].uintValue
        val size1: Int = data[4].uintValue
        val size0: Int = data[5].uintValue
        val addr: Int = (addr1 shl 8) or addr0
        val size: Int = (size1 shl 8) or size0
        if (addr + size > addressSizePerArea) {
            error("addr + size > max address count of area")
        }
        val area = busAreaFromAction(action)
        if (action <= 4) {
            return actionRead(action, addr, size)
        }
        if (data.size > 8) {
            val bytesSize: Int = data[6].uintValue
            val bytes: ByteArray = data.sliceArray(7..<data.size - 2)
            return actionWrite(action, addr, size, bytes)
        }
        error("invalid request ")
    }

    private fun actionWrite(action: Int, register: Int, size: Int, bytes: ByteArray): ByteArray {
        val area: Int = when (action) {
            0x0f -> 0
            0x10 -> 4
            else -> error("Invalid action")
        }
        if (area == 0) {
            for (i in 0..<size) {
                val nByte: Byte = bytes[i / 8]
                val bitVal: Int = (nByte.toInt() shr (i % 8)) and 0x01
                val item = this[area + register + i + 1 ]
                item.byte0 = bitVal
                item.byte1 = 0
            }
        }
        if (area == 4) {
            printX("write 4: ", " addr: ", register, " size: ", size, " bytesCount:", bytes.size, " bytes:", Hex.encode(bytes))
            for (i in 0..<size) {
                val item = this[area +  register + i + 1 ]
                item.byte0 = bytes[i * 2].uintValue
                item.byte1 = bytes[i * 2 + 1].uintValue
            }
        }
        val resp = ByteArray(8)
        resp[0] = 1
        resp[1] = action.toByte()
        resp[2] = register.low1
        resp[3] = register.low0
        resp[4] = size.low1
        resp[5] = size.low0
        resp.fillCRC16()
        return resp
    }

    private fun actionRead(action: Int, register: Int, size: Int): ByteArray {
        val area: Int = when (action) {
            1 -> 0
            2 -> 1
            3 -> 4
            4 -> 3
            else -> error("Invalid action")
        }
        val fromIndex: Int = valueList.indexOfFirst { it.addr.area == area && it.addr.register == register }
        val ls = valueList.slice(fromIndex..<fromIndex + size)
        val resultBytes: ArrayList<Int> = ArrayList()
        when (area) {
            0, 1 -> {
                val nByteCount: Int = (size + 7) / 8
                for (i in 0..<nByteCount) {
                    resultBytes.add(0)
                }
                ls.forEachIndexed { idx, item ->
                    var bitVal = item.byte0 and 0x01
                    bitVal = bitVal shl idx % 8
                    val byteVal = resultBytes[idx / 8]
                    resultBytes[idx / 8] = byteVal or bitVal
                }
            }

            3, 4 -> {
                for (item in ls) {
                    resultBytes += item.byte0
                    resultBytes += item.byte1
                }
            }
        }
        val resp = ByteArray(resultBytes.size + 5)
        resp[0] = 1
        resp[1] = action.toByte()
        resp[2] = resultBytes.size.toByte()
        resultBytes.forEachIndexed { idx, value ->
            resp[3 + idx] = value.toByte()
        }
        resp.fillCRC16(1024)
        return resp
    }

    class BusAddressValue(val addr: BusAddress, var byte0: Int = 0, var byte1: Int = 0) {
        override fun toString(): String {
            return "BusAddressValue{ addr:${addr}, value: $byte0, $byte1 }"
        }
    }
}
