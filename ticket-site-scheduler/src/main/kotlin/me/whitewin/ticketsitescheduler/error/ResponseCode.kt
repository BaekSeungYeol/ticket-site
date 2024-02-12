package me.whitewin.ticketsitescheduler.error

import org.springframework.http.HttpStatus

enum class ResponseCode(
    private val httpStatus: HttpStatus,
    private val code: String,
    private val reason: String?) {

    ALREADY_REGISTERED(HttpStatus.CONFLICT, "UQ-4009", "이미 대기열에 등록된 유저입니다.");

    fun build(): ApplicationException {
        return ApplicationException(httpStatus, code, reason)
    }
}