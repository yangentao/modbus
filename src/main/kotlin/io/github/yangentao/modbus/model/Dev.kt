package io.github.yangentao.modbus.model


import io.github.yangentao.anno.Label
import io.github.yangentao.anno.ModelField
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass

class Dev : TableModel() {
    @Label("ID")
    @ModelField(primaryKey = true)
    var id: Long by model

    @Label("IMEI")
    @ModelField(unique = true)
    var imei: String by model

    @Label("型号")
    @ModelField(defaultValue = "0")
    var ver: Int by model

    @Label("名称")
    @ModelField(index = true, defaultValue = "")
    var name: String? by model

    @Label("IP")
    @ModelField(index = true, defaultValue = "")
    var lastip: String by model

    @Label("RSSI")
    @ModelField(defaultValue = "0")
    var rssi: Int by model

    @Label("Time")
    @ModelField(defaultValue = "")
    var lastTime: String by model

    @Label("锁定")
    @ModelField(defaultValue = "0")
    var state: Int by model

    @Label("备注")
    @ModelField
    var remarks: String? by model

//    @NullValue("0")
//    @Label("用户")
//    var accountId: Int by model

    @ModelField(index = true, defaultValue = "0")
    @Label("在线")
    var online: Int by model

    @ModelField(defaultValue = "0")
    @Label("报警")
    var warnings: Int by model

    @Label("型号")
    @ModelField
    var userModel: String? by model

    @Label("标签1")
    @ModelField
    var tag1: String? by model

    @Label("标签2")
    @ModelField
    var tag2: String? by model

    @Label("标签3")
    @ModelField
    var tag3: String? by model

    companion object : TableModelClass<Dev>()
}