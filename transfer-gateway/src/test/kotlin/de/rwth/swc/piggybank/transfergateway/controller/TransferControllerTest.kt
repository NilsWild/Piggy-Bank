package de.rwth.swc.piggybank.transfergateway.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import de.rwth.swc.piggybank.transfergateway.dto.TransferRequest
import de.rwth.swc.piggybank.transfergateway.service.TransferService
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class TransferControllerTest {

    private lateinit var mockMvc: MockMvc
    private val transferService: TransferService = mockk()
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val transferController = TransferController(transferService)
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should handle transfer request and return appropriate status`(serviceSuccess: Boolean) {
        // Given
        val sourceAccount = Account("BankAccount", "DE123456789")
        val targetAccount = Account("PayPal", "user@example.com")
        val amount = Amount(BigDecimal("100.00"), "EUR")
        val transferRequest = TransferRequest(
            sourceAccount = sourceAccount,
            targetAccount = targetAccount,
            amount = amount,
            valuationTimestamp = Instant.now(),
            purpose = "Test transfer"
        )

        val transfer = transferRequest.toDomain()
        every{transferService.processTransfer(any())}.returns(serviceSuccess)

        // When/Then
        val expectedStatus = if (serviceSuccess) HttpStatus.CREATED.value() else HttpStatus.INTERNAL_SERVER_ERROR.value()
        mockMvc.perform(
            post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest))
        )
            .andExpect(status().`is`(expectedStatus))

        verify { transferService.processTransfer(any()) }
    }

    @Test
    fun `should return bad request for invalid transfer request`() {
        // Given
        // Invalid JSON with empty source account type
        val invalidJson = """
            {
                "sourceAccount": {
                    "type": "",
                    "identifier": "DE123456789"
                },
                "targetAccount": {
                    "type": "PayPal",
                    "identifier": "user@example.com"
                },
                "amount": {
                    "value": -100.00,
                    "currencyCode": "EUR"
                },
                "valuationTimestamp": "${Instant.now()}",
                "purpose": "Test transfer"
            }
        """.trimIndent()

        // When/Then
        mockMvc.perform(
            post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { transferService.processTransfer(any()) }
    }
}
