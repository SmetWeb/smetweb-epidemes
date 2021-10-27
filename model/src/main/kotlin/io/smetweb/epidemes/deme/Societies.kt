package io.smetweb.epidemes.deme

import io.smetweb.epidemes.data.duo.DuoPedagogy
import io.smetweb.math.Table

internal interface Societies {

    class SocietyName(value: String): Table.Property<String>(value)

    class Purpose(value: String): Table.Property<String>(value)

    class EduCulture(value: DuoPedagogy): Table.Property<DuoPedagogy>(value)

    class SiteRef(value: Comparable<*>): Table.Property<Comparable<*>>(value)

    class Capacity(value: Int): Table.Property<Int>(value)

    class MemberCount(value: Int): Table.Property<Int>(value)

    companion object {

        @JvmStatic
        val PROPERTIES: List<Class<out Table.Property<*>>> = listOf(
            EduCulture::class.java,
            MemberCount::class.java,
            Purpose::class.java,
            Capacity::class.java,
            SocietyName::class.java,
            SiteRef::class.java,
        )
    }
}