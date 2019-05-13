package net.wuillemin.jds.dataserver.entity.transaction.sql

import net.wuillemin.jds.dataserver.entity.transaction.LongTransaction
import java.sql.Connection

/**
 * Represents a long running transaction on a SQL database
 *
 * @param connection The connection
 */
data class LongTransactionSQL(
    val connection: Connection
) : LongTransaction {

    override fun rollback() {
        connection.rollback()
    }

    override fun commit() {
        connection.commit()
    }

    /**
     * Closes this resource, relinquishing any underlying resources. This method is invoked automatically on objects
     * managed by the try-with-resources statement.
     */
    override fun close() {
        connection.close()
    }
}