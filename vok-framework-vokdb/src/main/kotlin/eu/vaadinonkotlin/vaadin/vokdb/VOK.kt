package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.ktormvaadin.ActiveKtorm
import eu.vaadinonkotlin.VaadinOnKotlin
import org.ktorm.database.Database
import javax.sql.DataSource

private var _dataSource: DataSource? = null

/**
 * The JDBC [DataSource] used by VoK for database access. Setting this also connects
 * [ActiveKtorm.database] to the same DataSource, so ktorm queries become available
 * immediately. Typically set once at app boot from a [com.zaxxer.hikari.HikariDataSource].
 */
public var VaadinOnKotlin.dataSource: DataSource
    get() = checkNotNull(_dataSource) { "VaadinOnKotlin.dataSource has not been set yet" }
    set(value) {
        _dataSource = value
        ActiveKtorm.database = Database.connect(value)
    }
