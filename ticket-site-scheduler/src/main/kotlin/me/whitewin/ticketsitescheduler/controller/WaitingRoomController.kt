package me.whitewin.ticketsitescheduler.controller

import me.whitewin.ticketsitescheduler.service.UserQueueService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.result.view.Rendering
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Controller
class WaitingRoomController(
    private val userQueueService: UserQueueService
) {

    @GetMapping("/waiting-room")
    fun waitingRoomPage(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user_id") userId: Long,
        @RequestParam(name = "redirect_url") redirectUrl: String,
        exchange: ServerWebExchange
    ): Mono<Rendering> {
        val key = "user-queue-$queue-token"
        val cookieValue = exchange.request.cookies.getFirst(key)
        val token = cookieValue?.value ?: ""

        // 대기 등록
        // 웹 페이지에 필요한 데이터를 전달
        val register = userQueueService.registerWaitQueue(queue, userId)
            .onErrorResume { userQueueService.getRank(queue, userId) }
            .map { rank -> makeRendering(rank, userId, queue) }

        return userQueueService.isAllowedByToken(queue,userId, token)
            .filter { it }
            .flatMap { Mono.just(Rendering.redirectTo(redirectUrl).build()) }
            .switchIfEmpty(register)

    }

    private fun makeRendering(
        rank: Long,
        userId: Long,
        queue: String
    ) = Rendering.view("waiting-room.html")
        .modelAttribute("number", rank)
        .modelAttribute("userId", userId)
        .modelAttribute("queue", queue)
        .build()
}
