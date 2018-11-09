package com.github.vok.example.crudflow.person

import com.github.mvysny.karibudsl.v10.div
import com.github.mvysny.karibudsl.v10.navigateToView
import com.github.mvysny.karibudsl.v10.text
import com.github.vokorm.findById
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.Route

/**
 * Demonstrates the ability to pass parameters to views.
 * @author mavi
 */
@Route("person")
class PersonView: VerticalLayout(), HasUrlParameter<Long> {
    override fun setParameter(event: BeforeEvent, id: Long?) {
        val person = (if (id == null) null else Person.findById(id)) ?: return navigateToView<PersonListView>()
        removeAll()
        div {
            text("$person")
        }
    }
}
