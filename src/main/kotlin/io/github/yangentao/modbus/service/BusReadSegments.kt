package io.github.yangentao.modbus.service

import io.github.yangentao.modbus.proto.BusAddress
import io.github.yangentao.modbus.proto.BusReadRequest
import io.github.yangentao.modbus.model.PlcAddress

@Synchronized
fun findReadRequests(ver: Int, area: Int, slave: Int, autoQueryOnly: Boolean): List<BusReadRequest> {
    val key = ReadReqKey(ver, area, slave, autoQueryOnly)
    readRequestMap[key]?.also { return it }
    val ls = makeReadRequestBy(key)
    readRequestMap[key] = ls
    return ls
}

fun clearReadRequestCache() {
    readRequestMap.clear()
}

private data class ReadReqKey(val ver: Int, val area: Int, val slave: Int, val autoQueryOnly: Boolean)

private var readRequestMap: HashMap<ReadReqKey, List<BusReadRequest>> = HashMap()

private fun makeReadRequestBy(key: ReadReqKey): List<BusReadRequest> {
    var adList = PlcAddress.area(key.ver, key.area).sortedBy { it.address }
    if (key.autoQueryOnly) {
        adList = adList.filter { it.autoQuery == 1 }
    }
    val result: ArrayList<BusReadRequest> = ArrayList();
    if (adList.isEmpty()) return result
    val extraAddrSize = if (key.area in 3..4) 1 else 0
    val seg = ArrayList<PlcAddress>()
    for (ad in adList) {
        if (seg.isEmpty()) {
            seg.add(ad)
            continue
        }
        //125 max
        if (seg.size > 100 || ad.address > seg.last().address + 2) {
            result += BusReadRequest(BusAddress(seg.first().address, key.slave), extraAddrSize + 1 + seg.last().address - seg.first().address)
            seg.clear()
        }
        seg.add(ad)
    }
    if (seg.isNotEmpty()) {
        result += BusReadRequest(BusAddress(seg.first().address, key.slave), extraAddrSize + 1 + seg.last().address - seg.first().address)
    }
    return result
}