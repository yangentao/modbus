package io.github.yangentao.modbus.model

import io.github.yangentao.modbus.proto.BusReadRequest
import io.github.yangentao.modbus.proto.BusReadResponse
import io.github.yangentao.sql.update
import io.github.yangentao.types.*

class ValueSaver(
    private val dev: Dev,
    private val req: BusReadRequest,
    private val resp: BusReadResponse,
    private val dateTime: DateTime = DateTime.now
) {

    //@OptionList("0:默认", "n01:整型-2字节", "n10:整型-2字节", "N0123:整型-4字节", "N1032:整型-4字节", "F0123:浮点-4字节", "F1032:浮点-4字节")
    private fun bytes2Value(type: String, a: Byte, b: Byte): Int {
        return when (type) {
            AddrType.N01 -> bytes2Int(a.toInt(), b.toInt(), 0, 0)
            else -> bytes2Int(b.toInt(), a.toInt(), 0, 0) //N2, N10
        }
    }

    private fun bytes2Value(type: String, a: Byte, b: Byte, c: Byte, d: Byte): Number {
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

    //area 0, 1
    //返回是否有警告
    private fun saveAreaBits(area: Int): Boolean {
        assert(area == 0 || area == 1)
        val addrList = PlcAddress.area(dev.ver, area)
        if (addrList.isEmpty()) return false

        val nList: List<Int> = resp.data.toBitList()
        val startAddress = req.address.register
        val addrCount: Int = minValue(req.addressCount, nList.size)
        var warn = false
        for (i in 0 until addrCount) {
            val addr = area * 10000 + 1 + startAddress + i
            val ad: PlcAddress = addrList.firstOrNull { it.address == addr } ?: continue
            val v = nList[i]
            updateValue(addr, v.toString(), Hex.encode(v))
            if (dev.ver == 0 && v != 0 && addr in 10009..10018) {
                warn = true
            }
        }
        return warn
    }

    //3,4区, 一个地址2个字节, 一个值占两个地址(4个字节),  addrCount是地址数量, 不是值的数量
    private fun saveAreaBytes(area: Int) {
        assert(area == 3 || area == 4)
        val addrList = PlcAddress.area(dev.ver, area)
        if (addrList.isEmpty()) return

        val startAddress: Int = req.address.register
        val addrCount: Int = minValue(req.addressCount, resp.size / 2)

        var i = 0
        while (i < addrCount) {
            val address = startAddress + i + 1 + area * 10000
            val ad: PlcAddress? = addrList.firstOrNull { it.address == address }
            if (ad != null) {
                val a = resp.data[i * 2]
                val b = resp.data[i * 2 + 1]

                if (AddrType.isByte4(ad.addrType)) {
                    if (i + 1 < addrCount) {
                        val c = resp.data[i * 2 + 2]
                        val d = resp.data[i * 2 + 3]
                        val v = bytes2Value(ad.addrType!!, a, b, c, d)
                        if (v is Float) {
                            updateValue(address, v.format("#.##"), Hex.encode(byteArrayOf(a, b, c, d)))
                        } else {
                            updateValue(address, v.toString(), Hex.encode(byteArrayOf(a, b, c, d)))
                        }
                    }
                    i += 1
                } else {
                    val v = bytes2Value(ad.addrType ?: AddrType.DEFAULT, a, b)
                    updateValue(address, v.toString(), Hex.encode(byteArrayOf(a, b)))
                }
            }
            i += 1
        }
    }

    private fun updateValue(address: Int, valueText: String, valueHex: String) {
        val doubleValue: Double = valueText.toDoubleOrNull() ?: 0.0
        val longValue: Long = (doubleValue * 100).toLong()
        val v = PlcValue()
        v.devId = dev.id
        v.address = address
        v.updateDateTime = dateTime.formatDateTime()
        v.valueText = valueText
        v.valueHex = valueHex
        v.valueLong = longValue
        v.upsert()

        val plcAddr = PlcAddress.addr(dev.ver, address) ?: return
        if (plcAddr.recordHistory == 0) return
        val valueMap: Long = if (plcAddr.historyMod > 0) {
            longValue / plcAddr.historyMod
        } else {
            longValue
        }
        val hisKey = HistoryKey(dev.id, address)
        val hisValue = HistoryValue(valueMap, dateTime.timeInMillis)
        val oldHisValue = historyMap[hisKey]
        if (oldHisValue == null || oldHisValue.value != hisValue.value || (oldHisValue.time + 3600_000L < hisValue.time)) {
            historyMap[hisKey] = hisValue
            val a = PlcHistory()
            a.devId = dev.id
            a.address = address
            a.createDate = dateTime.formatDate()
            a.createTime = dateTime.formatTime()
            a.valueText = valueText
            a.valueHex = valueHex
            a.valueLong = longValue
            a.insert()
        }
    }

    fun saveResponse() {
        when (resp.area) {
            0 -> saveAreaBits(0)
            1 -> {
                val hasWarning = saveAreaBits(1)
                dev.update {
                    it.warnings = if (hasWarning) 1 else 0
                }
            }

            3 -> saveAreaBytes(3)
            4 -> saveAreaBytes(4)
            else -> return
        }
    }

    companion object {
        private val historyMap: HashMap<HistoryKey, HistoryValue> = HashMap()
    }

}

private data class HistoryKey(val devId: Long, val address: Int)
private data class HistoryValue(val value: Long, val time: Long)

//fun main() {
//    val map = HashMap<HistoryKey, Int>()
//    map[HistoryKey(1, 1)] = 11
//    map[HistoryKey(1, 1)] = 12
//    printX(map)
//}
//


