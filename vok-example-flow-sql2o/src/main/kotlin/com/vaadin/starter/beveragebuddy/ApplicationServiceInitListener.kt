package com.vaadin.starter.beveragebuddy

import com.vaadin.server.ServiceInitEvent
import com.vaadin.server.VaadinServiceInitListener
import org.jsoup.nodes.Element

class ApplicationServiceInitListener : VaadinServiceInitListener {

    override fun serviceInit(event: ServiceInitEvent) {
        event.addBootstrapListener { response ->
            val document = response.document
            document.head().apply {
                meta("viewport", "width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
                meta("apple-mobile-web-app-capable", "yes")
                meta("apple-mobile-web-app-status-bar-style", "black")
            }
        }
    }

    private fun Element.meta(name: String, content: String) {
        val meta = ownerDocument().createElement("meta").apply {
            attr("name", name)
            attr("content", content)
        }
        appendChild(meta)
    }
}
