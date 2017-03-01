package com.github.vok.framework.vaadin7

import com.github.vok.framework.vaadin.init
import com.vaadin.ui.HasComponents
import com.vaadin.v7.data.Container
import com.vaadin.v7.ui.Calendar
import com.vaadin.v7.ui.Table
import com.vaadin.v7.ui.TreeTable

// Vaadin 7 compat stuff
@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.treeTable(caption: String? = null, dataSource: Container? = null, block: TreeTable.()->Unit = {}) =
        init(TreeTable(caption, dataSource), block)
@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.table(caption: String? = null, dataSource: Container? = null, block: Table.()->Unit = {}) =
        init(Table(caption, dataSource), block)
@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.calendar(caption: String? = null, block: Calendar.()->Unit = {}) = init(Calendar(caption), block)
