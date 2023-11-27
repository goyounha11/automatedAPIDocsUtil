package com.goyounha11.core.reponse

import org.springframework.http.ResponseEntity


class Result {
    companion object {
        fun created(): ApiResult<*> =ApiResult.of(ErrorCode.SUCCESS_NORMAL)

        fun <T> created(data: T): ApiResult<T> = ApiResult.of(ErrorCode.SUCCESS_NORMAL, data)

        fun ok(): ApiResult<*> = ApiResult.of(ErrorCode.SUCCESS_NORMAL)

        fun <T> ok(data: T): ApiResult<T> = ApiResult.of(ErrorCode.SUCCESS_NORMAL, data)

        fun error(): ApiResult<*> = ApiResult.of(ErrorCode.ERROR_SYSTEM)
    }
}