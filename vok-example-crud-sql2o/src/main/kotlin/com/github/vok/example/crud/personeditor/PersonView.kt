package com.github.vok.example.crud.personeditor

import com.github.vok.example.crud.WelcomeView
import com.github.vok.karibudsl.AutoView
import com.github.vok.karibudsl.navigateToView
import com.github.vok.karibudsl.parameterList
import com.github.vokorm.findById
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.ui.Label
import com.vaadin.ui.VerticalLayout

/**
 * Demonstrates the ability to pass parameters to views.
 * @author mavi
 */
@AutoView
class PersonView: VerticalLayout(), View {
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
        val id = event.parameterList[0]?.toLong()
        val person = (if (id == null) null else Person.findById(id)) ?: return navigateToView<WelcomeView>()
        removeAllComponents()
        addComponent(Label(person.toString()))
    }

    companion object {
        fun navigateTo(person: Person) {
            navigateToView<PersonView>(person.id!!.toString())
        }
    }
}
