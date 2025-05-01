@file:Suppress("unused")

package io.github.yangentao.modbus.service

import io.github.yangentao.modbus.BusTasks
import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import io.github.yangentao.types.ListMap
import io.github.yangentao.types.TimeValue
import io.github.yangentao.types.appendAll
import io.github.yangentao.types.timeSeconds

abstract class BusContext {
    private val lock = Object();
    private var lastResponse: BusResponse? = null

    private var queryTasks: ArrayList<AutoRequestTask> = ArrayList()

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

    fun writeRequest(request: BusRequest): Boolean {
        val b = writeBytes(request.bytes)
        Thread.sleep(5)
        return b
    }

    fun cancelAllQueryTasks() {
        queryTasks.forEach { it.cancel() }
        queryTasks.clear()
    }

    //合并触发
    fun startAutoRequests(autoRequests: List<DelayRequestList>) {
        if (autoRequests.isEmpty()) return
        val multMap = ListMap<Long, BusRequest>()
        for (q in autoRequests) {
            multMap.appendAll(q.delay.toSeconds.value, q.requests)
        }
        val ls = this.queryTasks
        for (e in multMap.entries) {
            val task = AutoRequestTask(this, DelayRequestList(e.key.timeSeconds, e.value))
            ls += task
            BusTasks.delay(e.key.timeSeconds, task)
        }
        this.queryTasks = ls
    }

}

data class DelayRequestList(val delay: TimeValue, val requests: List<BusRequest>)

private class AutoRequestTask(val context: BusContext, val item: DelayRequestList) : Runnable {
    private var canceled: Boolean = false
    fun cancel() {
        canceled = true
    }

    override fun run() {
        if (canceled) return
        try {
            doRun()
        } catch (ex: Exception) {
            println(ex)
        }
        if (!canceled && context.isActive) {
            BusTasks.delay(item.delay, this)
        }
    }

    private fun doRun() {
        if (!canceled && context.isActive) {
            for (req in item.requests) {
                if (canceled) return
                context.send(req, 10) ?: return
            }
        }
    }
}

