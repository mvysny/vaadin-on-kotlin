package eu.vaadinonkotlin.sql2o

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.expectList
import com.github.vokorm.Filter
import com.github.vokorm.dataloader.DataLoader
import com.github.vokorm.dataloader.SortClause
import kotlin.test.expect

class FetchLimitOvercomerTest : DynaTest({
    test("empty delegate") {
        expectList() { emptydl.overcomeFetchLimit(1).fetch(range = 0..9) }
        expectList() { emptydl.overcomeFetchLimit(5).fetch(range = 0..9) }
        expectList() { emptydl.overcomeFetchLimit(9).fetch(range = 0..9) }
        expectList() { emptydl.overcomeFetchLimit(10).fetch(range = 0..9) }
        expectList() { emptydl.overcomeFetchLimit(20).fetch(range = 0..9) }
        expectList() { emptydl.overcomeFetchLimit(100).fetch(range = 0..9) }
        expectList() { emptydl.overcomeFetchLimit(1).fetch(range = 10..19) }
        expectList() { emptydl.overcomeFetchLimit(5).fetch(range = 10..19) }
        expectList() { emptydl.overcomeFetchLimit(9).fetch(range = 10..19) }
        expectList() { emptydl.overcomeFetchLimit(10).fetch(range = 10..19) }
        expectList() { emptydl.overcomeFetchLimit(20).fetch(range = 10..19) }
        expectList() { emptydl.overcomeFetchLimit(100).fetch(range = 10..19) }
    }

    test("delegate with 1 item") {
        val dl = listOf(25).dl
        expectList(25) { dl.overcomeFetchLimit(1).fetch(range = 0..9) }
        expectList(25) { dl.overcomeFetchLimit(5).fetch(range = 0..9) }
        expectList(25) { dl.overcomeFetchLimit(9).fetch(range = 0..9) }
        expectList(25) { dl.overcomeFetchLimit(10).fetch(range = 0..9) }
        expectList(25) { dl.overcomeFetchLimit(20).fetch(range = 0..9) }
        expectList(25) { dl.overcomeFetchLimit(100).fetch(range = 0..9) }
        expectList() { dl.overcomeFetchLimit(1).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(5).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(9).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(10).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(20).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(100).fetch(range = 10..19) }
    }

    test("delegate with 5 items") {
        val dl = listOf(0, 1, 2, 3, 4).dl
        expectList(0, 1, 2, 3, 4) { dl.overcomeFetchLimit(1).fetch(range = 0..9) }
        expectList(0, 1, 2, 3, 4) { dl.overcomeFetchLimit(5).fetch(range = 0..9) }
        expectList(0, 1, 2, 3, 4) { dl.overcomeFetchLimit(9).fetch(range = 0..9) }
        expectList(0, 1, 2, 3, 4) { dl.overcomeFetchLimit(10).fetch(range = 0..9) }
        expectList(0, 1, 2, 3, 4) { dl.overcomeFetchLimit(20).fetch(range = 0..9) }
        expectList(0, 1, 2, 3, 4) { dl.overcomeFetchLimit(100).fetch(range = 0..9) }
        expectList() { dl.overcomeFetchLimit(1).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(5).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(9).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(10).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(20).fetch(range = 10..19) }
        expectList() { dl.overcomeFetchLimit(100).fetch(range = 10..19) }
    }

    test("delegate with 20 items") {
        val dl = (0..19).toList().dl
        expect((0..9).toList()) { dl.overcomeFetchLimit(1).fetch(range = 0..9) }
        expect((0..9).toList()) { dl.overcomeFetchLimit(5).fetch(range = 0..9) }
        expect((0..9).toList()) { dl.overcomeFetchLimit(9).fetch(range = 0..9) }
        expect((0..9).toList()) { dl.overcomeFetchLimit(10).fetch(range = 0..9) }
        expect((0..9).toList()) { dl.overcomeFetchLimit(20).fetch(range = 0..9) }
        expect((0..9).toList()) { dl.overcomeFetchLimit(100).fetch(range = 0..9) }
        expect((10..19).toList()) { dl.overcomeFetchLimit(1).fetch(range = 10..19) }
        expect((10..19).toList()) { dl.overcomeFetchLimit(5).fetch(range = 10..19) }
        expect((10..19).toList()) { dl.overcomeFetchLimit(9).fetch(range = 10..19) }
        expect((10..19).toList()) { dl.overcomeFetchLimit(10).fetch(range = 10..19) }
        expect((10..19).toList()) { dl.overcomeFetchLimit(20).fetch(range = 10..19) }
        expect((10..19).toList()) { dl.overcomeFetchLimit(100).fetch(range = 10..19) }
    }
})

val List<Int>.dl: DataLoader<Int> get() = SimpleListDataLoader(this)
val emptydl = listOf<Int>().dl

class SimpleListDataLoader(val list: List<Int>) : DataLoader<Int> {
    override fun fetch(filter: Filter<Int>?, sortBy: List<SortClause>, range: IntRange): List<Int> {
        require(filter == null) { "filter is unsupported" }
        require(sortBy.isEmpty()) { "sortBy is unsupported" }
        val start = range.start.coerceAtMost(list.size)
        val toIndex = (range.endInclusive + 1).coerceAtMost(list.size)
        return list.subList(start, toIndex)
    }

    override fun getCount(filter: Filter<Int>?): Int {
        require(filter == null) { "filter is unsupported" }
        return list.size
    }
}
