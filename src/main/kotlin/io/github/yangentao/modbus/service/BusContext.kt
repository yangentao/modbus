@file:Suppress("unused")

package io.github.yangentao.modbus.service

import io.github.yangentao.modbus.BusTasks
import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import io.github.yangentao.types.timeSeconds
import io.github.yangentao.xlog.loge
import java.util.concurrent.ScheduledFuture

abstract class BusContext(val app: BusApp) {
    private val lock = Object();
    private var lastResponse: BusResponse? = null
    var lastRequest: BusRequest? = null
    private var queryTask: ScheduledFuture<*>? = null

    abstract val isActive: Boolean
    abstract fun closeSync()
    abstract fun writeBytes(data: ByteArray): Boolean

    fun send(request: BusRequest, timeoutSeconds: Long = 10): BusResponse? {
        synchronized(lock) {
            if (writeRequest(request)) {
                if (timeoutSeconds > 0) {
                    lock.wait(timeoutSeconds * 1000L)
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

    private fun writeRequest(request: BusRequest): Boolean {
        lastRequest = request
        val b = writeBytes(request.bytes)
        Thread.sleep(5)
        return b
    }

    fun cancelAutoQuery() {
        queryTask?.cancel(false);
        queryTask = null;
    }

    fun startAutoQuery() {
        queryTask = BusTasks.fixedDelay(app.querySeconds.timeSeconds) {
            try {
                doQuery()
            } catch (e: Throwable) {
                loge(e)
            }
        }
    }

    private fun doQuery() {
        for (ver in app.versions) {
            for (area in listOf(0, 1, 3, 4)) {
                val list = app.findQueryRequests(ver, area)
                if (list.isEmpty()) continue
                for (req in list) {
                    if (queryTask == null || !isActive) return
                    send(req)
                }
            }
        }
    }

}

