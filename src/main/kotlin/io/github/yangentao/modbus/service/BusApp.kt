package io.github.yangentao.modbus.service

import kotlin.reflect.KClass

/// "HELLO {ident}"
open class BusApp(val endpoint: KClass<out BusEndpoint>, val identMessage: String?, val identName: String? = null) {

    var messages: HashSet<String> = HashSet()
    var slaves: HashSet<Int> = hashSetOf(1)
    var autoQueryDelaySeconds: Int = 30

    open fun onCreate() {}
    open fun onService() {}
    open fun onDestroy() {}

    fun message(msg: String) {
        messages += msg
    }

}
