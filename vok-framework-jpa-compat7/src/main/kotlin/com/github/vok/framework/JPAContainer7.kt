@file:Suppress("DEPRECATION")

package com.github.vok.framework

import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.addon.jpacontainer.provider.CachingBatchableLocalEntityProvider
import com.vaadin.v7.ui.AbstractSelect

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

inline fun <reified T : Any> jpaContainer(): JPAContainer<T> = jpaContainer(T::class.java)

fun <T> jpaContainer(entity: Class<T>): JPAContainer<T> {
    val provider = CachingBatchableLocalEntityProvider(entity, extendedEntityManager)
    val container = JPAContainer(entity)
    container.entityProvider = provider
    return container
}
