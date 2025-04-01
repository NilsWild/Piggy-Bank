package de.rwth.swc.piggybank.transferclassifier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TransferClassifierApplication

fun main(args: Array<String>) {
    runApplication<TransferClassifierApplication>(*args)
}