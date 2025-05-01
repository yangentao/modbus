@file:Suppress("MemberVisibilityCanBePrivate")

package io.github.yangentao.modbus

import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import io.github.yangentao.tcp.attr
import io.github.yangentao.tcp.write
import java.nio.channels.SelectionKey

class ModBus(val selectionKey: SelectionKey) {
    private val lock = Object();
    private var lastResponse: BusResponse? = null

    fun sendOnly(request: BusRequest): Boolean {
        synchronized(lock) {
            return selectionKey.writeBus(request.bytes)
        }
    }

    fun send(request: BusRequest, timeoutMS: Long = 10_000): BusResponse? {
        synchronized(lock) {
            if (selectionKey.writeBus(request.bytes)) {
                if (timeoutMS > 0) {
                    lock.wait(timeoutMS)
                    val r = lastResponse
                    lastResponse = null
                    return r
                }
            }
        }
        return null
    }

    @Synchronized
    fun parseResponse(data: ByteArray): BusResponse? {
        synchronized(lock) {
            val resp = BusResponse.parse(data) ?: return null
            lastResponse = resp
            lock.notify()
            return resp
        }
    }
}

val SelectionKey.modbus: ModBus
    @Synchronized
    get() {
        return this.attr("modbus") {
            ModBus(this)
        }
    }

//发送需要间隔3.5个字符的时间, 大约4毫秒
fun SelectionKey.writeBus(data: ByteArray): Boolean {
    val b = this.write(data)
    Thread.sleep(5)
    return b
}
