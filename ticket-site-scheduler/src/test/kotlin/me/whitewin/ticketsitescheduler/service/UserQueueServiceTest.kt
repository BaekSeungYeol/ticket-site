package me.whitewin.ticketsitescheduler.service

import me.whitewin.ticketsitescheduler.EmbeddedRedis
import me.whitewin.ticketsitescheduler.error.ApplicationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier

@SpringBootTest
@Import(EmbeddedRedis::class)
@ActiveProfiles("test")
class UserQueueServiceTest {

    @Autowired
    private lateinit var sut: UserQueueService

    @Autowired
    private lateinit var reactiveRedisTemplate: ReactiveRedisTemplate<String, String>

    @BeforeEach
    fun flush() {
        val redisConnection = reactiveRedisTemplate.connectionFactory.reactiveConnection
        redisConnection.serverCommands().flushAll().subscribe()
    }

    @Test
    fun `대기큐에 유저를 등록한다`() {
        StepVerifier.create(sut.registerWaitQueue("default", 100L))
            .expectNext(1L)
            .verifyComplete()
    }

    @Test
    fun `이미 대기큐에 등록되었다면 다시 등록할 수 없다`() {
        StepVerifier.create(sut.registerWaitQueue("default", 100L))
            .expectNext(1L)
            .verifyComplete()

        StepVerifier.create(sut.registerWaitQueue("default", 100L))
            .expectError(ApplicationException::class.java)
            .verify()

    }


    @Test
    fun `허용된 유저의 수를 불러올 수 있다`() {
        StepVerifier.create(
            sut.registerWaitQueue("default", 3L)
                .then(sut.registerWaitQueue("default", 4L))
                .then(sut.allowUser("default", 2L))
        )
            .expectNext(2L)
            .verifyComplete()
    }

    @Test
    fun `허용된 유저의 수보다 요청이 큰 경우 허용된 유저의 수만 불러온다`() {
        StepVerifier.create(
            sut.registerWaitQueue("default", 3L)
                .then(sut.registerWaitQueue("default", 4L))
                .then(sut.allowUser("default", 3L))
        )
            .expectNext(2L)
            .verifyComplete()
    }

    @Test
    fun `유저를 허용할 수 없다`() {
        StepVerifier.create(sut.isAllowed("default", 3L))
            .expectNext(false)
            .verifyComplete()
    }

    @Test
    fun `유저를 허용할 수 있다`() {
        StepVerifier.create(
            sut.registerWaitQueue("default", 3L)
                .then(sut.allowUser("default", 1L))
                .then(sut.isAllowed("default", 3L))
        )
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `대기순 랭크를 불러올 수 있다`() {
        StepVerifier.create(
            sut.registerWaitQueue("default", 100L)
                .then(sut.getRank("default", 100L))
        )
            .expectNext(1L)
            .verifyComplete()

        StepVerifier.create(
            sut.registerWaitQueue("default", 101L)
                .then(sut.getRank("default", 101L))
        )
            .expectNext(2L)
            .verifyComplete()
    }

    @Test
    fun `대기를 등록하지 않은 경우 랭킹을 불러올 수 없다`() {
        StepVerifier.create(
            sut.getRank("default", 100L)
        )
            .expectNext(-1)
            .verifyComplete()
    }

    @Test
    fun `토큰을 발행한다`() {
        StepVerifier.create(sut.generateToken("default", 100L))
            .expectNext("d333a5d4eb24f3f5cdd767d79b8c01aad3cd73d3537c70dec430455d37afe4b8")
            .verifyComplete()
    }

    @Test
    fun `유효한 토큰을 통해 대기를 통과할 수 있다`() {
        StepVerifier.create(sut.isAllowedByToken("default", 100L, "d333a5d4eb24f3f5cdd767d79b8c01aad3cd73d3537c70dec430455d37afe4b8"))
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `유효한 토큰이 아니면 대기를 통과할 수 없다`() {
        StepVerifier.create(sut.isAllowedByToken("default", 100L, ""))
            .expectNext(false)
            .verifyComplete()
    }

}
