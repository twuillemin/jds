package net.wuillemin.jds.dataserver.service.query

import net.wuillemin.jds.dataserver.entity.model.Column
import net.wuillemin.jds.dataserver.entity.model.DataType
import net.wuillemin.jds.dataserver.entity.query.Predicate
import net.wuillemin.jds.dataserver.entity.query.RequestElement
import org.springframework.stereotype.Service

/**
 * Create an alias named Context, to the actual data type which is just a map.
 * The context holds the necessary information to ensure that the SQL generation can be processed efficiently
 */
typealias Context = Map<RequestElement, DataType>

/**
 * Service for generating the context of a predicate
 */
@Service
class PredicateContextBuilder {

    /**
     * Create a context from a predicate
     * @param columns The list of columns for which to run the predicate
     * @param predicate The predicate from which to extract the context
     * @return a Context
     */
    fun buildContext(columns: List<Column>, predicate: Predicate): Context {

        val dataTypeByColumn = columns.map { it.name to it.dataType }.toMap()
        val walker = PredicateWalker(predicate)

        // Get the initial context
        val contextInitializer = walker.walk(PredicateContextInitializer(dataTypeByColumn))
        var context = contextInitializer.context

        // While context is enhanced
        do {
            val enhancer = walker.walk(PredicateContextEnhancer(context))
            context = enhancer.context
        }
        while (enhancer.numberOfChanges > 0)


        return context
    }
}