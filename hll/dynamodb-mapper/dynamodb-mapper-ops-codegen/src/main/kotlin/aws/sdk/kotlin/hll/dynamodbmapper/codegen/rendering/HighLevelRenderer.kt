/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.ItemSourceKind
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Operation
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Type
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.itemSourceKinds

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param ctx The active [RenderContext]
 * @param operations A list of the operations in scope for codegen
 */
class HighLevelRenderer(private val ctx: RenderContext, private val operations: List<Operation>) {
    fun render() {
        operations.forEach(::render)

        val kindTypes = mutableMapOf<ItemSourceKind, Type>()
        ItemSourceKind.entries.forEach { kind ->
            val parentType = kind.parent?.let { kindTypes[it] }
            val operations = this.operations.filter { kind in it.itemSourceKinds }

            val renderer = OperationsTypeRenderer(ctx, kind, parentType, operations)
            renderer.render()
            kindTypes += kind to renderer.interfaceType
        }
    }

    private fun render(operation: Operation) {
        OperationRenderer(ctx, operation).render()
    }
}
