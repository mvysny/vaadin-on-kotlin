package eu.vaadinonkotlin.vaadin10

import com.github.mvysny.karibudsl.v10.karibuDslI18n
import eu.vaadinonkotlin.VOKPlugin

class Vaadin10UtilPlugin : VOKPlugin {
    override fun init() {
        karibuDslI18n = { key -> vt["dsl.$key"] }
    }

    override fun destroy() {}
}
