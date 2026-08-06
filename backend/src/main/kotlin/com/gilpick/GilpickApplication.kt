package com.gilpick

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GilpickApplication

fun main(args: Array<String>) {
    runApplication<GilpickApplication>(*args)
}
