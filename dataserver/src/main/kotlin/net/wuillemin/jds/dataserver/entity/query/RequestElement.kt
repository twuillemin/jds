package net.wuillemin.jds.dataserver.entity.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * General interface for all elements that could part of a request
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = And::class, name = "And"),
    JsonSubTypes.Type(value = ColumnName::class, name = "ColumnName"),
    JsonSubTypes.Type(value = Contains::class, name = "Contains"),
    JsonSubTypes.Type(value = EndsWith::class, name = "EndsWith"),
    JsonSubTypes.Type(value = Equal::class, name = "Equal"),
    JsonSubTypes.Type(value = GreaterThan::class, name = "GreaterThan"),
    JsonSubTypes.Type(value = GreaterThanOrEqual::class, name = "GreaterThanOrEqual"),
    JsonSubTypes.Type(value = In::class, name = "In"),
    JsonSubTypes.Type(value = LowerThan::class, name = "LowerThan"),
    JsonSubTypes.Type(value = LowerThanOrEqual::class, name = "LowerThanOrEqual"),
    JsonSubTypes.Type(value = NotEqual::class, name = "NotEqual"),
    JsonSubTypes.Type(value = NotIn::class, name = "NotIn"),
    JsonSubTypes.Type(value = Or::class, name = "Or"),
    JsonSubTypes.Type(value = StartsWith::class, name = "StartsWith"),
    JsonSubTypes.Type(value = Value::class, name = "Value"))
interface RequestElement