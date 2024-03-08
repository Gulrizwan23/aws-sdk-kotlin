/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

/**
 * Specifies the attribute name for a property in a [DynamoDbItem]-annotated class/interface. If this annotation is not
 * included then the attribute name matches the property name.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbAttribute(val name: String)

/**
 * Specifies that this class/interface describes an item type in a table. All properties of this type will be mapped to
 * attributes unless they are explicitly ignored.
 */
@Target(AnnotationTarget.CLASS)
public annotation class DynamoDbItem

/**
 * Specifies that this property is the primary key for the item. Every top-level [DynamoDbItem] to be used in a table
 * must have exactly one partition key.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbPartitionKey

/**
 * Specifies that this property is the sort key for the item. Every top-level [DynamoDbItem] to be used in a table may
 * have at most one sort key.
 */
@Target(AnnotationTarget.PROPERTY)
public annotation class DynamoDbSortKey
