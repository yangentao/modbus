package io.github.yangentao.modbus.tcp

import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import io.github.yangentao.modbus.service.BusContext
import io.github.yangentao.tcp.close
import java.nio.channels.SelectionKey

class TcpBusContext(val key: SelectionKey) : BusContext() {
    override val isActive: Boolean get() = key.isValid
    override fun send(request: BusRequest, timeoutSeconds: Int): BusResponse? {
        return key.modbus.send(request, timeoutSeconds * 1000L)
    }

    override fun closeSync() {
        key.close()
    }

}