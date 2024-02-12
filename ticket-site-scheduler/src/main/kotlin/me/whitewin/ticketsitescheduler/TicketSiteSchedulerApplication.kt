package me.whitewin.ticketsitescheduler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TicketSiteSchedulerApplication

fun main(args: Array<String>) {
    runApplication<TicketSiteSchedulerApplication>(*args)
}
