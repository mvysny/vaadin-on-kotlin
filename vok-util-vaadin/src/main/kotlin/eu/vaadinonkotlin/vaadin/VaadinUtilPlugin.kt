package eu.vaadinonkotlin.vaadin

import com.github.mvysny.karibudsl.v10.karibuDslI18n
import eu.vaadinonkotlin.VOKPlugin

public class VaadinUtilPlugin : VOKPlugin {
    override fun init() {
        karibuDslI18n = { key -> vt["dsl.$key"] }
    }

    override fun destroy() {}
}
