package net.sovereign.components.economy

import net.sovereign.core.SovereignCore
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.*
import java.util.logging.Logger

class PlayerPointsCurrencyProviderTest {

    class FakePointsApi {
        var lastTakeAmount: Int = -1
        var lastGiveAmount: Int = -1
        var balance: Int = 100

        fun look(uuid: UUID): Int = balance
        fun take(uuid: UUID, amount: Int): Boolean {
            lastTakeAmount = amount
            if (balance < amount) return false
            balance -= amount
            return true
        }
        fun give(uuid: UUID, amount: Int): Boolean {
            lastGiveAmount = amount
            balance += amount
            return true
        }
    }

    private lateinit var fakeApi: FakePointsApi
    private lateinit var provider: PlayerPointsCurrencyProvider
    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        fakeApi = FakePointsApi()
        val logger = mock<Logger>()
        val plugin = mock<SovereignCore> {
            on { this.logger } doReturn logger
        }

        provider = PlayerPointsCurrencyProvider(plugin)

        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
            .also { it.isAccessible = true }
            .get(null) as sun.misc.Unsafe
        val apiField = PlayerPointsCurrencyProvider::class.java.getDeclaredField("api")
        unsafe.putObject(provider, unsafe.objectFieldOffset(apiField), fakeApi)

        player = mock {
            on { uniqueId } doReturn UUID.randomUUID()
        }
    }

    @Test
    fun `withdraw rounds fractional amount to nearest integer`() {
        provider.withdraw(player, 10.75)
        assertEquals(11, fakeApi.lastTakeAmount)
    }

    @Test
    fun `withdraw rounds down when fraction is below half`() {
        provider.withdraw(player, 10.3)
        assertEquals(10, fakeApi.lastTakeAmount)
    }

    @Test
    fun `withdraw rounds up at half boundary`() {
        provider.withdraw(player, 10.5)
        assertEquals(11, fakeApi.lastTakeAmount)
    }

    @Test
    fun `deposit rounds fractional amount to nearest integer`() {
        provider.deposit(player, 10.75)
        assertEquals(11, fakeApi.lastGiveAmount)
    }

    @Test
    fun `deposit rounds down when fraction is below half`() {
        provider.deposit(player, 10.3)
        assertEquals(10, fakeApi.lastGiveAmount)
    }

    @Test
    fun `hasBalance checks against rounded amount`() {
        fakeApi.balance = 11
        assertTrue(provider.hasBalance(player, 11.3))
        assertFalse(provider.hasBalance(player, 11.5))
    }
}
