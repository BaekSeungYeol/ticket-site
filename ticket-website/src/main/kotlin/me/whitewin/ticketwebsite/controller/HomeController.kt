package me.whitewin.ticketwebsite.controller

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import me.whitewin.ticketwebsite.client.RestClientCustomBuilder
import me.whitewin.ticketwebsite.client.getAllowedResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HomeController(
    restClientCustomBuilder: RestClientCustomBuilder
) {

    val restClient = restClientCustomBuilder.createRestClient()

    @GetMapping("/")
    fun home(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user_id") userId: Long,
        request: HttpServletRequest
    ): String {
        val key = "user-queue-$queue-token"
        val cookies = request.cookies ?: arrayOf()
        val token = (cookies.find { it.name == key } ?: Cookie(key, "")).value

        val response  = restClient.getAllowedResponse(queue, userId, token)
        val redirectUrl = "http://127.0.0.1:9081?user_id=$userId"

        // 대기 웹페이지로 리다이렉트
        if(response.body == null || !response.body!!.allowed) {
            return "redirect:http://127.0.0.1:9082/waiting-room?user_id=$userId&redirect_url=$redirectUrl"
        }

        // 허용 상태라면 해당 페이지를 진입
        return "home"
    }
}
