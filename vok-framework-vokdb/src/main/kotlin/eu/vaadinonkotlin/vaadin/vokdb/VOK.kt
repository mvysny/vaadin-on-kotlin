package eu.vaadinonkotlin.vaadin.vokdb

import com.gitlab.mvysny.jdbiorm.JdbiOrm
import eu.vaadinonkotlin.VaadinOnKotlin
import javax.sql.DataSource

public var VaadinOnKotlin.dataSource: DataSource
    get() = JdbiOrm.getDataSource()
    set(value) { JdbiOrm.setDataSource(value) }
