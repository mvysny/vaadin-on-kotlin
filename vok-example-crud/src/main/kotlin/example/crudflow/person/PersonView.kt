package example.crudflow.person

import com.github.mvysny.karibudsl.v10.div
import com.github.mvysny.karibudsl.v10.text
import com.github.mvysny.kaributools.navigateTo
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
        val person: Person = (if (id == null) null else Person.findById(id)) ?: return navigateTo<PersonListView>()
        removeAll()
        div {
            text("$person")
        }
    }
}
