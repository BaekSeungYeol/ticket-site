package me.whitewin.ticketsitescheduler.service

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class UserQueueSchedulerService(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val userQueueService: UserQueueService,
    @Value("\${scheduler.enabled}") private val scheduling: Boolean,
) {

    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    fun scheduleAllowedUser() {
        if(!scheduling) {
            logger.info { "passed Scheduling..." }
            return
        }
        logger.info { "called Scheduling..." }

        reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                .match(USER_QUEUE_WAIT_KEY_FOR_SCAN)
                .count(100)
                .build())
            .map { key -> key.split(":")[2]}
            .flatMap { queue ->
                logger.info { "Queue: $queue"}
                userQueueService.allowUser(queue, MAX_ALLOW_USER_COUNT)
            }
            .doOnNext { allowed -> logger.info { "Tried $MAX_ALLOW_USER_COUNT and allowed $allowed"} }
            .subscribe()

    }

    companion object : KLogging() {
        private const val MAX_ALLOW_USER_COUNT = 3L
        private const val USER_QUEUE_WAIT_KEY_FOR_SCAN = "users:queue:*:wait"
    }
}
