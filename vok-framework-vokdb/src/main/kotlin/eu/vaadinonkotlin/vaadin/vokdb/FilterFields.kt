package eu.vaadinonkotlin.vaadin.vokdb

import com.gitlab.mvysny.jdbiorm.vaadin.filter.EnumFilterField

public inline fun <reified E: Enum<E>> enumFilterField(): EnumFilterField<E> = EnumFilterField(E::class.java)
