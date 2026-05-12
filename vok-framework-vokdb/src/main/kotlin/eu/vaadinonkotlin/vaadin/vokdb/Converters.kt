package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.ktormvaadin.EntityToIdConverter
import com.vaadin.flow.data.binder.Binder
import org.ktorm.entity.Entity
import org.ktorm.schema.Column

/**
 * Converts an entity to its ID and back. Useful for combo boxes which show a list
 * of entities as their options while being bound to a field containing the ID of
 * that entity:
 *
 * ```kotlin
 * interface Category : Entity<Category> { var id: Long?; var name: String }
 * interface Review : Entity<Review> { var id: Long?; var category: Long? }
 *
 * val binder = BeanValidationBinder(Review::class.java)
 * categoryBox = comboBox("Choose a category") {
 *     setItemLabelGenerator { it.name }
 *     isAllowCustomValue = false
 *     setItems(Categories.dataProvider.withStringFilterOn(Categories.name))
 *     bind(binder).toId(Categories.id).bind(Review::category)
 * }
 * ```
 */
public inline fun <BEAN, ID: Any, reified ENTITY: Entity<ENTITY>>
    Binder.BindingBuilder<BEAN, ENTITY?>.toId(idColumn: Column<ID>): Binder.BindingBuilder<BEAN, ID?> =
    withConverter(EntityToIdConverter(idColumn, ENTITY::class))
