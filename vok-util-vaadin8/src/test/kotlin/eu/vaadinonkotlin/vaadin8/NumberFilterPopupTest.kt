package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v8.*
import com.vaadin.ui.*
import kotlin.test.expect
import kotlin.test.fail

class NumberFilterPopupTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }
    lateinit var component: NumberFilterPopup
    beforeEach { component = NumberFilterPopup(); UI.getCurrent().content = component }

    test("Initial value is null") {
        expect(null) { component._value }
    }

    test("setting the value preserves the value") {
        component._value = NumberInterval(5.0, 25.0)
        expect(NumberInterval(5.0, 25.0)) { component._value!! }
    }

    group("value change listener tests") {
        test("Setting to the same value does nothing") {
            component.addValueChangeListener {
                fail("should not be fired")
            }
            component._value = null
        }

        test("Setting the value programatically triggers value change listeners") {
            lateinit var newValue: NumberInterval<Double>
            component.addValueChangeListener {
                expect(false) { it.isUserOriginated }
                expect(null) { it.oldValue }
                newValue = it.value!!
            }
            component._value = NumberInterval(5.0, 25.0)
            expect(NumberInterval(5.0, 25.0)) { newValue }
        }

        test("value change won't trigger unregistered change listeners") {
            component.addValueChangeListener {
                fail("should not be fired")
            } .remove()
            component._value = NumberInterval(5.0, 25.0)
        }
    }

    group("popup tests") {
        beforeEach {
            // open popup
            component.isPopupVisible = true
            expect(1) { component._get<PopupView>().componentCount }  // expect the dialog to pop up
        }

        test("Clear does nothing when the value is already null") {
            component.addValueChangeListener {
                fail("No listener must be fired")
            }
            _get<Button> { caption = "Clear" } ._click()
            expect(null) { component._value }
            expect(false) { component._get<PopupView>().isPopupVisible }  // the Clear button must close the dialog
        }

        test("setting the value while the dialog is opened propagates the values to date fields") {
            component._value = NumberInterval(5.0, 25.0)
            expect("5") { _get<TextField> { placeholder = "at least" } ._value }
            expect("25") { _get<TextField> { placeholder = "at most" } ._value }
        }

        test("Clear properly sets the value to null") {
            component._value = NumberInterval(25.0, 35.0)
            var wasCalled = false
            component.addValueChangeListener {
                expect(true) { it.isUserOriginated }
                expect(null) { it.value }
                wasCalled = true
            }
            _get<Button> { caption = "Clear" } ._click()
            expect(true) { wasCalled }
            expect(null) { component._value }
            expect(false) { component._get<PopupView>().isPopupVisible }  // the Clear button must close the dialog
        }

        test("Set properly sets the value to null if nothing is filled in") {
            component._value = NumberInterval(25.0, 35.0)
            var wasCalled = false
            component.addValueChangeListener {
                expect(true) { it.isUserOriginated }
                expect(null) { it.value }
                wasCalled = true
            }
            _get<TextField> { placeholder = "at least" } ._value = ""
            _get<TextField> { placeholder = "at most" } ._value = ""
            _get<Button> { caption = "Ok" } ._click()
            expect(true) { wasCalled }
            expect(null) { component._value }
            expect(false) { component._get<PopupView>().isPopupVisible }  // the Set button must close the dialog
        }

        test("Set properly sets the value in") {
            var wasCalled = false
            component.addValueChangeListener {
                expect(true) { it.isUserOriginated }
                expect(NumberInterval(25.0, 35.0)) { it.value }
                wasCalled = true
            }
            _get<TextField> { placeholder = "at least" } ._value = "25"
            _get<TextField> { placeholder = "at most" } ._value = "35"
            _get<Button> { caption = "Ok" } ._click()
            expect(true) { wasCalled }
            expect(NumberInterval(25.0, 35.0)) { component._value }
            expect(false) { component._get<PopupView>().isPopupVisible }  // the Set button must close the dialog
        }

        test("setting readOnly disables the fields") {
            component.isReadOnly = true
            expect(false) { _get<TextField> { placeholder = "at least" } .isEnabled }
            expect(false) { _get<TextField> { placeholder = "at most" } .isEnabled }
        }
    }

    group("lazy popup") {
        test("setting readOnly disables the fields") {
            component.isReadOnly = true
            component.isPopupVisible = true
            expect(false) { _get<TextField> { placeholder = "at least" } .isEnabled }
            expect(false) { _get<TextField> { placeholder = "at most" } .isEnabled }
        }

        test("setting values before popup is open will populate the fields") {
            component._value = NumberInterval(5.0, 25.0)
            component.isPopupVisible = true
            expect("5") { _get<TextField> { placeholder = "at least" } ._value }
            expect("25") { _get<TextField> { placeholder = "at most" } ._value }
        }
    }
})
