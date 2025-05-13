package io.github.yangentao.modbus.service

import io.github.yangentao.modbus.model.PlcAddress
import io.github.yangentao.modbus.proto.BusAddress
import io.github.yangentao.modbus.proto.BusReadRequest
import io.github.yangentao.types.PatternText
import io.github.yangentao.types.patternText
import kotlin.reflect.KClass

/// "HELLO {ident}"
open class BusApp(val endpoint: KClass<out BusEndpoint>, val identMessage: String?, val identName: String? = null, var querySeconds: Long = 30) {
    val identPattern: PatternText? = identMessage?.patternText

    var messages: HashSet<String> = HashSet()
    var slaves: HashSet<Int> = hashSetOf(1)

    private var readRequestMap: Map<Pair<Int, Int>, List<BusReadRequest>> = emptyMap()
    private val allVers: HashSet<Int> = HashSet()

    val versions: Set<Int> get() = allVers.toSet()

    open fun onCreate() {}
    open fun onService() {
        buildReadRequests(force = true)
    }

    open fun onDestroy() {}

    fun message(msg: String) {
        messages += msg
    }

    fun parseMessage(text: String): BusMessage? {
        val identMap = identPattern?.tryMatchEntire(text)
        if (identMap != null) {
            val key: String = identName ?: identMap.keys.first()
            return IDBusMessage(text, identMap, key, identMap[key]!!)
        }
        for (m in messages) {
            val p = m.patternText
            val map = p.tryMatchEntire(text)
            if (map != null) {
                return BusMessage(text, map)
            }
        }
        return null
    }

    fun buildReadRequests(force: Boolean = false) {
        if (force || readRequestMap.isEmpty()) {
            val result = HashMap<Pair<Int, Int>, List<BusReadRequest>>()
            val addrList = PlcAddress.all(null)
            val verSet = HashSet<Int>()
            for (ad in addrList) {
                verSet.add(ad.ver)
            }
            for (v in verSet) {
                for (a in BusAddress.areaList) {
                    result[v to a] = makeReadRequestBy(addrList, v, a)
                }
            }
            readRequestMap = result
            allVers.clear()
            allVers.addAll(verSet)
        }
    }

    @Synchronized
    fun findQueryRequests(ver: Int, area: Int): List<BusReadRequest> {
        buildReadRequests()
        return readRequestMap[ver to area] ?: emptyList()
    }

    private fun makeReadRequestBy(addrList: List<PlcAddress>, ver: Int, area: Int): List<BusReadRequest> {
        val ls = addrList.filter { it.ver == ver && it.area == area && it.autoQuery >= 1 }.sortedBy { it.address }
        val result: ArrayList<BusReadRequest> = ArrayList();
        if (ls.isEmpty()) return result
        val extraAddrSize = if (area in 3..4) 1 else 0
        val seg = ArrayList<PlcAddress>()
        for (ad in ls) {
            if (seg.isEmpty()) {
                seg.add(ad)
                continue
            }
            val addrSize = 1 + extraAddrSize + seg.last().address - seg.first().address
            if (ad.address > seg.last().address + 2 || addrSize >= 125 - 2) {
                result += BusReadRequest(BusAddress(seg.first().address), addrSize)
                seg.clear()
            }
            seg.add(ad)
        }
        if (seg.isNotEmpty()) {
            result += BusReadRequest(BusAddress(seg.first().address), 1 + extraAddrSize + seg.last().address - seg.first().address)
        }
        return result
    }

}
