/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.items.AttributeDescriptor
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.SimpleItemConverter
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.Interceptor
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LReqContext
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.DdbLocalTest
import aws.sdk.kotlin.hll.dynamodbmapper.testutils.ddbItem
import aws.sdk.kotlin.hll.dynamodbmapper.values.IntConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.StringConverter
import kotlinx.coroutines.test.runTest
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import aws.sdk.kotlin.services.dynamodb.model.QueryRequest as LowLevelQueryRequest

// FIXME This whole test class is temporary because Query/Scan don't yet have pagination, object mapping, or conditions

class QueryTest : DdbLocalTest() {
    companion object {
        private const val TABLE_NAME = "query-test"
        private const val TITLE_INDEX_NAME = "emps-by-title"
        private const val NAME_INDEX_NAME = "emps-by-name"

        private data class NamedEmp(
            var companyId: String = "",
            var empId: String = "",
            var name: String = "",
            var title: String = "",
            var tenureYears: Int = 0,
        )

        private val empConverter = SimpleItemConverter(
            ::NamedEmp,
            { this },
            AttributeDescriptor("companyId", NamedEmp::companyId, NamedEmp::companyId::set, StringConverter),
            AttributeDescriptor("empId", NamedEmp::empId, NamedEmp::empId::set, StringConverter),
            AttributeDescriptor("name", NamedEmp::name, NamedEmp::name::set, StringConverter),
            AttributeDescriptor("title", NamedEmp::title, NamedEmp::title::set, StringConverter),
            AttributeDescriptor("tenureYears", NamedEmp::tenureYears, NamedEmp::tenureYears::set, IntConverter),
        )

        private val namedEmpSchema = ItemSchema(empConverter, KeySpec.String("companyId"), KeySpec.String("empId"))
        private val empsByNameSchema = ItemSchema(empConverter, KeySpec.String("companyId"), KeySpec.String("name"))

        private data class TitleEmp(
            var title: String = "",
            var name: String = "",
            var companyId: String = "",
            var empId: String = "",
        )

        private val titleConverter = SimpleItemConverter(
            ::TitleEmp,
            { this },
            AttributeDescriptor("title", TitleEmp::title, TitleEmp::title::set, StringConverter),
            AttributeDescriptor("name", TitleEmp::name, TitleEmp::name::set, StringConverter),
            AttributeDescriptor("empId", TitleEmp::empId, TitleEmp::empId::set, StringConverter),
            AttributeDescriptor("companyId", TitleEmp::companyId, TitleEmp::companyId::set, StringConverter),
        )

        private val titleSchema = ItemSchema(titleConverter, KeySpec.String("title"), KeySpec.String("name"))
    }

    @BeforeAll
    fun setUp() = runTest {
        createTable(
            name = TABLE_NAME,
            schema = namedEmpSchema,
            gsis = mapOf(TITLE_INDEX_NAME to titleSchema),
            lsis = mapOf(NAME_INDEX_NAME to empsByNameSchema),
            items = listOf(
                mapOf(
                    "companyId" to "foo-corp",
                    "empId" to "AB0123",
                    "name" to "Alice Birch",
                    "title" to "SDE",
                    "tenureYears" to 5,
                ),
                mapOf(
                    "companyId" to "foo-corp",
                    "empId" to "AB0126",
                    "name" to "Adriana Beech",
                    "title" to "Manager",
                    "tenureYears" to 7,
                ),
                mapOf(
                    "companyId" to "foo-corp",
                    "empId" to "EF0124",
                    "name" to "Eddie Fraser",
                    "title" to "SDE",
                    "tenureYears" to 3,
                ),
                mapOf(
                    "companyId" to "bar-corp",
                    "empId" to "157X",
                    "name" to "Charlie Douglas",
                    "title" to "Manager",
                    "tenureYears" to 4,
                ),
            ),
        )
    }

