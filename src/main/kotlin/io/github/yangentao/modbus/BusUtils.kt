@file:Suppress("unused")

package io.github.yangentao.modbus

import io.github.yangentao.types.*
import java.util.concurrent.*

fun busAreaFromAction(action: Int): Int {
    return when (action) {
        1, 5, 0x0F, 0x81, 0x85, 0x8F -> 0
        2, 0x82 -> 1
        3, 6, 0x10, 0x83, 0x86, 0x90 -> 4
        4, 0x84 -> 3
        else -> error("Invalid action")
    }
}

val Int.formatPlcAddress: String get() = this.format("00000")

fun ByteArray.fillCRC16(maxSize: Int = 1024) {
    if (this.size > maxSize) return
    if (this.size < 3) return
    val data = this
    val crc = CRC.crc16(data, data.size - 2)
    data[data.size - 2] = crc.low1
    data[data.size - 1] = crc.low0
}

//查询最多可以是125个地址,  250个字节
fun ByteArray.checkCRC16(maxSize: Int = 260): Boolean {
    val data = this
    if (data.size > maxSize) return false
    if (data.size < 3) return false
    val a = CRC.crc16(data, data.size - 2)
    return a.low0 == data[data.size - 1] && a.low1 == data[data.size - 2]
}

fun ByteArray.shortValuePLC(offset: Int): Int {
    return (this[offset + 1].toInt() and 0xFF) or (this[offset].toInt() shl 8)
}

fun ByteArray.intValuePLC(offset: Int): Int {
    //b0, b1, b2, b3 => b1, b0, b3, b2
    return bytes2Int(this[offset + 1].toInt(), this[offset].toInt(), this[offset + 3].toInt(), this[offset + 2].toInt())
}

fun ByteArray.floatValuePLC(offset: Int): Float {
    //b0, b1, b2, b3 => b1, b0, b3, b2
    val n = this.intValuePLC(offset)
    return java.lang.Float.intBitsToFloat(n)
}

internal object BusTasks {
    val service: ScheduledExecutorService = Executors.newScheduledThreadPool(4) {
        Thread(it, "TaskScheduled").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY
            setUncaughtExceptionHandler(::uncaughtException)
        }
    }

    //result = exec. submit(aCallable).get()
    fun <T> call(time: TimeValue, task: Callable<T>): ScheduledFuture<T> {
        return service.schedule(task, time.value, time.unit)
    }

    //result = exec. submit(aCallable).get()
    fun <T> call(task: Callable<T>): Future<T> {
        return service.submit(task)
    }

    fun submit(task: Runnable): Future<*> {
        return service.submit(task)
    }

    //TimeUnit.MILLISECONDS
    fun delayMill(delay: Long, block: Runnable): ScheduledFuture<*> {
        return service.schedule(block, delay, TimeUnit.MILLISECONDS)
    }

    //TimeUnit.MILLISECONDS
    fun fixedDelayMill(delay: Long, block: Runnable): ScheduledFuture<*> {
        return service.scheduleWithFixedDelay(block, delay, delay, TimeUnit.MILLISECONDS)
    }

    //TimeUnit.MILLISECONDS
    fun fixedRateMill(period: Long, block: Runnable): ScheduledFuture<*> {
        return service.scheduleAtFixedRate(block, period, period, TimeUnit.MILLISECONDS)
    }

    fun delay(delay: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.schedule(block, delay.value, delay.unit)
    }

    fun fixedDelay(delay: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.scheduleWithFixedDelay(block, delay.value, delay.value, delay.unit)
    }

    fun fixedRate(period: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.scheduleAtFixedRate(block, period.value, period.value, period.unit)
    }

}

@Suppress("UNUSED_PARAMETER")
internal fun uncaughtException(thread: Thread, ex: Throwable) {
    printX("uncaughtException: ", thread.name)
    printX(ex)
    ex.printStackTrace()
}