package de.rwth.swc.piggybank.transfergateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TransferGatewayApplication

fun main(args: Array<String>) {
    runApplication<TransferGatewayApplication>(*args)
}