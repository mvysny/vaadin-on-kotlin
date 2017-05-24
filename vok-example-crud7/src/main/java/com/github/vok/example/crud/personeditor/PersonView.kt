package com.github.vok.example.crud.personeditor

import com.github.vok.example.crud.WelcomeView
import com.github.vok.framework.db
import com.github.vok.framework.vaadin.ViewName
import com.github.vok.framework.vaadin.navigateToView
import com.github.vok.framework.vaadin.parameterList
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.ui.Label
import com.vaadin.ui.VerticalLayout

/**
 * Demonstrates the ability to pass parameters to views.
 * @author mavi
 */
@ViewName
class PersonView: VerticalLayout(), View {
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
        val id = event.parameterList[0]?.toLong()
        val person = (if (id == null) null else db { em.find(Person::class.java, id)}) ?: return navigateToView<WelcomeView>()
        removeAllComponents()
        addComponent(Label(person.toString()))
    }

    companion object {
        fun navigateTo(person: Person) {
            navigateToView<PersonView>(person.id!!.toString())
        }
    }
}
