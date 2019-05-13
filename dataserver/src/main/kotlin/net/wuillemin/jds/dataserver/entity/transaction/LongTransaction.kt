package net.wuillemin.jds.dataserver.entity.transaction

/**
 * Represent a long transaction that could be reused between multiple data access. A long Transaction should not
 * auto commit data if possible
 */
interface LongTransaction : AutoCloseable {

    /**
     * Makes all changes made since the previous commit/rollback permanent and releases any database locks
     * currently held by this LongTransaction object.
     */
    fun commit()

    /**
     * Undoes all changes made in the current transaction and releases any database locks currently held by this
     * LongTransaction object.
     */
    fun rollback()
}