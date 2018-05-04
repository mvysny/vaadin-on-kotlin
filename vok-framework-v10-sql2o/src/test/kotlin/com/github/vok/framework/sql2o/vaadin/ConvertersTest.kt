package com.github.vok.framework.sql2o.vaadin

import com.github.karibu.testing.v10.MockVaadin
import com.github.mvysny.dynatest.DynaTest
import com.github.vok.karibudsl.flow.bind
import com.github.vokorm.Entity
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.data.binder.BeanValidationBinder
import kotlin.test.expect

class ConvertersTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    test("toId() test") {
        data class Category(override var id: Long? = null, var name: String = "") : Entity<Long>
        data class Review(override var id: Long? = null, var category: Long? = null) : Entity<Long>

        val binder = BeanValidationBinder(Review::class.java)
        val categoryBox = ComboBox<Category>("Choose a category").apply {
            setItemLabelGenerator { it.name }
            isAllowCustomValue = false
            setItems(Category(1L, "cat"))
            bind(binder).toId().bind(Review::category)
        }

        val r = Review()
        binder.readBean(r)
        expect(null) { categoryBox.value }
        categoryBox.value = Category(1L, "cat")
        binder.writeBean(r)
        expect(1L) { r.category }
    }
})
