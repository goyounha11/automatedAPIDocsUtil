package com.goyounha11.core.reponse

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime


data class ApiResult<T>(
    @field:NotBlank
    val code: String,
    @field:NotBlank
    val message: String,
    @field:NotNull
    val responseAt: LocalDateTime = LocalDateTime.now(),
    val data: T? = null
) {
    companion object {
        fun of(exceptionCode: ErrorCode): ApiResult<Unit> {
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
