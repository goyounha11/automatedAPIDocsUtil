package com.goyounha11.core.reponse

import java.time.LocalDateTime


data class ApiResult<T>(
    val code: String,
    val message: String,
    val responseAt: LocalDateTime = LocalDateTime.now(),
    val data: T? = null
) {
    companion object {
        fun of(exceptionCode: ErrorCode): ApiResult<*> {
            return of(exceptionCode, null)
        }

        fun <T> of(exceptionCode: ErrorCode, data: T?): ApiResult<T> {
            return ApiResult(
                code = exceptionCode.code,
                message = exceptionCode.message,
                responseAt = LocalDateTime.now(),
                data = data
            )
        }
    }
}
