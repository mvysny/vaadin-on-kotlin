package com.github.vok.framework.sql2o

import com.github.vok.framework.VOKPlugin
import com.github.vok.framework.VaadinOnKotlin
import com.github.vokorm.VokOrm
import com.zaxxer.hikari.HikariConfig
import javax.sql.DataSource

class VokOrmPlugin : VOKPlugin {

    override fun init() {
        VokOrm.init()
    }

    override fun destroy() {
        VokOrm.destroy()
    }
}

/**
 * Configure this before initializing VoK. At minimum you need to set [HikariConfig.dataSource], or
 * [HikariConfig.driverClassName], [HikariConfig.jdbcUrl], [HikariConfig.username] and [HikariConfig.password].
 */
val VaadinOnKotlin.dataSourceConfig: HikariConfig get() = VokOrm.dataSourceConfig

val VaadinOnKotlin.dataSource: DataSource get() = VokOrm.dataSource!!
