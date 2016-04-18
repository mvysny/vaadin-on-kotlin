package com.github.kotlinee.framework

import java.util.*

/**
 * Given a tree root and a closure which computes children, iterates recursively over a tree of objects.
 * Iterator works breadth-first: initially the root itself is offered, then its children, then the child's children etc.
 * @property root the root object of the tree
 * @property children for given node, returns a list of its children.
 */
class TreeIterator<T>(private val root: T, private val children:(T)->Iterable<T>): Iterator<T> {
    /**
     * The items to iterate over. Gradually filled with children, until there are no more items to iterate over.
     */
    private val queue: Queue<T> = LinkedList<T>(listOf(root))

    override fun hasNext() = !queue.isEmpty()

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        val result = queue.remove()
        queue.addAll(children(result))
        return result
    }
}
