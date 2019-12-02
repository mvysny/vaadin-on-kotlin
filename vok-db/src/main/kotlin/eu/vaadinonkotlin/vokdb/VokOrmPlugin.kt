package eu.vaadinonkotlin.vokdb

import eu.vaadinonkotlin.VOKPlugin
import eu.vaadinonkotlin.VaadinOnKotlin
import com.gitlab.mvysny.jdbiorm.JdbiOrm
import javax.sql.DataSource

class VokOrmPlugin : VOKPlugin {

    override fun init() {
    }

    override fun destroy() {
        JdbiOrm.destroy()
    }
}

var VaadinOnKotlin.dataSource: DataSource
    get() = JdbiOrm.getDataSource()
    set(value) { JdbiOrm.setDataSource(value) }
