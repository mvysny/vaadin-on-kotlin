package eu.vaadinonkotlin.vaadin.vokdb

import com.github.mvysny.ktormvaadin.filter.EnumFilterField

public inline fun <reified E: Enum<E>> enumFilterField(): EnumFilterField<E> = EnumFilterField(E::class.java)
