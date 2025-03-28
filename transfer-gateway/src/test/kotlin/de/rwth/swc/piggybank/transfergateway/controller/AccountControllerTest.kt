package de.rwth.swc.piggybank.transfergateway.controller

import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.dto.AccountRequest
import de.rwth.swc.piggybank.transfergateway.service.AccountService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.HashSet

class AccountControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var accountService: AccountService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        accountService = mockk()
        val accountController = AccountController(accountService)
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build()
        objectMapper = ObjectMapper()
    }

    @Test
    fun `should get all monitored accounts`() {
        // Given
        val accounts = setOf(
            Account("BankAccount", "DE123456789"),
            Account("PayPal", "user@example.com")
        )
        every { accountService.getAllMonitoredAccounts() } returns accounts

        // When/Then
        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].type").exists())
            .andExpect(jsonPath("$[0].identifier").exists())
            .andExpect(jsonPath("$[1].type").exists())
            .andExpect(jsonPath("$[1].identifier").exists())

        verify { accountService.getAllMonitoredAccounts() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should add monitored account and return appropriate status`(serviceSuccess: Boolean) {
        // Given
        val accountObj = Account("BankAccount", "DE123456789")
        val accountRequest = AccountRequest(
            account = accountObj
        )

        val domainAccount = accountRequest.toDomain()
        every { accountService.addMonitoredAccount(domainAccount) } returns serviceSuccess

        // When/Then
        val expectedStatus = if (serviceSuccess) HttpStatus.CREATED.value() else HttpStatus.CONFLICT.value()
        mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountRequest))
        )
            .andExpect(status().`is`(expectedStatus))

        verify { accountService.addMonitoredAccount(domainAccount) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should remove monitored account and return appropriate status`(serviceSuccess: Boolean) {
        // Given
        val accountObj = Account("BankAccount", "DE123456789")
        val accountRequest = AccountRequest(
            account = accountObj
        )

        val domainAccount = accountRequest.toDomain()
        every { accountService.removeMonitoredAccount(domainAccount) } returns serviceSuccess

        // When/Then
        val expectedStatus = if (serviceSuccess) HttpStatus.NO_CONTENT.value() else HttpStatus.NOT_FOUND.value()
        mockMvc.perform(
            delete("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountRequest))
        )
            .andExpect(status().`is`(expectedStatus))

        verify { accountService.removeMonitoredAccount(domainAccount) }
    }

    @Test
    fun `should return bad request for invalid account request`() {
        // Given
        // Invalid JSON with empty account type
        val invalidJson = """
            {
                "account": {
                    "type": "",
                    "identifier": "DE123456789"
                }
            }
        """.trimIndent()

        // When/Then
        mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { accountService.addMonitoredAccount(any<Account>()) }
    }
}
