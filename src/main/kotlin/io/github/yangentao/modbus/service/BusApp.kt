package io.github.yangentao.modbus.service

import io.github.yangentao.types.PatternText
import io.github.yangentao.types.patternText
import kotlin.reflect.KClass

/// "HELLO {ident}"
open class BusApp(val endpoint: KClass<out BusEndpoint>, val identMessage: String?, val identName: String? = null) {
    val identPattern: PatternText? = identMessage?.patternText

    var messages: HashSet<String> = HashSet()
    var slaves: HashSet<Int> = hashSetOf(1)
    var autoQueryDelaySeconds: Int = 30

    open fun onCreate() {}
    open fun onService() {}
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

}
