@file:Suppress("unused")

package io.github.yangentao.modbus.tcp

import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import io.github.yangentao.modbus.service.BusApp
import io.github.yangentao.modbus.service.BusEndpoint
import io.github.yangentao.modbus.service.IDBusMessage
import io.github.yangentao.tcp.*
import io.github.yangentao.tcp.frames.RawFrame
import io.github.yangentao.types.Hex
import io.github.yangentao.types.createInstanceArgOne
import io.github.yangentao.types.printX
import java.nio.channels.SelectionKey

/**
 * Modbus Server
 */
class BusServer(val port: Int, val app: BusApp, val secondsIdle: Int = 90) : TcpServerCallback {

    private var tcpServer: TcpServer? = null
    private val identMap: HashMap<String, SelectionKey> = HashMap()
    val allClients: List<SelectionKey> get() = tcpServer?.clientKeys ?: emptyList()

    fun setIdent(key: SelectionKey, ident: String?) {
        if (ident == null) {
            val k = key.ident ?: return
            identMap.get(k)?.close()
            identMap.remove(k)
            key.ident = null
            return
        }
        identMap[ident]?.close()
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
            key.endpoint?.onClose()
            key.endpoint = null
            identMap.remove(ident)
        }
        printX("CLOSE CLIENT: ident $ident,   total count:  ${key.selector().keyCount}")
    }

    override fun onTcpAccept(key: SelectionKey) {
        printX("ACCEPT, total count ${key.selector().keyCount}")
        val ctx = TcpBusContext(key)
        val inst: BusEndpoint = app.endpoint.createInstanceArgOne(ctx) as? BusEndpoint ?: return
        key.endpoint = inst
        inst.onCreate()
    }

    override fun onTcpRecvFrame(key: SelectionKey, data: ByteArray) {
        printX("RECV ident:", key.ident, "  DATA: ", Hex.encode(data))
        try {
            val resp = key.modbus.parseResponse(data)
            if (resp != null) return
            try {
                val text = data.toString(Charsets.US_ASCII)
                if (text.isEmpty()) return
                val m = app.parseMessage(text) ?: return
                if (m is IDBusMessage) {
                    setIdent(key, m.identValue)
                    key.endpoint?.onIdent(m)
                } else {
                    key.endpoint?.onMessage(m)
                }
            } catch (ex: Exception) {
            }
        } catch (ex: Exception) {
            printX(ex)
        }
    }
}

var SelectionKey.endpoint: BusEndpoint? by SelectionKeyValue