package me.whitewin.ticketsitescheduler.service

import me.whitewin.ticketsitescheduler.error.ResponseCode
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant


@Service
class UserQueueService(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String,String>
){
    fun registerWaitQueue(
        queue: String,
        userId: Long): Mono<Long> {
        val unixTime = Instant.now().epochSecond
        return reactiveRedisTemplate
            .opsForZSet()
            .add(getWaitKey(queue), userId.toString(), unixTime.toDouble())
            .filter { it } // filter when true
            .switchIfEmpty(Mono.error(ResponseCode.ALREADY_REGISTERED.build()))
            .flatMap { reactiveRedisTemplate.opsForZSet().rank(getWaitKey(queue), userId.toString()) }
            .map { rank -> plusRankOneIfNotEmpty(rank) }
    }

    // 진입이 가능한 상태인지 조회
    fun isAllowed(queue: String, userId: Long): Mono<Boolean> {
        return reactiveRedisTemplate.opsForZSet().rank(getProceedKey(queue), userId.toString())
            .defaultIfEmpty(-1)
            .map { it >= 0 }
    }

    // 진입이 가능한 상태인지 조회
    fun isAllowedByToken(queue: String, userId: Long, token: String): Mono<Boolean> {
        return this.generateToken(queue, userId)
            .log()
            .filter { gen -> gen.equals(token)}
            .map { true }
            .defaultIfEmpty(false)
    }


    // 진입을 허용
    fun allowUser(queue: String, count: Long): Mono<Long> {
        return reactiveRedisTemplate.opsForZSet()
            .popMin(getWaitKey(queue), count)
            .flatMap { user ->
                val userValue = user.value ?: return@flatMap Flux.error(IllegalArgumentException())
                reactiveRedisTemplate.opsForZSet().add(getProceedKey(queue), userValue, Instant.now().epochSecond.toDouble())
            }
            .count()
    }

    fun getRank(
        queue: String,
        userId: Long
    ): Mono<Long> {
        return reactiveRedisTemplate.opsForZSet().rank(getWaitKey(queue), userId.toString())
            .defaultIfEmpty(-1)
            .map { rank -> plusRankOneIfNotEmpty(rank) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun generateToken(queue: String, userId: Long): Mono<String> {
        return Mono.fromCallable {
            val digest = MessageDigest.getInstance("SHA-256")
            val input = "user-queue-$queue-$userId"
            val encodedHash = digest.digest(input.byteInputStream(charset = StandardCharsets.UTF_8).readAllBytes())
            encodedHash.toList()
                .map { it.toHexString()}
                .reduce { a, b  ->  a + b }
        }
    }

    private fun plusRankOneIfNotEmpty(rank: Long): Long = if (rank >= 0) rank + 1 else rank

    companion object {
        fun getWaitKey(queue: String) = "users:queue:${queue}:wait"
        fun getProceedKey(queue: String) = "users:queue:${queue}:proceed"
    }
}
