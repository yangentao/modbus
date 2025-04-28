package io.github.yangentao.modbus.emulator

import io.github.yangentao.modbus.writeBus
import io.github.yangentao.tcp.TcpClient
import io.github.yangentao.tcp.TcpClientCallback
import io.github.yangentao.tcp.frames.RawFrame
import io.github.yangentao.types.Rand
import java.nio.channels.SelectionKey
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val taskService: ScheduledExecutorService = Executors.newScheduledThreadPool(4) {
    Thread(it, "taskService").apply {
        isDaemon = true
        priority = Thread.NORM_PRIORITY
    }
}

class BusDevice : TcpClientCallback {
    val emulator: BusEmulator = BusEmulator()
    private var tcpClient: TcpClient? = null
    private var heartTask: ScheduledFuture<*>? = null

    var registerPackage: ByteArray = "HELLO ${Rand.nextLong(10_000, 90_000)}".toByteArray()
    var heartPackage: () -> ByteArray = { "RSSI: 20".toByteArray() }
    var heartDelay: Long = 10_000L

    fun powerOn(host: String, port: Int) {
        if (tcpClient != null) error("tcp client already exist!")
        tcpClient = TcpClient(RawFrame(), host, port)
        tcpClient?.start(this)
    }

    fun powerOff() {
        heartTask?.cancel(true)
        heartTask = null
        tcpClient?.stop()
        tcpClient?.waitThreadExit(5000)
        tcpClient = null
    }

    fun waitOff() {
        tcpClient?.waitThreadExit(0)
    }

    override fun onTcpConnect(key: SelectionKey, success: Boolean) {
        key.writeBus(registerPackage)
        heartTask = taskService.scheduleWithFixedDelay({
            if (key.isValid) {
                key.writeBus(heartPackage())
            } else {
                heartTask?.cancel(false)
                heartTask = null
            }
        }, heartDelay, heartDelay, TimeUnit.MILLISECONDS)
    }

    override fun onTcpRecvFrame(key: SelectionKey, data: ByteArray) {
        try {
            val bytes = emulator.processRequest(data)
            key.writeBus(bytes)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onTcpClosed(key: SelectionKey) {
        taskService.submit {
            powerOff()
        }
    }

    override fun onTcpIdle(key: SelectionKey) {
    }

    override fun onTcpException(key: SelectionKey, ex: Throwable) {
        ex.printStackTrace()
    }

}