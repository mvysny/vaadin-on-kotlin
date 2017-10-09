/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.starter.beveragebuddy.ui

import com.github.vok.karibudsl.flow.VaadinDsl
import com.github.vok.karibudsl.flow.init
import com.vaadin.shared.Registration
import com.vaadin.ui.Component
import com.vaadin.ui.Tag
import com.vaadin.ui.Text
import com.vaadin.ui.common.HasComponents
import com.vaadin.ui.common.HasStyle
import com.vaadin.ui.common.HtmlImport
import com.vaadin.ui.event.ComponentEvent
import com.vaadin.ui.event.ComponentEventListener
import com.vaadin.ui.event.DomEvent

/**
 * Wrapper for the paper-toast element.
 *
 * See https://www.webcomponents.org/element/PolymerElements/paper-toast/elements/paper-toast for details.
 */
@Tag("paper-toast")
@HtmlImport("frontend://bower_components/paper-toast/paper-toast.html")
class PaperToast : Component(), HasStyle {

    /**
     * The duration in milliseconds to show the toast.
     * Set to 0, a negative number, or Infinity, to disable the toast auto-closing.
     */
    var duration: Int
        get() = element.getProperty("duration", 3000)
        set(duration) {
            element.setProperty("duration", duration.toDouble())
        }

    /**
     * The orientation against which to align the dropdown content horizontally
     * relative to `positionTarget`.
     */
    var horizontalAlign: String
        get() = element.getProperty("horizontalAlign", "left")
        set(horizontalAlign) {
            element.setProperty("horizontalAlign", horizontalAlign)
        }

    /**
     * Enables or disables auto-focusing the toast or child nodes with the autofocus attribute
     * when the overlay is opened.
     * Set to true to disable auto-focusing.
     */
    var noAutoFocus: Boolean
        get() = element.getProperty("noAutoFocus", true)
        set(noAutoFocus) {
            element.setProperty("noAutoFocus", noAutoFocus)
        }

    /**
     * Enables or disables closing of the toast by clicking outside it.
     * false to enable closing of the toast by clicking outside it or true to disable it
     */
    var noCancelOnOutsideClick: Boolean
        get() = element.getProperty("noCancelOnOutsideClick", true)
        set(noCancelOnOutsideClick) {
            element.setProperty("noCancelOnOutsideClick", noCancelOnOutsideClick)
        }

    /**
     * The [text] to display in the toast.
     */
    var text: String
        get() = element.getProperty("text", "")
        set(text) {
            element.setProperty("text", text)
        }

    /**
     * The orientation against which to align the dropdown content vertically
     * relative to `positionTarget`.
     */
    var verticalAlign: String
        get() = element.getProperty("verticalAlign", "bottom")
        set(verticalAlign) {
            element.setProperty("verticalAlign", verticalAlign)
        }

    /**
     * The paper-toast background-color.
     */
    var backgroundColor: String
        get() = element.style.get("backgroundColor")
        set(backgroundColor) {
            element.style.set("backgroundColor", backgroundColor)
        }

    /**
     * The paper-toast color.
     */
    var color: String
        get() = element.style.get("color")
        set(color) {
            element.style.set("color", color)
        }

    /**
     * Shows the toast.
     */
    fun show() {
        element.callFunction("open")
    }

    /**
     * Shows the toast with the given text.
     * @param text the text to display
     */
    fun show(text: String) {
        element.callFunction("show", text)
    }

    /**
     * Shows the toast with the given text for the given duration (in milliseconds).
     * @param text the text to display
     * @param duration the duration in milliseconds to show the toast
     */
    fun show(text: String, duration: Int) {
        this.duration = duration
        show(text)
    }

    /**
     * Hides the toast.
     */
    fun hide() {
        element.callFunction("hide")
    }

    /**
     * Adds a listener for the iron-announce event.
     * @param ironAnnounceListener  the listener to add
     * @return  the registration which allows the listener to be removed
     */
    fun addIronAnnounceListener(
            ironAnnounceListener: ComponentEventListener<IronAnnounceEvent>): Registration {
        return super.addListener(IronAnnounceEvent::class.java, ironAnnounceListener)
    }
}

fun (@VaadinDsl HasComponents).paperToast(block: (@VaadinDsl PaperToast).() -> Unit = {}) = init(PaperToast()) {
    addClassName("notification")
    block()
}

@DomEvent("iron-announce")
class IronAnnounceEvent(source: PaperToast, fromClient: Boolean) : ComponentEvent<PaperToast>(source, fromClient)
