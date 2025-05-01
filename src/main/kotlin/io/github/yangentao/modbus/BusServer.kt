@file:Suppress("unused")

package io.github.yangentao.modbus.tcp

import io.github.yangentao.modbus.modbus
import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import io.github.yangentao.tcp.*
import io.github.yangentao.tcp.frames.RawFrame
import io.github.yangentao.types.Hex
import io.github.yangentao.types.printX
import java.nio.channels.SelectionKey

/**
 * Modbus Server Callback
 * Read ---false-->  Response OR Data
 */
interface BusCallback {
    fun onBusClosed(key: SelectionKey, ident: String) {}
    fun onBusRead(key: SelectionKey, data: ByteArray): Boolean {
        return false
    }

    fun onBusResponse(key: SelectionKey, response: BusResponse) {}
    fun onBusData(key: SelectionKey, data: ByteArray) {}
}

/**
 * Modbus Server
 */
class BusServer(val port: Int, val secondsIdle: Int = 90, var callback: BusCallback? = null) : TcpServerCallback {

    private var tcpServer: TcpServer? = null
    private val identMap: HashMap<String, SelectionKey> = HashMap()
    val allClients: List<SelectionKey> get() = tcpServer?.clientKeys ?: emptyList()

    fun setIdent(key: SelectionKey, ident: String?, autoClose: Boolean = true) {
        if (ident == null) {
            val k = key.ident ?: return
            val old = identMap.remove(k)
            if (old != null && autoClose) {
                old.close()
            }
            key.ident = null
            return
        }
        if (autoClose) {
            identMap[ident]?.close()
        }
        identMap[ident] = key
        key.ident = ident

    }

    fun send(ident: String, request: BusRequest): BusResponse? {
        return identMap[ident]?.modbus?.send(request)
    }

    fun send(key: SelectionKey, request: BusRequest): BusResponse? {
        return key.modbus.send(request)
    }

    fun isOnline(ident: String): Boolean {
        val k = identMap[ident] ?: return false
        return k.isValid
    }

    fun stop() {
        tcpServer?.stop()
        tcpServer?.thread?.join(2000)
        tcpServer = null
    }

    fun start() {
        if (tcpServer != null) {
            error("mbus服务非空")
        }
        val sv = TcpServer(RawFrame())
        sv.clientIdleSeconds = secondsIdle
        sv.start(port, this)
        tcpServer = sv
    }

    override fun onTcpClosed(key: SelectionKey) {
        val ident = key.ident
        if (ident != null) {
            identMap.remove(ident)
            val c = callback
            c?.onBusClosed(key, ident)
        }
        printX("CLOSE CLIENT: ident $ident,   total count:  ${key.selector().keyCount}")
    }

    override fun onTcpAccept(key: SelectionKey) {
        printX("ACCEPT, total count ${key.selector().keyCount}")
    }

    override fun onTcpRecvFrame(key: SelectionKey, data: ByteArray) {
        printX("RECV ident:", key.ident, "  DATA: ", Hex.encode(data))
        val cb = callback ?: return
        try {
            if (cb.onBusRead(key, data)) {
                return
            }
            key.modbus.parseResponse(data)?.also {
                cb.onBusResponse(key, it)
                return
            }
            cb.onBusData(key, data)
        } catch (ex: Exception) {
            printX(ex)
        }
    }
}