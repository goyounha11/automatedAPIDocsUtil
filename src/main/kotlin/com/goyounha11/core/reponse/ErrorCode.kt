package com.goyounha11.core.reponse

enum class ErrorCode(val code: String, val message: String) {
    SUCCESS_NORMAL("S000", "success"),
    ERROR_SYSTEM("E000", "error.system");
}