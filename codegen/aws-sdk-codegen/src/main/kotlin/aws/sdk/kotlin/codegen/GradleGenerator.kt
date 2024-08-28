/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.GradleWriter
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.kotlin.codegen.utils.operations
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait

// TODO - would be nice to allow integrations to define custom settings in the plugin
// e.g. we could then more consistently apply this integration if we could define a property like: `build.isAwsSdk: true`

/**
 * Integration that generates custom gradle build files
 */
class GradleGenerator : KotlinIntegration {

    // Specify to run last, to ensure all other integrations have had a chance to register dependencies.
    override val order: Byte
        get() = 127

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) = generateGradleBuild(ctx, delegator)

    /**
     * Generate a customized version of `build.gradle.kts` for AWS services
     *
     * Services belong to the `aws-sdk-kotlin` build which configures services via `subprojects {}`, thus the generated
     * gradle build needs to be stripped down.
     */
    private fun generateGradleBuild(ctx: CodegenContext, delegator: KotlinDelegator) {
        if (ctx.settings.build.generateDefaultBuildFiles) {
            // plugin settings are configured such that we should let smithy-kotlin generate the build
            // otherwise assume we are responsible
            return
        }

        val writer = GradleWriter()

        writer.write("")
        if (!ctx.settings.pkg.description.isNullOrEmpty()) {
            writer.write("description = #S", ctx.settings.pkg.description)
        }

        writer.write("project.ext.set(#S, #S)", "aws.sdk.id", ctx.settings.sdkId)
        writer.write("")

        val allDependencies = delegator.dependencies.mapNotNull { it.properties["dependency"] as? KotlinDependency }.distinct()

        writer
            .write("")
            .withBlock("kotlin {", "}") {
                withBlock("sourceSets {", "}") {
                    allDependencies
                        .sortedWith(compareBy({ it.config }, { it.artifact }))
                        .groupBy { it.config.sourceSet }
                        .forEach { (sourceSet, dependencies) ->
                            withBlock("$sourceSet {", "}") {
                                withBlock("dependencies {", "}") {
                                    dependencies
                                        .map { it.dependencyNotation() }
                                        .forEach(::write)
                                }
                            }
                        }
                }
                if (ctx.model.operations(ctx.settings.service).any { it.hasTrait<SmokeTestsTrait>() }) {
                    emptyLine()
                    generateSmokeTestConfig(writer, ctx.settings.sdkId, ctx)
                }
            }

        val contents = writer.toString()
        delegator.fileManifest.writeFile("build.gradle.kts", contents)
    }

    private fun generateSmokeTestConfig(writer: GradleWriter, sdkId: String, ctx: CodegenContext) {
        val formattedDenyList = smokeTestDenyList.joinToString(",", "setOf(", ")") { it.dq() }
        writer.withBlock("if (#S !in #L) {", "}", sdkId, formattedDenyList) {
            generateSmokeTestJarTask(writer, ctx)
            emptyLine()
            generateSmokeTestTask(writer)
        }
    }

    /**
     * Generates a gradle task to create smoke test runner JARs
     */
    private fun generateSmokeTestJarTask(writer: GradleWriter, ctx: CodegenContext) {
        writer.withBlock("jvm {", "}") {
            withBlock("compilations {", "}") {
                write("val runtimePath = configurations.getByName(#S).map { if (it.isDirectory) it else zipTree(it) }", "jvmRuntimeClasspath")
                write("val mainPath = getByName(#S).output.classesDirs", "main")
                write("val testPath = getByName(#S).output.classesDirs", "test")
                withBlock("tasks {", "}") {
                    withBlock("register<Jar>(#S) {", "}", "smokeTestJar") {
                        write("description = #S", "Creates smoke tests jar")
                        write("group = #S", "application")
                        write("dependsOn(build)")
                        withBlock("manifest {", "}") {
                            write("attributes[#S] = #S", "Main-Class", "${ctx.settings.pkg.name}.smoketests.SmokeTestsKt")
                        }
                        write("duplicatesStrategy = DuplicatesStrategy.EXCLUDE")
                        write("from(runtimePath, mainPath, testPath)")
                        write("archiveBaseName.set(#S)", "\${project.name}-smoketests")
                    }
                }
            }
        }
    }

    /**
     * Generates a gradle task to run smoke tests
     */
    private fun generateSmokeTestTask(writer: GradleWriter) {
        writer.withBlock("tasks.register<JavaExec>(#S) {", "}", "smokeTest") {
            write("description = #S", "Runs smoke tests jar")
            write("group = #S", "verification")
            write("dependsOn(tasks.getByName(#S))", "smokeTestJar")
            emptyLine()
            write("val sdkVersion: String by project")
            write("val jarFile = file(#S)", "build/libs/\${project.name}-smoketests-\$sdkVersion.jar")
            write("classpath = files(jarFile)")
        }
    }
}
