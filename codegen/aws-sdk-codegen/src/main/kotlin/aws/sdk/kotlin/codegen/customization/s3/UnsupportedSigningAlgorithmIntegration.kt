/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.aws.traits.auth.SigV4ATrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.ServiceIndex
import software.amazon.smithy.model.shapes.OperationShape

// FIXME: Remove this once sigV4a is supported by default AWS signer
/**
 * Registers an interceptor for sigV4a services to deal with the default signer not supporting sigV4a
 * See: [aws.sdk.kotlin.runtime.http.interceptors.UnsupportedSigningAlgorithmInterceptor]
 */
class UnsupportedSigningAlgorithmIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        ServiceIndex
            .of(model)
            .getAuthSchemes(settings.service)
            .values
            .any { it.javaClass == SigV4ATrait::class.java }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + UnsupportedSigningAlgorithmMiddleware
}

private val UnsupportedSigningAlgorithmMiddleware = object : ProtocolMiddleware {
    override val name: String = "UnsupportedSigningAlgorithmMiddleware"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write(
            "op.interceptors.add(#T())",
            AwsRuntimeTypes.Http.Interceptors.UnsupportedSigningAlgorithmInterceptor,
        )
    }
}
