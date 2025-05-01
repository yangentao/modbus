package io.github.yangentao.modbus.model


import io.github.yangentao.anno.Label
import io.github.yangentao.anno.ModelField
import io.github.yangentao.kson.KsonObject
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.toJson

@Label("PLC历史值13区")
class PlcHistory : TableModel() {

    @Label("ID")
    @ModelField(primaryKey = true, autoInc = 1)
    var id: Long by model

    @ModelField(index = true)
    @Label("设备")
    var devId: Long by model

    //40001
    @ModelField(index = true)
    @Label("地址")
    var address: Int by model

    //原始值*100
    @ModelField(index = true, defaultValue = "0")
    var valueLong: Long by model

    @ModelField
    var valueText: String? by model

    @ModelField
    var valueHex: String? by model

    @ModelField(index = true)
    @Label("日期")
    var createDate: String? by model

    @ModelField(index = true)
    @Label("时间")
    var createTime: String? by model

    val area: Int get() = address / 10000

    val register: Int get() = address % 10000 - 1

    fun json(): KsonObject {
        return toJson(includes = listOf(this::area, this::register))
    }

    companion object : TableModelClass<PlcHistory>()
}