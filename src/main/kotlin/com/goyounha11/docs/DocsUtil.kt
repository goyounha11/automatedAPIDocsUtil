package com.goyounha11.docs

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document
import com.epages.restdocs.apispec.ParameterDescriptorWithType
import com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName
import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.test.web.servlet.ResultActions
import org.springframework.web.servlet.HandlerMapping


object DocsUtil {
    @JvmStatic
    fun createDocs(
        tag: String,
        identifier: String,
        description: String,
        resultActions: ResultActions
    ): RestDocumentationResultHandler {
        val resourceSnippetParametersBuilder = ResourceSnippetParametersBuilder().tags(tag).description(description)

        val request = resultActions.andReturn().request
        val requestNode: JsonNode? =
            request.contentAsString?.let { jacksonObjectMapper().readTree(request.contentAsString) }
        val requestFieldDescriptors = createFieldDescriptors(requestNode)

        val response = resultActions.andReturn().response
        val responseNode: JsonNode? =
            response.contentAsString.let { jacksonObjectMapper().readTree(response.contentAsString) }
        val responseFieldDescriptors = createFieldDescriptors(responseNode)

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

    private fun createFieldDescriptors(jsonNode: JsonNode?, parentPath: String = ""): List<FieldDescriptor> {
        if (jsonNode == null) return emptyList()

        val fieldDescriptors = mutableListOf<FieldDescriptor>()

        val iterator = jsonNode.fields()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val key = entry.key
            val value = entry.value
            val path = if (parentPath.isEmpty()) key else "$parentPath.$key"

            when {
                value.isObject -> {
                    fieldDescriptors.addAll(createFieldDescriptors(value, path))
                }

                value.isArray -> {
                    value.forEachIndexed { index, item ->
                        if (item.isObject) {
                            fieldDescriptors.addAll(createFieldDescriptors(item, "$path.[]."))
                        } else {
                            fieldDescriptors.add(
                                PayloadDocumentation.fieldWithPath("$path.[].")
                                    .description("An element of $path at index $index")
                            )
                        }
                    }
                }

                else -> {
                    fieldDescriptors.add(
                        PayloadDocumentation.fieldWithPath(path).description(value.asText())
                    )
                }
            }
        }

        return fieldDescriptors
    }

    private fun createParameters(
        request: MockHttpServletRequest,
        type: ParameterType
    ): List<ParameterDescriptorWithType> {
        return if (type == ParameterType.QUERY) {
            request.parameterMap.map { (key, value) ->
                parameterWithName(key).description("Value: ${value.joinToString()}")
            }
        } else {
            val uriVars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>

            return uriVars?.entries?.map { (key, value) ->
                parameterWithName(key.toString()).description("$value")
            }?.toList() ?: listOf()
        }
    }
}
private enum class ParameterType {
    PATH,
    QUERY
}
