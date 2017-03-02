@file:Suppress("DEPRECATION")

package com.github.vok.framework.vaadin7

import com.github.vok.framework.extendedEntityManager
import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.addon.jpacontainer.provider.CachingBatchableLocalEntityProvider
import com.vaadin.event.ShortcutAction.KeyCode.ENTER
import com.vaadin.event.ShortcutListener
import com.vaadin.v7.data.fieldgroup.BeanFieldGroup
import com.vaadin.v7.data.util.converter.Converter
import com.vaadin.v7.shared.ui.label.ContentMode
import com.vaadin.v7.ui.AbstractField
import com.vaadin.v7.ui.AbstractSelect
import com.vaadin.v7.ui.AbstractTextField
import com.vaadin.v7.ui.Label
import org.intellij.lang.annotations.Language
import java.util.*


/**
 * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only,
 * simply call [JPAContainer.addContainerFilter] on the container produced.
 *
 * Containers produced by this method have the following properties:
 * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container
 * together with [AbstractSelect] as the select's value is taken amongst the Item ID.
 * * [Item]'s Property IDs are [String] values - the field names of given JPA bean.
 *
 * @param entity the entity type
 * @return the new container which can be assigned to a [Grid]
 */

// have to wait until jpa container is vaadin 8 compatible
// https://github.com/vaadin/framework/issues/8717
inline fun <reified T : Any> jpaContainer(): JPAContainer<T> = jpaContainer(T::class.java)

fun <T> jpaContainer(entity: Class<T>): JPAContainer<T> {
    val provider = CachingBatchableLocalEntityProvider(entity, extendedEntityManager)
    val container = JPAContainer(entity)
    container.entityProvider = provider
    return container
}

/**
 * Shows given html in this label.
 * @param html the html code to show.
 */
@Deprecated("Deprecated in Vaadin 8")
fun Label.html(@Language("HTML") html: String?) {
    contentMode = ContentMode.HTML
    value = html
}

/**
 * Triggers given listener when the text field is focused and user presses the Enter key.
 * @param enterListener the listener to invoke when the user presses the Enter key.
 */
@Deprecated("Deprecated in Vaadin 8")
fun AbstractTextField.onEnterPressed(enterListener: (AbstractTextField) -> Unit) {
    val enterShortCut = object : ShortcutListener("EnterOnTextAreaShorcut", null, ENTER) {
        override fun handleAction(sender: Any, target: Any) {
            enterListener(this@onEnterPressed)
        }
    }
    addFocusListener { addShortcutListener(enterShortCut) }
    addBlurListener { removeShortcutListener(enterShortCut) }
}

/**
 * Trims the user input string before storing it into the underlying property data source. Vital for mobile-oriented apps:
 * Android keyboard often adds whitespace to the end of the text when auto-completion occurs. Imagine storing a username ending with a space upon registration:
 * such person can no longer log in from his PC unless he explicitely types in the space.
 */
@Deprecated("Deprecated in Vaadin 8")
fun AbstractField<String>.trimmingConverter() {
    setConverter(object : Converter<String?, String?> {
        override fun convertToModel(value: String?, targetType: Class<out String?>?, locale: Locale?): String? = value?.trim()

        override fun convertToPresentation(value: String?, targetType: Class<out String?>?, locale: Locale?): String? = value

        @Suppress("UNCHECKED_CAST")
        override fun getPresentationType(): Class<String?> = String::class.java as Class<String?>

        @Suppress("UNCHECKED_CAST")
        override fun getModelType(): Class<String?> = String::class.java as Class<String?>
    })
}

/**
 * An utility method which adds an item and sets item's caption.
 * @param the Identification of the item to be created.
 * @param caption the new caption
 * @return the newly created item ID.
 */
@Deprecated("Deprecated in Vaadin 8")
fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!

/**
 * Allows you to create [BeanFieldGroup] like this: `BeanFieldGroup<Person>()` instead of `BeanFieldGroup<Person>(Person::class.java)`
 */
@Deprecated("Deprecated in Vaadin 8")
inline fun <reified T : Any> BeanFieldGroup(): BeanFieldGroup<T> = BeanFieldGroup(T::class.java)
