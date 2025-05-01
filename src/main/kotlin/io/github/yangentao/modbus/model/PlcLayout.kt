package io.github.yangentao.modbus.model

import io.github.yangentao.anno.Length
import io.github.yangentao.anno.ModelField
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass

class PlcLayout : TableModel() {

    @ModelField(primaryKey = true)
    var ver: Int by model

    @ModelField(notNull = true, defaultValue = "''")
    @Length(max = 1024)
    var layout: String by model

    @ModelField(notNull = true, defaultValue = "''")
    @Length(max = 1024)
    var desktop: String by model

    @ModelField(notNull = true, defaultValue = "''")
    @Length(max = 1024)
    var mobile: String by model

    @ModelField
    var remark: String? by model

    companion object : TableModelClass<PlcLayout>() {

    }
}