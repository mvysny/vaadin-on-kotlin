package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v8.MockVaadin
import com.github.mvysny.kaributesting.v8._find
import com.github.mvysny.kaributesting.v8._get
import com.github.mvysny.kaributesting.v8._value
import com.vaadin.shared.ui.datefield.DateTimeResolution
import com.vaadin.ui.InlineDateField
import com.vaadin.ui.InlineDateTimeField
import com.vaadin.ui.PopupView
import com.vaadin.ui.UI
import java.time.LocalDateTime
import kotlin.test.expect

var DateFilterPopup.isPopupVisible: Boolean
get() = (firstOrNull() as? PopupView)?.isPopupVisible ?: false
set(value) {
    (first() as PopupView).isPopupVisible = value
}

class DateFilterPopupTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("smoke test") {
        DateFilterPopup()
    }

    test("setting stuff works even when not attached") {
        DateFilterPopup().apply {
            value = DateInterval(LocalDateTime.now(), LocalDateTime.now())
            isReadOnly = true
            resolution = DateTimeResolution.DAY
            expect(false) { isPopupVisible }
        }
    }

    test("setting stuff is applied immediately when the popup is opened") {
        UI.getCurrent().dateRangePopup {
            isPopupVisible = true
            isReadOnly = true
            resolution = DateTimeResolution.DAY
            val fields = _find<InlineDateTimeField> { count = 2..2 }
            expect(false) { fields[0].isEnabled }
            expect(false) { fields[1].isEnabled }
            expect(DateTimeResolution.DAY) { fields[0].resolution }
            expect(DateTimeResolution.DAY) { fields[1].resolution }
        }
    }

    test("setting value while popup visible will update the text fields as well") {
        UI.getCurrent().dateRangePopup {
            isPopupVisible = true
            val now = LocalDateTime.now()
            value = DateInterval(now, now)
            val fields = _find<InlineDateTimeField> { count = 2..2 }
            expect(now) { fields[0]._value }
            expect(now) { fields[1]._value }
        }
    }
})
