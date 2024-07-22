package com.goyounha11.docs

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.epages.restdocs.apispec.ParameterDescriptorWithType
import com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName
import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.goyounha11.core.reponse.ApiResult
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
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

object DocsUtil {
    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @JvmStatic
    fun createDocs(
        tag: String,
        identifier: String,
        description: String,
        resultActions: ResultActions,
        requestClazz: Class<*>? = null,
        responseClazz: Class<*>? = null
    ): RestDocumentationResultHandler {
        val resourceSnippetParametersBuilder = ResourceSnippetParametersBuilder().tags(tag).description(description)

        val request = resultActions.andReturn().request
        val requestNode: JsonNode? = request.contentAsString?.takeIf { it.isNotBlank() }?.let { objectMapper.readTree(it) }
        val requestObject = requestNode?.let { objectMapper.treeToValue(it, requestClazz) }
        val requestFieldDescriptors = createFieldDescriptors(requestObject, wrappedClazz = requestClazz?.kotlin)

        val response = resultActions.andReturn().response
        response.characterEncoding = "UTF-8"

        val responseNode: JsonNode? = response.contentAsString.takeIf { it.isNotBlank() }?.let { objectMapper.readTree(it) }
        val apiResultType = object : TypeReference<ApiResult<Any>>() {}
        val responseObject: ApiResult<Any>? = responseNode?.let { objectMapper.readValue(it.toString(), apiResultType) }
        val data = responseObject?.data

        val responseFieldDescriptors = createFieldDescriptors(responseObject, ApiResult::class, responseClazz?.kotlin)

        val requestParameter = createParameters(request, ParameterType.QUERY)
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
        obj: Any?, wrapperClazz: KClass<*>? = null, wrappedClazz: KClass<*>?, parentPath: String = ""
    ): List<FieldDescriptor> {
        if (obj == null) return emptyList()

        val fieldDescriptors = mutableListOf<FieldDescriptor>()
        val properties = wrapperClazz?.memberProperties ?: return emptyList()

        properties.forEach { property ->
            val key = property.name
            val value = if (obj is Map<*, *>) {
                obj[key]
            } else {
                property.getter.call(obj)
            }
            val path = if (parentPath.isEmpty()) key else "$parentPath.$key"

            val childClazz = if (key == "data" && wrappedClazz != null) {
                wrappedClazz
            } else {
                property.returnType.jvmErasure
            }

            val description = createFieldDescription(property, value)

            when (value) {
                is Map<*, *> -> {
                    fieldDescriptors.addAll(createFieldDescriptors(value, childClazz, wrappedClazz, path))
                }

                is Collection<*> -> {
                    fieldDescriptors.add(PayloadDocumentation.fieldWithPath(path).type(JsonFieldType.ARRAY).description(description))
                    if (value.isNotEmpty()) {
                        value.forEachIndexed { index, item ->
                            if (item is Map<*, *>) {
                                fieldDescriptors.addAll(createFieldDescriptors(item, childClazz, wrappedClazz, "$path[$index]"))
                            } else {
                                fieldDescriptors.add(PayloadDocumentation.fieldWithPath("$path[$index]").description(description))
                            }
                        }
                    }
                }

                else -> {
                    fieldDescriptors.add(PayloadDocumentation.fieldWithPath(path).description(description))
                }
            }
        }

        return fieldDescriptors
    }

    private fun createFieldDescription(property: KProperty<*>, value: Any?): String {
        val typeDescriptor = TypeDescriptor(property.returnType.jvmErasure)
        val annotations = property.javaField?.annotations ?: emptyArray()
        val required = isRequired(annotations)

        val enumValues = if (property.returnType.jvmErasure.isSubclassOf(Enum::class)) {
            property.returnType.jvmErasure.java.enumConstants.joinToString(", ") { it.toString() }
        } else {
            ""
        }

        return buildString {
            append(if (required) "[필수] " else "[선택] ")
            append(typeDescriptor)
            if (enumValues.isNotEmpty()) {
                append(" - [가능한 값: $enumValues]")
            }
            append(" - ").append(value?.toString() ?: "")
        }
    }

    private fun createParameters(
        request: MockHttpServletRequest, type: ParameterType
    ): List<ParameterDescriptorWithType> {
        return if (type == ParameterType.QUERY) {
            request.parameterMap.map { (key, value) ->
                parameterWithName(key).description(value.joinToString())
            }
        } else {
            val uriVars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>
            uriVars?.entries?.map { (key, value) ->
                parameterWithName(key.toString()).description("$value")
            }?.toList() ?: listOf()
        }
    }

    private enum class ParameterType {
        PATH, QUERY
    }

    private class TypeDescriptor(type: KClass<*>?) {
        private val fieldType: JsonFieldType = when {
            type == null -> JsonFieldType.VARIES
            Number::class.isSuperclassOf(type) -> JsonFieldType.NUMBER
            String::class.isSuperclassOf(type) -> JsonFieldType.STRING
            Map::class.isSuperclassOf(type) -> JsonFieldType.OBJECT
            Collection::class.isSuperclassOf(type) -> JsonFieldType.ARRAY
            Boolean::class.isSuperclassOf(type) -> JsonFieldType.BOOLEAN
            Enum::class.isSuperclassOf(type) -> JsonFieldType.STRING
            else -> JsonFieldType.OBJECT
        }

        constructor(jsonNode: JsonNode) : this(
            when {
                jsonNode.isNumber -> Number::class
                jsonNode.isTextual -> String::class
                jsonNode.isObject -> Map::class
                jsonNode.isArray -> Collection::class
                jsonNode.isBoolean -> Boolean::class
                else -> null
            }
        )

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