@file:Suppress("unused")

package io.github.yangentao.modbus.tcp

import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import io.github.yangentao.modbus.service.BusApp
import io.github.yangentao.modbus.service.BusContext
import io.github.yangentao.modbus.service.BusEndpoint
import io.github.yangentao.modbus.service.IDBusMessage
import io.github.yangentao.tcp.*
import io.github.yangentao.tcp.frames.RawFrame
import io.github.yangentao.types.Hex
import io.github.yangentao.types.createInstanceArgOne
import io.github.yangentao.xlog.TagLog
import java.nio.channels.SelectionKey

/**
 * Modbus Server
 */
class TcpBusServer(val port: Int, val app: BusApp, val secondsIdle: Int = 90) : TcpServerCallback {

    private var tcpServer: TcpServer? = null
    private val identMap: HashMap<String, BusContext> = HashMap()
    val allClients: List<SelectionKey> get() = tcpServer?.clientKeys ?: emptyList()

    fun setIdent(key: SelectionKey, ident: String?) {
        if (ident == null) {
            val k = key.ident ?: return
            identMap[k]?.closeSync()
            identMap.remove(k)
            key.ident = null
        } else {
            identMap[ident]?.closeSync()
            key.context?.also {
                identMap[ident] = it
            }
            key.ident = ident
        }
    }

    fun send(ident: String, request: BusRequest): BusResponse? {
        return identMap[ident]?.send(request)
    }

    fun send(key: SelectionKey, request: BusRequest): BusResponse? {
        return key.context?.send(request)
    }

    fun isOnline(ident: String): Boolean {
        val k = identMap[ident] ?: return false
        return k.isActive
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
            identMap.remove(ident)
        }
        key.endpoint = null
        busLog.d("CLOSE CLIENT: ident $ident,   total count:  ${key.selector().keyCount}")
    }

    override fun onTcpAccept(key: SelectionKey) {
        busLog.d("ACCEPT, total count ${key.selector().keyCount}")
        val ctx = TcpBusContext(key, app)
        val inst: BusEndpoint = app.endpoint.createInstanceArgOne(ctx) as? BusEndpoint ?: return
        key.endpoint = inst
        inst.onCreate()
    }

    override fun onTcpRecvFrame(key: SelectionKey, data: ByteArray) {
        busLog.d("RECV ident:", key.ident, "  DATA: ", Hex.encode(data))
        try {
            val resp = key.context?.parseResponse(data)
            if (resp != null) {
                val req = key.context?.lastRequest
                if (req != null) {
                    key.endpoint?.onResponse(req, resp)
                }
                return
            }
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
            busLog.e(ex)
        }
    }
}

var SelectionKey.endpoint: BusEndpoint? by SelectionKeyValue
val SelectionKey.context: BusContext? get() = this.endpoint?.context

val busLog = TagLog("modbus")