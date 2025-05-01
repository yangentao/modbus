package io.github.yangentao.modbus.tcp

import io.github.yangentao.modbus.service.BusContext
import io.github.yangentao.tcp.close
import io.github.yangentao.tcp.write
import java.nio.channels.SelectionKey

class TcpBusContext(val key: SelectionKey) : BusContext() {
    override val isActive: Boolean get() = key.isValid

    override fun closeSync() {
        key.close()
    }

    override fun writeBytes(data: ByteArray): Boolean {
        return key.write(data)
    }

}