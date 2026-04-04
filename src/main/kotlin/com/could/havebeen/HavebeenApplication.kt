package com.could.havebeen

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@SpringBootApplication
class HavebeenApplication

fun main(args: Array<String>) {
	runApplication<HavebeenApplication>(*args)
}
