package de.rwth.swc.piggybank.transfergateway.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import de.rwth.swc.piggybank.transfergateway.service.TransferService
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal

/**
 * This test specifically tests the scenario where the valuationTimestamp is in an incomplete format
 * (without seconds and timezone), which was causing a deserialization error.
 * 
 * In a real-world scenario, our UI fix would prevent this issue by ensuring that the timestamp
 * is properly formatted before sending it to the backend.
 */
class TimestampFormatTest {

    private lateinit var mockMvc: MockMvc
    private val transferService: TransferService = mockk()
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val transferController = TransferController(transferService)
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Test
    fun `should fail with incomplete timestamp format`() {
        // Given
        // JSON with incomplete timestamp format (without seconds and timezone)
        val incompleteTimestampJson = """
            {
                "sourceAccount": {
                    "type": "BankAccount",
                    "identifier": "DE123456789"
                },
                "targetAccount": {
                    "type": "PayPal",
                    "identifier": "user@example.com"
                },
                "amount": {
                    "value": 100.00,
                    "currencyCode": "EUR"
                },
                "valuationTimestamp": "2025-03-27T23:34",
                "purpose": "Test transfer"
            }
        """.trimIndent()

        // When/Then
        // This should fail with a 400 Bad Request due to the incomplete timestamp format
        mockMvc.perform(
            post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteTimestampJson)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should succeed with complete timestamp format`() {
        // Given
        // JSON with complete timestamp format (with seconds and timezone)
        val completeTimestampJson = """
            {
                "sourceAccount": {
                    "type": "BankAccount",
                    "identifier": "DE123456789"
                },
                "targetAccount": {
                    "type": "PayPal",
                    "identifier": "user@example.com"
                },
                "amount": {
                    "value": 100.00,
                    "currencyCode": "EUR"
                },
                "valuationTimestamp": "2025-03-27T23:34:00Z",
                "purpose": "Test transfer"
            }
        """.trimIndent()

        // Mock the service to return success
        every { transferService.processTransfer(any()) } returns true

        // When/Then
        // This should succeed with a 201 Created
        mockMvc.perform(
            post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeTimestampJson)
        )
            .andExpect(status().isCreated)
    }
}