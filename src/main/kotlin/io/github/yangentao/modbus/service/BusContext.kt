@file:Suppress("unused")

package io.github.yangentao.modbus.service

import io.github.yangentao.modbus.BusRequest
import io.github.yangentao.modbus.BusResponse
import io.github.yangentao.modbus.BusTasks
import io.github.yangentao.types.ListMap
import io.github.yangentao.types.TimeValue
import io.github.yangentao.types.appendAll
import io.github.yangentao.types.timeSeconds

abstract class BusContext {
    private var queryTasks: ArrayList<AutoRequestTask> = ArrayList()

    abstract val isActive: Boolean
    abstract fun send(request: BusRequest, timeoutSeconds: Int = 10): BusResponse?
    abstract fun closeSync()

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