    @Test
    fun testQueryTable() = runTest {
        val mapper = mapper { interceptors += ExpressionAttributeInterceptor("c" to "foo-corp") }
        val table = mapper.getTable(TABLE_NAME, namedEmpSchema)

        val result = table.query(
            QueryRequest(
                null,
                null,
                null,
                """companyId = :c""", // FIXME ugly hack until conditions are implemented
                null,
                null,
                null,
                null,
            ),
        )

        val items = assertNotNull(result.items)

        val expected = listOf( // FIXME query/scan don't support object mapping yet
            mapOf(
                "companyId" to "foo-corp",
                "empId" to "AB0123",
                "name" to "Alice Birch",
                "title" to "SDE",
                "tenureYears" to 5,
            ),
            mapOf(
                "companyId" to "foo-corp",
                "empId" to "AB0126",
                "name" to "Adriana Beech",
                "title" to "Manager",
                "tenureYears" to 7,
            ),
            mapOf(
                "companyId" to "foo-corp",
                "empId" to "EF0124",
                "name" to "Eddie Fraser",
                "title" to "SDE",
                "tenureYears" to 3,
            ),
        ).map(::ddbItem)

        assertContentEquals(expected, items)
    }

    @Test
    fun testQueryGsi() = runTest {
        val mapper = mapper { interceptors += ExpressionAttributeInterceptor("t" to "Manager") }
        val table = mapper.getTable(TABLE_NAME, namedEmpSchema)
        val index = table.getIndex(TITLE_INDEX_NAME, titleSchema)

        val result = index.query(
            QueryRequest(
                null,
                null,
                null,
                """title = :t""", // FIXME ugly hack until conditions are implemented
                null,
                null,
                null,
                null,
            ),
        )

        val items = assertNotNull(result.items)

        val expected = listOf( // FIXME query/scan don't support object mapping yet
            mapOf(
                "companyId" to "foo-corp",
                "empId" to "AB0126",
                "name" to "Adriana Beech",
                "title" to "Manager",
            ),
            mapOf(
                "companyId" to "bar-corp",
                "empId" to "157X",
                "name" to "Charlie Douglas",
                "title" to "Manager",
            ),
        ).map(::ddbItem)

        assertContentEquals(expected, items)
    }

    @Test
    fun testQueryLsi() = runTest {
        val mapper = mapper { interceptors += ExpressionAttributeInterceptor("c" to "foo-corp") }
        val table = mapper.getTable(TABLE_NAME, namedEmpSchema)
        val index = table.getIndex(NAME_INDEX_NAME, empsByNameSchema)

        val result = index.query(
            QueryRequest(
                null,
                null,
                null,
                """companyId = :c""", // FIXME ugly hack until conditions are implemented
                null,
                null,
                null,
                null,
            ),
        )

        val items = assertNotNull(result.items)

        val expected = listOf( // FIXME query/scan don't support object mapping yet
            mapOf(
                "companyId" to "foo-corp",
                "empId" to "AB0126",
                "name" to "Adriana Beech",
                "title" to "Manager",
                "tenureYears" to 7,
            ),
            mapOf(
                "companyId" to "foo-corp",
                "empId" to "AB0123",
                "name" to "Alice Birch",
                "title" to "SDE",
                "tenureYears" to 5,
            ),
            mapOf(
                "companyId" to "foo-corp",
                "empId" to "EF0124",
                "name" to "Eddie Fraser",
                "title" to "SDE",
                "tenureYears" to 3,
            ),
        ).map(::ddbItem)

        assertContentEquals(expected, items)
    }
}

// FIXME ugly hack until conditions are implemented
private class ExpressionAttributeInterceptor(
    vararg attributeValues: Pair<String, Any>,
) : Interceptor<Any, Any, LowLevelQueryRequest, Any, Any> {

    val attributeValues = ddbItem(*attributeValues).mapKeys { (k, _) -> ":$k" }

    override fun modifyBeforeInvocation(ctx: LReqContext<Any, Any, LowLevelQueryRequest>): LowLevelQueryRequest =
        ctx.lowLevelRequest.copy { expressionAttributeValues = attributeValues }
}
