package eu.vaadinonkotlin.vaadin8.vokdb

import com.github.vokorm.KEntity
import com.gitlab.mvysny.jdbiorm.Dao
import com.vaadin.data.Binder
import com.vaadin.data.Converter
import com.vaadin.data.Result
import com.vaadin.data.ValueContext

/**
 * Converts an entity to its ID and back. Useful for combo boxes which shows a list of entities as their options while being bound to a
 * field containing ID of that entity.
 * @param T the type of the entity
 * @param ID the type of the ID field of the entity
 */
class EntityToIdConverter<ID: Any, T: KEntity<ID>>(val dao: Dao<T, ID>) : Converter<T?, ID?> {
    constructor(clazz: Class<T>) : this(Dao<T, ID>(clazz))
    override fun convertToModel(value: T?, context: ValueContext?): Result<ID?> =
        Result.ok(value?.id)

    override fun convertToPresentation(value: ID?, context: ValueContext?): T? {
        if (value == null) return null
        return dao.findById(value)
    }
}

/**
 * Converts an entity to its ID and back. Useful for combo boxes which shows a list of entities as their options while being bound to a
 * field containing ID of that entity:
 * ```kotlin
 * data class Category(override var id: Long? = null, var name: String = "") : Entity<Long>
 * data class Review(override var id: Long? = null, var category: Long? = null) : Entity<Long>
 *
 * // editing the Review, we want the user to be able to choose the Review's category
 * val binder = BeanValidationBinder(Review::class.java)
 * val categoryBox = comboBox("Choose a category") {
 *     setItemCaptionGenerator { it.name }
 *     isTextInputAllowed = false
 *     dataProvider = Category.dataProvider
 *     bind(binder).toId().bind(Review::category)
 * }
 * ```
 */
inline fun <BEAN, ID: Any, reified ENTITY: KEntity<ID>> Binder.BindingBuilder<BEAN, ENTITY?>.toId(): Binder.BindingBuilder<BEAN, ID?> =
    withConverter(EntityToIdConverter(ENTITY::class.java))
