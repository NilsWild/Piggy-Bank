package de.rwth.swc.piggybank.transfergateway.service

import de.rwth.swc.piggybank.transfergateway.domain.Account
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class AccountServiceTest {

    private lateinit var accountService: AccountService

    @BeforeEach
    fun setUp() {
        accountService = AccountService()
    }

    @Test
    fun `should add account to monitored accounts`() {
        // Given
        val account = Account("BankAccount", "DE123456789")

        // When
        val result = accountService.addMonitoredAccount(account)

        // Then
        result shouldBe true
        accountService.isMonitored(account) shouldBe true
        accountService.getAllMonitoredAccounts().size shouldBe 1
    }

    @Test
    fun `should not add duplicate account to monitored accounts`() {
        // Given
        val account = Account("BankAccount", "DE123456789")
        accountService.addMonitoredAccount(account)

        // When
        val result = accountService.addMonitoredAccount(account)

        // Then
        result shouldBe false
        accountService.getAllMonitoredAccounts().size shouldBe 1
    }

    @Test
    fun `should remove account from monitored accounts`() {
        // Given
        val account = Account("BankAccount", "DE123456789")
        accountService.addMonitoredAccount(account)

        // When
        val result = accountService.removeMonitoredAccount(account)

        // Then
        result shouldBe true
        accountService.isMonitored(account) shouldBe false
        accountService.getAllMonitoredAccounts().size shouldBe 0
    }

    @Test
    fun `should not remove non-existent account from monitored accounts`() {
        // Given
        val account = Account("BankAccount", "DE123456789")

        // When
        val result = accountService.removeMonitoredAccount(account)

        // Then
        result shouldBe false
        accountService.getAllMonitoredAccounts().size shouldBe 0
    }

    @ParameterizedTest
    @MethodSource("accountProvider")
    fun `should correctly check if account is monitored`(account: Account, isMonitored: Boolean) {
        // Given
        if (isMonitored) {
            accountService.addMonitoredAccount(account)
        }

        // When
        val result = accountService.isMonitored(account)

        // Then
        result shouldBe isMonitored
    }

    @Test
    fun `should consider accounts with same type and identifier but different accountId as equal`() {
        // Given
        val accountWithId = Account("BankAccount", "DE123456789", "account-123")
        accountService.addMonitoredAccount(accountWithId)

        // When
        val accountWithoutId = Account("BankAccount", "DE123456789")
        val accountWithDifferentId = Account("BankAccount", "DE123456789", "account-456")

        // Then
        accountService.isMonitored(accountWithoutId) shouldBe true
        accountService.isMonitored(accountWithDifferentId) shouldBe true
    }

    companion object {
        @JvmStatic
        fun accountProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Account("BankAccount", "DE123456789"), true),
                Arguments.of(Account("PayPal", "user@example.com"), false)
            )
        }
    }
}
