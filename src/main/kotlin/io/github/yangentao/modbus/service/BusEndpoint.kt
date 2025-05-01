package io.github.yangentao.modbus.service

import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import java.util.concurrent.ConcurrentHashMap

abstract class BusEndpoint(val context: BusContext) {
    var autoQueryDelaySeconds: Int? = null
    var slaves: Set<Int>? = null
    var identName: String? = null
    var identValue: String? = null

    abstract fun onMessage(message: BusMessage)
    abstract fun onResponse(request: BusRequest, response: BusResponse)
    abstract fun onIdent(message: IDBusMessage)

    open fun onCreate() {}

    open fun onClose() {
        context.cancelAllQueryTasks()
    }

    companion object {
        val identContextMap: ConcurrentHashMap<String, BusContext> = ConcurrentHashMap()
        fun isOnline(ident: String): Boolean {
            return identContextMap.containsKey(ident)
        }

        fun find(ident: String): BusContext? = identContextMap[ident]

        fun allClient(): Set<String> {
            return HashSet(identContextMap.keys)
        }

        fun sendTo(ident: String, request: BusRequest, timeoutSeconds: Int = 10): BusResponse? {
            return find(ident)?.send(request, timeoutSeconds)
        }
    }
}