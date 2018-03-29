package com.github.vok.framework

import com.github.karibu.testing.MockVaadin
import com.github.mvysny.dynatest.DynaTest

class NumberFilterPopupTest : DynaTest({
    beforeEach { MockVaadin.setup() }
    test("smoke test") {
        NumberFilterPopup()
    }
})