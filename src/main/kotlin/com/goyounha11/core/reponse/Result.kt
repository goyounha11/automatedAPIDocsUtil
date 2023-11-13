package com.goyounha11.core.reponse

import org.springframework.http.ResponseEntity


class Result {
    companion object {
        fun created(): ResponseEntity<ApiResult<*>> =
            ResponseEntity.status(201).body(ApiResult.of(ErrorCode.SUCCESS_NORMAL))

        fun <T> created(data: T): ResponseEntity<ApiResult<T>> =
            ResponseEntity.status(201).body(ApiResult.of(ErrorCode.SUCCESS_NORMAL, data))

        fun ok(): ResponseEntity<ApiResult<*>> =
            ResponseEntity.status(200).body(ApiResult.of(ErrorCode.SUCCESS_NORMAL))

        fun <T> ok(data: T): ResponseEntity<ApiResult<T>> =
            ResponseEntity.status(200).body(ApiResult.of(ErrorCode.SUCCESS_NORMAL, data))

        fun error(): ResponseEntity<ApiResult<*>> =
            ResponseEntity.status(200).body(ApiResult.of(ErrorCode.ERROR_SYSTEM))
    }
}