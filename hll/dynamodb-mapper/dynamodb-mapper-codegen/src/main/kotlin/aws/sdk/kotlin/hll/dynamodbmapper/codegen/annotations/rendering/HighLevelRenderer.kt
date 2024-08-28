/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param annotatedClasses A list of annotated classes
 */
class HighLevelRenderer(
    private val annotatedClasses: List<KSClassDeclaration>,
    private val logger: KSPLogger,
    private val codegenFactory: CodeGeneratorFactory,
    private val codegenAttributes: Attributes = emptyAttributes(),
) {
    fun render() {
        annotatedClasses.forEach {
            logger.info("Processing annotation on ${it.simpleName}")

            val renderCtx = RenderContext(
                logger,
                codegenFactory,
                "${it.packageName.asString()}.mapper.schemas",
                "dynamodb-mapper-annotation-processor",
                codegenAttributes,
            )

            val annotation = SchemaRenderer(it, renderCtx)
            annotation.render()
        }
    }
}
