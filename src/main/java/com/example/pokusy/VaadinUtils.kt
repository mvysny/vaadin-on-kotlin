package com.example.pokusy

import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.addon.jpacontainer.provider.CachingBatchableLocalEntityProvider

/**
 * Kedysi som extendoval LocalEntityProvider, ale ten debil loadoval este aj IDcka po jednom: SELECT ID AS a1 FROM notepad_category_item ORDER BY ID ASC LIMIT 1 OFFSET 1 atd atd.
 * Presiel som na CachingBatchableLocalEntityProvider, pretoze ten:
 * 1. vie batchovo vykonat jeden select na IDcka, na rozdiel od kokota LocalEntityProvidera
 * 2. vie cachovat loadnute entity
 * Stale sice chuj loaduje entity jednu po druhej ako user scrolluje, ale aspon ich cachuje.
 *
 *
 * Skusal som to nahradit Viritin lazy listami, ale ten ma plnu rit dependencies; navyse, IDcka vo viritine su samotne entity,
 * co je fajn, ale nie uplne fajn ked user stlaci select all.
 * Nakolko select from bla where ID = ? je mega rychle (menej ako 1ms), nech si ich donacitava postupne.
 * @author mvy
 */
fun <T> createContainer(entity: Class<T>): JPAContainer<T> {
    val provider = CachingBatchableLocalEntityProvider(entity, PersistenceContext.create().em)
    val container = JPAContainer(entity)
    container.entityProvider = provider
    return container
}
