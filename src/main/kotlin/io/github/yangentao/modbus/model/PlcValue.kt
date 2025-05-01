package io.github.yangentao.modbus.model


import io.github.yangentao.anno.Label
import io.github.yangentao.anno.ModelField
import io.github.yangentao.kson.KsonObject
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.toJson

@Label("PLC值")
class PlcValue : TableModel() {
    @Label("设备")
    @ModelField(primaryKey = true)
    var devId: Long by model

    //40001
    @Label("地址")
    @ModelField(primaryKey = true)
    var address: Int by model

    @ModelField(index = true, defaultValue = "0")
    //原始值*100
    var valueLong: Long by model

    @ModelField
    var valueText: String? by model

    @ModelField
    var valueHex: String? by model

    @ModelField
    @Label("更新时间")
    var updateDateTime: String? by model

    val area: Int get() = address / 10000

    //寄存器地址从0开始
    val register: Int get() = address % 10000 - 1

    fun json(): KsonObject {
        return toJson(includes = listOf(this::area, this::register))
    }

    companion object : TableModelClass<PlcValue>()
}