package io.github.yangentao.modbus.model

import io.github.yangentao.anno.Label
import io.github.yangentao.anno.Length
import io.github.yangentao.anno.ModelField
import io.github.yangentao.modbus.proto.BusAddress
import io.github.yangentao.modbus.proto.BusWriteRequest
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.clause.ASC
import io.github.yangentao.sql.clause.ORDER_BY
import io.github.yangentao.types.low0

//40001 从1开始
@Label("PLC地址")
class PlcAddress : TableModel() {
    //40001
    @Label("地址4")
    @ModelField(primaryKey = true)
    var address: Int by model

    @Label("版本")
    @ModelField(primaryKey = true, defaultValue = "0")
    var ver: Int by model

    @ModelField
    @Label("描述")
    var descs: String by model

    //@see AddrType
    @ModelField(index = true, defaultValue = "0")
    @Label("数据类型")
    @Length(max = 8)
    var addrType: String? by model

    // enum:0=已注册:1=未注册
    // range:2024:2099
    // warn:1
    // confirm
    // default:0
    // single:1
    // 多种组合用分号隔开,  single:1;default:0
    // ';' -> ':' -> '=' -> ','
    @Label("数据定义")
    @Length(max = 512)
    @ModelField()
    var valuedef: String? by model

    @Label("仅管理员")
    @ModelField(defaultValue = "0")
    var adminOnly: Int by model

    @Label("只读")
    @ModelField(defaultValue = "0")
    var readonly: Int by model

    @Label("报警标志")
    @ModelField(defaultValue = "0")
    var iswarn: Int by model

    @Label("报警值")
    @ModelField
    var warnTrue: Int? by model

    @Label("非报警值")
    @ModelField
    var warnFalse: Int? by model

    @Label("备注")
    @ModelField
    var remarks: String? by model

    // button           用按钮显示, 用于0区
    // buttons          用按钮组显示,用于0区, 0,1两个值生成两个按钮, 对应的valuedef应该用enum来定义标签
    // fraction:0       浮点小数点位数
    // title:value_label  按钮/开关/listtile的显示标签
    // outlined         outline样式的按钮
    // switch;on=0;label=自动大小火;   开关,值是0时是开启状态
    // rowmax           填充水平空间,  switch;on=0;label=自动大小火;rowmax
    @Label("展示")
    @Length(max = 512)
    @ModelField
    var display: String? by model

    @Label("单位")
    @Length(max = 16)
    @ModelField
    var unit: String? by model

    @Label("记录历史数据")
    @ModelField(defaultValue = "0")
    var recordHistory: Int by model

    // 假如, 传来的温度值是 30.12, PlcValue.valueLong和ValueHistory.valueLong是Long(30.12*100) = 3012
    // 这个值可能变化很快 30.12, 30.13, 30.14 ....这样会产生非常多的历史记录
    // historyMod的目的是减少历史记录.
    // 30.12 * 100 / historyMod,  这个值跟前一个值相同的,就不再记录
    // If historyMod = 100;  Long(30.12 * 100) /100 = 30, 就是跟上次数据相比, 不超过1度的不再记录.
    // If historyMod = 10;  Long(30.12 * 100) /10 = 301, 就是跟上次数据相比, 不超过0.1度的不再记录.
    // 只影响是否记录该数据, 不影响数据原来的值
    @Label("历史数据")
    @ModelField(defaultValue = "0")
    var historyMod: Int by model

    @Label("第二页显示")
    @ModelField(defaultValue = "0")
    var page2: Int by model

    @Label("定时查询")
    @ModelField(defaultValue = "0")
    var autoQuery: Int by model

    val area: Int get() = address / 10000
    val register: Int get() = address % 10000 - 1

    fun writeValue(value: String): BusWriteRequest? {
        return writeValueTo(this.addrType, this.address, value)
    }

    fun isWarning(value: Int): Boolean {
        if (this.iswarn != 1) return false
        if (this.warnTrue == value) return true
        return this.warnFalse != null && this.warnFalse != value
    }

    companion object : TableModelClass<PlcAddress>() {
        private var addressList: List<PlcAddress> = emptyList()
        private var addressMap: Map<Int, PlcAddress> = emptyMap()

        fun trySync(force: Boolean = false) {
            if (force || addressList.isEmpty()) {
                addressList = list { ORDER_BY(PlcAddress::address.ASC) }
                addressMap = addressList.associateBy { it.ver * 100_000 + it.address }

            }
        }

        fun addr(ver: Int, address: Int): PlcAddress? {
            trySync()
            return addressMap[ver * 100_000 + address]
        }

        fun all(ver: Int?): List<PlcAddress> {
            trySync()
            if (ver == null) return addressList
            return addressList.filter { it.ver == ver }
        }

        fun area(ver: Int, area: Int): List<PlcAddress> {
            trySync()
            return addressList.filter { it.ver == ver && it.area == area }
        }

        fun writeValueTo(addrType: String?, address: Int, value: String): BusWriteRequest? {
            val floatValue: Float = value.toFloatOrNull() ?: return null
            val intVal: Int = floatValue.toInt()
            val addr = BusAddress(address)
            return when (addr.area) {
                0 -> addr.write(1, byteArrayOf((intVal and 0x01).low0))
                4 -> {
                    val t = addrType ?: "N10"
                    val bytes = addressType2Bytes(t, floatValue, intVal)
                    if (AddrType.isByte4(t)) {
                        addr.write(2, bytes)
                    } else {
                        addr.write(1, bytes)
                    }
                }

                else -> null
            }
        }

    }

}