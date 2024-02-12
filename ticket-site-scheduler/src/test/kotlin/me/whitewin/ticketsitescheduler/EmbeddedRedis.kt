package me.whitewin.ticketsitescheduler

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.test.context.TestConfiguration
import redis.embedded.RedisServer

@TestConfiguration
class EmbeddedRedis {

    val redisServer = RedisServer(63790)

    @PostConstruct
    fun start() {
        redisServer.start()
    }

    @PreDestroy
    fun stop() {
        redisServer.stop()
    }

}