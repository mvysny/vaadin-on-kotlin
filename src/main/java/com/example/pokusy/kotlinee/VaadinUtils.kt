package com.example.pokusy.kotlinee

import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.addon.jpacontainer.provider.CachingBatchableLocalEntityProvider
import com.vaadin.data.Container
import com.vaadin.shared.ui.label.ContentMode
import com.vaadin.ui.*

/**
 * Kedysi som extendoval LocalEntityProvider, ale ten debil loadoval este aj IDcka po jednom: SELECT ID AS a1 FROM notepad_category_item ORDER BY ID ASC LIMIT 1 OFFSET 1 atd atd.
 * Presiel som na CachingBatchableLocalEntityProvider, pretoze ten:
 * 1. vie batchovo vykonat jeden select na IDcka, na rozdiel od kokota LocalEntityProvidera
 * 2. vie cachovat loadnute entity
 * Stale sice chuj loaduje entity jednu po druhej ako user scrolluje, ale aspon ich cachuje.
 *
 *
 * Skusal som to nahradit Viritin lazy listami, ale ten ma plnu rit dependencies; navyse, IDcka vo viritine su samotne entity,
 * co je fajn, ale nie uplne fajn ked user stlaci select all.
 * Nakolko select from bla where ID = ? je mega rychle (menej ako 1ms), nech si ich donacitava postupne.
 * @author mvy
 */
fun <T> createContainer(entity: Class<T>): JPAContainer<T> {
    // @todo this leaks database connection! create entity manager which automatically closes when the request finishes
    // and re-attaches on demand.
    val provider = CachingBatchableLocalEntityProvider(entity, PersistenceContext.Companion.create().em)
    val container = JPAContainer(entity)
    container.entityProvider = provider
    return container
}

private fun <T : Component> HasComponents.init(component: T, block: T.()->Unit = {}): T {
    component.block()
    when (this) {
        is ComponentContainer -> addComponent(component)
        is SingleComponentContainer -> content = component
        else -> throw RuntimeException("Unsupported component container $this")
    }
    return component
}

fun HasComponents.verticalLayout(block: VerticalLayout.()->Unit = {}) = init(VerticalLayout(), block)

fun HasComponents.horizontalLayout(block: HorizontalLayout.()->Unit = {}) = init(HorizontalLayout(), block)

fun HasComponents.button(caption: String? = null, block: Button.() -> Unit = {}) = init(Button(caption), block)

fun HasComponents.grid(caption: String? = null, dataSource: Container.Indexed? = null, block: Grid.() -> Unit = {}) = init(Grid(caption, dataSource), block)

fun HasComponents.textField(caption: String? = null, value: String? = null, block: TextField.()->Unit = {}): TextField {
    val textField = TextField(caption, value)
    init(textField, block)
    textField.nullRepresentation = ""
    return textField
}

fun HasComponents.label(content: String? = null, block: Label.()->Unit = {}) = init(Label(content), block)

fun Label.html(html: String?) {
    setContentMode(ContentMode.HTML)
    setValue(html)
}
