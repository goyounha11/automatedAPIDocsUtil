package com.goyounha11.docs

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.epages.restdocs.apispec.ParameterDescriptorWithType
import com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName
import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder
import com.epages.restdocs.apispec.Schema
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.web.servlet.ResultActions
import org.springframework.web.servlet.HandlerMapping
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

object DocsUtil {
    @JvmStatic
    fun createDocs(
        tag: String,
        identifier: String,
        description: String,
        resultActions: ResultActions,
        parameterDescriptor: List<ParameterDescriptorWithType> = emptyList(),
        requestClazz: Class<*>? = null,
        responseClazz: Class<*>? = null
    ): RestDocumentationResultHandler {
        val resourceSnippetParametersBuilder =
            ResourceSnippetParametersBuilder().tags(tag).description(description)

        val request = resultActions.andReturn().request
        val requestNode: JsonNode? =
            request.contentAsString?.let { jacksonObjectMapper().readTree(it) }
        val requestFieldDescriptors = createFieldDescriptors(requestNode, requestClazz?.kotlin)

        val response = resultActions.andReturn().response
        response.characterEncoding = "UTF-8"

        val responseNode: JsonNode? =
            response.contentAsString.let { jacksonObjectMapper().readTree(it) }
        val responseFieldDescriptors = createFieldDescriptors(responseNode, responseClazz?.kotlin)

        resourceSnippetParametersBuilder.apply {
            requestClazz?.let { schema ->
                requestSchema(Schema(schema.simpleName))
            }

            responseClazz?.let { schema ->
                responseSchema(Schema(schema.simpleName))
            }
        }

        val requestParameter = parameterDescriptor.ifEmpty { createParameters(request, ParameterType.QUERY) }
        val requestPathParameter = createParameters(request, ParameterType.PATH)

        resourceSnippetParametersBuilder.requestFields(requestFieldDescriptors)
        resourceSnippetParametersBuilder.queryParameters(requestParameter)
        resourceSnippetParametersBuilder.pathParameters(requestPathParameter)
        resourceSnippetParametersBuilder.responseFields(responseFieldDescriptors)

        return document(
            identifier,
            Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
            Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
            resource(resourceSnippetParametersBuilder.build())
        )
    }

