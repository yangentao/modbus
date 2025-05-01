@file:Suppress("MemberVisibilityCanBePrivate")

package io.github.yangentao.modbus.proto

import io.github.yangentao.types.format

/**
 * 40001, 从1开始
 * 01  Read   00001-09999  bit   one/multi
 * 02  Read   10001-19999  bit   one/multi
 * 03  Read   40001-49999  word  one/multi
 * 04  Read   30001-39999  word  one/multi
 * 05  Write  00001-09999  bit   one
 * 06  Write  40001-49999  word  one
 * 0F  Write  00001-09999  bit   multi
 * 10  Write  40001-49999  word  multi
 */
class BusAddress(val address: Int, val slave: Int = 1) {
    val area: Int = address / 10000
    val register: Int = address % 10000 - 1

    init {
        assert(address > 0 && slave >= 0)
    }

    override fun toString(): String {
        return address.format("00000")
    }

    val readAction: Int
        get() = when (area) {
            0 -> 1
            1 -> 2
            3 -> 4
            4 -> 3
            else -> error("invalid plc address: $this ")
        }
    val writeAction: Int
        get() = when (area) {
            0 -> 0x0F
            4 -> 0x10
            else -> error("invalid plc address: $this ")
        }
    val writeOneAction: Int
        get() = when (area) {
            0 -> 5
            4 -> 6
            else -> error("invalid plc address: $this ")
        }

    fun read(addressCount: Int): BusReadRequest {
        return BusReadRequest(this, addressCount)
    }

    fun write(addressCount: Int, buf: ByteArray): BusWriteRequest {
        return BusWriteRequest(this, addressCount, buf)
    }

    fun writeOne(value: Int): BusWriteOneRequest {
        return BusWriteOneRequest(this, value)
    }

    companion object {
        val areaList = listOf(0, 1, 3, 4)
        fun from(area: Int, register: Int, slave: Int = 1): BusAddress {
            assert(area in 0..4 && register >= 0)
            return BusAddress(area * 10000 + register + 1, slave)
        }
    }
}

