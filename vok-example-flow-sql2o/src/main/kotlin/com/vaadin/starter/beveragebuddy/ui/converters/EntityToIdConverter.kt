package com.vaadin.starter.beveragebuddy.ui.converters

import com.github.vok.framework.sql2o.db
import com.github.vok.framework.sql2o.findById
import com.vaadin.data.Binder
import com.vaadin.data.Converter
import com.vaadin.data.Result
import com.vaadin.data.ValueContext
import com.vaadin.starter.beveragebuddy.LEntity

class EntityToIdConverter<T: LEntity>(val clazz: Class<T>) : Converter<T?, Long?> {
    override fun convertToModel(value: T?, context: ValueContext?): Result<Long?> =
            Result.ok(value?.id)

    override fun convertToPresentation(value: Long?, context: ValueContext?): T? {
        if (value == null) return null
        return db { con.findById(clazz, value) }
    }
}

inline fun <BEAN, reified ENTITY: LEntity> Binder.BindingBuilder<BEAN, ENTITY?>.toId(): Binder.BindingBuilder<BEAN, Long?> =
        withConverter(EntityToIdConverter<ENTITY>(ENTITY::class.java))
