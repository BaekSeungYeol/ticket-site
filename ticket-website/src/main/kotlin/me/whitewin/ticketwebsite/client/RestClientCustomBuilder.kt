package me.whitewin.ticketwebsite.client

import me.whitewin.ticketwebsite.dto.AllowedUserResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RestClientCustomBuilder {

    fun createRestClient(
        baseUrl: String? = null,
        connectTimeout: Long? = 1000,
        readTimeout: Long? = 1000,
        writeTimeout: Long? = 1000
    ): RestClient {

        val builder = RestClient.builder()
            .baseUrl("http://127.0.0.1:9082")

        return builder.build()
    }
}

fun RestClient.getAllowedResponse(
    queue: String,
    userId: Long,
    token: String
): ResponseEntity<AllowedUserResponse> {
    return this.get()
        .uri { builder ->
            builder
                .path("/api/v1/queue/allowed")
                .queryParam("queue", queue)
                .queryParam("user_id", userId)
                .queryParam("token", token)
                .build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(AllowedUserResponse::class.java)
}
