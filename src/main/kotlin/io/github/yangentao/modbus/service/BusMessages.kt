package io.github.yangentao.modbus.service

interface ModbusFrame

open class BusMessage(val message: String, val params: Map<String, String>) : ModbusFrame {

    override fun toString(): String {
        return "DtuMessage{ message:$message,  params: $params}"
    }
}

class IDBusMessage(message: String, params: Map<String, String>, val identName: String, val identValue: String) : BusMessage(message, params) {

    override fun toString(): String {
        return "IdentDtuMessage{ message:$message, $identName: $identValue, params: $params}"
    }
}