    private fun createFieldDescriptors(
        jsonNode: JsonNode?,
        clazz: KClass<*>?,
        parentPath: String = "",
        isInDataField: Boolean = false
    ): List<FieldDescriptor> {
        if (jsonNode == null) return emptyList()

        val fieldDescriptors = mutableListOf<FieldDescriptor>()
        val iterator = jsonNode.fields()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val key = entry.key
            val value = entry.value
            val path = if (parentPath.isEmpty()) key else "$parentPath.$key"

            val property = clazz?.memberProperties?.find { it.name == key }

            val description =
                property?.let { createFieldDescription(it, value) } ?: createFieldDescription(
                    null,
                    value
                )

            when {
                value.isObject -> {
                    val childIsInDataField = isInDataField || key == "data"
                    var childClass = if (childIsInDataField) clazz else property?.returnType?.jvmErasure

                    if (path.startsWith("data.")) childClass = property?.returnType?.jvmErasure

                    fieldDescriptors.addAll(
                        createFieldDescriptors(
                            value,
                            childClass ?: clazz,
                            path,
                            childIsInDataField
                        )
                    )
                }

                value.isArray -> {
                    when {
                        value.isEmpty -> {
                            fieldDescriptors.add(
                                PayloadDocumentation.fieldWithPath("$path[]")
                                    .description("empty array")
                            )
                        }

                        else -> {
                            val itemClass =
                                property?.returnType?.arguments?.firstOrNull()?.type?.jvmErasure
                            value.forEachIndexed { _, item ->
                                if (item.isObject) {
                                    fieldDescriptors.addAll(
                                        createFieldDescriptors(
                                            item,
                                            itemClass ?: clazz,
                                            "$path[]",
                                            isInDataField
                                        )
                                    )
                                } else {
                                    fieldDescriptors.add(
                                        PayloadDocumentation.fieldWithPath("$path[]")
                                            .description(description)
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    fieldDescriptors.add(
                        PayloadDocumentation.fieldWithPath(path).description(description)
                    )
                }
            }
        }

        return fieldDescriptors
    }

    private fun createFieldDescription(property: KProperty<*>?, value: JsonNode): String {
        val typeDescriptor =
            if (property != null) TypeDescriptor(property.returnType.jvmErasure) else TypeDescriptor(
                value
            )
        val annotations = property?.javaField?.annotations ?: emptyArray()
        val required = isRequired(annotations)

        val enumValues = if (property?.returnType?.jvmErasure?.isSubclassOf(Enum::class) == true) {
            val enumConstants = property.returnType.jvmErasure.java.enumConstants
            enumConstants.joinToString(", ") { enumConstant ->
                if (enumConstant is Enum<*>) {
                    val enumName = enumConstant.name
                    val enumDescription =
                        enumConstant.javaClass.kotlin.primaryConstructor?.parameters?.mapNotNull { param ->
                            try {
                                enumConstant.javaClass.getDeclaredField(param.name!!).let { field ->
                                    field.isAccessible = true
                                    field.get(enumConstant).toString()
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }?.joinToString(", ") ?: ""
                    if (enumDescription.isNotEmpty()) "$enumName($enumDescription)" else enumName
                } else {
                    enumConstant.toString()
                }
            }
        } else {
            ""
        }

        return buildString {
            append(if (required) "[필수] " else "[선택] ")
            append(typeDescriptor)
            if (enumValues.isNotEmpty()) {
                append(" - [가능한 값: $enumValues]")
            }
            append(" - ").append(value.asText())
        }
    }

    private fun createParameters(
        request: MockHttpServletRequest, type: ParameterType
    ): List<ParameterDescriptorWithType> {
        return if (type == ParameterType.QUERY) {
            request.parameterMap.map { (key, value) ->
                if (value.joinToString().isNullOrBlank()) {
                    parameterWithName(key).description(value.joinToString()).optional()
                } else {
                    parameterWithName(key).description(value.joinToString())
                }
            }
        } else {
            val uriVars =
                request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>
            uriVars?.entries?.map { (key, value) ->
                parameterWithName(key.toString()).description("$value")
            }?.toList() ?: listOf()
        }
    }

    private enum class ParameterType {
        PATH, QUERY
    }

    private class TypeDescriptor {
        private val fieldType: JsonFieldType

        constructor(type: KClass<*>?) {
            fieldType = when {
                type == null -> JsonFieldType.VARIES
                Number::class.isSuperclassOf(type) -> JsonFieldType.NUMBER
                String::class.isSuperclassOf(type) -> JsonFieldType.STRING
                Map::class.isSuperclassOf(type) -> JsonFieldType.OBJECT
                Collection::class.isSuperclassOf(type) -> JsonFieldType.ARRAY
                Boolean::class.isSuperclassOf(type) -> JsonFieldType.BOOLEAN
                Enum::class.isSuperclassOf(type) -> JsonFieldType.STRING
                else -> JsonFieldType.OBJECT
            }
        }

        constructor(jsonNode: JsonNode) {
            fieldType = when {
                jsonNode.isNumber -> JsonFieldType.NUMBER
                jsonNode.isTextual -> JsonFieldType.STRING
                jsonNode.isObject -> JsonFieldType.OBJECT
                jsonNode.isArray -> JsonFieldType.ARRAY
                jsonNode.isBoolean -> JsonFieldType.BOOLEAN
                else -> JsonFieldType.VARIES
            }
        }

        override fun toString(): String {
            return when (fieldType) {
                JsonFieldType.NUMBER -> "(숫자)"
                JsonFieldType.STRING -> "(문자열)"
                JsonFieldType.ARRAY -> "(배열)"
                JsonFieldType.BOOLEAN -> "(참/거짓)"
                JsonFieldType.VARIES -> "(시간)"
                else -> "(객체)"
            }
        }
    }

    private fun isRequired(annotations: Array<Annotation>): Boolean {
        return annotations.any { it.annotationClass == NotNull::class || it.annotationClass == NotBlank::class }
    }
}