package net.wuillemin.jds.dataserver.supplier.sql.util

import java.sql.ResultSet


/**
 * Creates an iterator through a [ResultSet]
 */
operator fun ResultSet.iterator(): Iterator<ResultSet> {

    val rs = this

    return object : Iterator<ResultSet> {
        override fun hasNext(): Boolean = rs.next()

        override fun next(): ResultSet = rs
    }
}

/**
 * Returns iterable that calls to the specified mapper function for each row
 * @param fn The function to apply to each row
 * @return a list with results of the function applied to the row
 */
fun <T> ResultSet.map(fn: (ResultSet) -> T): Iterable<T> {

    val rs = this

    val iterator = object : Iterator<T> {
        override fun hasNext(): Boolean = rs.next()

        override fun next(): T = fn(rs)
    }

    return object : Iterable<T> {
        override fun iterator(): Iterator<T> = iterator
    }
}

/**
 * Returns a list that calls to the specified mapper function for each row
 * @param fn The function to apply to each row
 * @return a list with the non-null results of the function applied to the row
 */
fun <T> ResultSet.mapNotNull(fn: (ResultSet) -> T?): List<T> {

    val rs = this

    val iterator = object : Iterator<T?> {
        override fun hasNext(): Boolean = rs.next()

        override fun next(): T? = fn(rs)
    }

    val destination = ArrayList<T>(10)
    iterator.forEach { element -> element?.let { destination.add(it) } }
    return destination
}

