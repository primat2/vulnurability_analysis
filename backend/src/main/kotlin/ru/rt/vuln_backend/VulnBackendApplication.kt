package ru.rt.vuln_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VulnBackendApplication

fun main(args: Array<String>) {
	runApplication<VulnBackendApplication>(*args)
}
