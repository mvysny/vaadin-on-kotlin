package eu.vaadinonkotlin.vaadin8

import com.github.mvysny.karibudsl.v8.karibuDslI18n
import eu.vaadinonkotlin.VOKPlugin

class Vaadin8UtilPlugin : VOKPlugin {
    override fun init() {
        karibuDslI18n = { key -> vt["dsl.$key"] }
    }

    override fun destroy() {}
}
