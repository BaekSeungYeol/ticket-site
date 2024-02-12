package me.whitewin.ticketsitescheduler.controller

import me.whitewin.ticketsitescheduler.dto.AllowUserResponse
import me.whitewin.ticketsitescheduler.dto.AllowedUserResponse
import me.whitewin.ticketsitescheduler.dto.RankNumberResponse
import me.whitewin.ticketsitescheduler.dto.RegisterUserResponse
import me.whitewin.ticketsitescheduler.service.UserQueueService
import mu.KLogging
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration

@RestController
@RequestMapping("/api/v1/queue")
class UserQueueController(
    private val userQueueService: UserQueueService
) {


    @PostMapping("")
    fun registerUser(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user_id") userId: Long
    ): Mono<RegisterUserResponse> {
        return userQueueService.registerWaitQueue(queue, userId)
            .map { RegisterUserResponse(it)}
    }

    @PostMapping("/allow")
    fun allowUser(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "count") count: Long
    ): Mono<AllowUserResponse> {
        return userQueueService.allowUser(queue, count)
            .map { AllowUserResponse(count, it)}
    }

    @GetMapping("/allowed")
    fun isAllowedUser(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user_id") userId: Long,
        @RequestParam(name = "token") token: String
    ): Mono<AllowedUserResponse> {
        return userQueueService.isAllowedByToken(queue, userId, token)
            .map { AllowedUserResponse(it) }
    }

    @GetMapping("/rank")
    fun getRankUser(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user_id") userId: Long
    ): Mono<RankNumberResponse> {
        return userQueueService.getRank(queue,userId)
            .map { RankNumberResponse(it) }
    }

    @GetMapping("/touch")
    fun touch(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user_id") userId: Long,
        exchange: ServerWebExchange
    ): Mono<String> {
        return Mono.defer { userQueueService.generateToken(queue, userId) }
            .map { token ->
                addCookieToken(exchange, queue, token)
                logger.info("token = $token")
                token
            }
    }

    private fun addCookieToken(
        exchange: ServerWebExchange,
        queue: String,
        token: String
    ) {
        exchange.response.addCookie(
            ResponseCookie.from("user-queue-$queue-token", token)
                .maxAge(Duration.ofSeconds(300))
                .path("/")
                .build()
        )
    }

    companion object: KLogging()
}
