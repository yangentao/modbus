package io.github.yangentao.modbus

import io.github.yangentao.modbus.proto.BusRequest
import io.github.yangentao.modbus.proto.BusResponse
import io.github.yangentao.modbus.service.BusContext
import java.nio.channels.SelectionKey

class TcpBusContext (val key: SelectionKey): BusContext(){
    override val isActive: Boolean get() = key.isValid
    override fun send(request: BusRequest, timeoutSeconds: Int): BusResponse? {

    }

    override fun closeSync() {

    }

}