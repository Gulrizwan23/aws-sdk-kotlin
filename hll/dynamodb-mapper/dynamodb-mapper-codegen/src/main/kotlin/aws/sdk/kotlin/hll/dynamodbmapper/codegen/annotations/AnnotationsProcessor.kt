/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering.HighLevelRenderer
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.collections.mutableAttributes
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

private val annotationName = DynamoDbItem::class.qualifiedName!!

class AnnotationsProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var invoked = false
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val codeGeneratorFactory = CodeGeneratorFactory(codeGenerator, logger)
    private val codegenAttributes = mutableAttributes()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return listOf()
        }
        invoked = true

        logger.info("Searching for symbols annotated with $annotationName")
        val annotated = resolver.getSymbolsWithAnnotation(annotationName)
        val invalid = annotated.filterNot { it.validate() }.toList()
        logger.info("Found invalid classes $invalid")

        codegenAttributes[CodegenAttributes.AlwaysGenerateBuilders] = environment.options[CodegenAttributes.AlwaysGenerateBuilders.name].equals("ALWAYS", ignoreCase = true)

        val annotatedClasses = annotated
            .toList()
            .also { logger.info("Found annotated classes: $it") }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }

        HighLevelRenderer(annotatedClasses, logger, codeGeneratorFactory, codegenAttributes).render()

        return invalid
    }
}

object CodegenAttributes {
    val AlwaysGenerateBuilders: AttributeKey<Boolean> = AttributeKey("AlwaysGenerateBuilders")
}
