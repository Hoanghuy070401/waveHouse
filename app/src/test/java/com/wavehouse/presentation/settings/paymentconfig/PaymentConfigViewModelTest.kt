package com.wavehouse.presentation.settings.paymentconfig

import android.content.Context
import app.cash.turbine.test
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.User
import com.wavehouse.domain.model.UserRole
import com.wavehouse.domain.model.Warehouse
import com.wavehouse.domain.model.WarehouseStatus
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.repository.WarehouseRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import android.content.ContentResolver
import android.net.Uri
import com.wavehouse.core.network.ApiResult.Error
import io.mockk.every

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentConfigViewModelTest {

    private val context: Context = mockk(relaxed = true)
    private val getCurrentUserUseCase: GetCurrentUserUseCase = mockk()
    private val warehouseRepository: WarehouseRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)

    private val dispatcher = StandardTestDispatcher()

    private fun buildUser(role: UserRole) = User(
        id = "u1",
        name = "Test",
        email = "a@b.c",
        role = role,
        warehouseId = "w1"
    )

    private fun buildWarehouse(qrUrl: String? = null) = Warehouse(
        id = "w1",
        name = "Kho chính",
        managerId = "u1",
        status = WarehouseStatus.ACTIVE,
        memberCount = 1,
        qrImageUrl = qrUrl
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `non-admin cannot edit QR`() = runTest(dispatcher) {
        coEvery { getCurrentUserUseCase() } returns buildUser(UserRole.STAFF)
        coEvery { authRepository.reloadAndCheckVerified() } returns true
        coEvery { warehouseRepository.getWarehouseById("w1") } returns ApiResult.Success(buildWarehouse())

        val vm = PaymentConfigViewModel(context, getCurrentUserUseCase, warehouseRepository, authRepository)

        vm.uiState.test {
            // skip initial loading
            awaitItem()
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertFalse(loaded.isAdmin)
            assertTrue(loaded.isEmailVerified)
            assertFalse("STAFF must not be able to edit", loaded.canEdit)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `admin without verified email cannot edit`() = runTest(dispatcher) {
        coEvery { getCurrentUserUseCase() } returns buildUser(UserRole.ADMIN)
        coEvery { authRepository.reloadAndCheckVerified() } returns false
        coEvery { warehouseRepository.getWarehouseById("w1") } returns ApiResult.Success(buildWarehouse())

        val vm = PaymentConfigViewModel(context, getCurrentUserUseCase, warehouseRepository, authRepository)

        vm.uiState.test {
            awaitItem() // loading
            val loaded = awaitItem()
            assertTrue(loaded.isAdmin)
            assertFalse(loaded.isEmailVerified)
            assertFalse(loaded.canEdit)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `admin verified can edit and receives warehouse qr url`() = runTest(dispatcher) {
        coEvery { getCurrentUserUseCase() } returns buildUser(UserRole.ADMIN)
        coEvery { authRepository.reloadAndCheckVerified() } returns true
        coEvery { warehouseRepository.getWarehouseById("w1") } returns
            ApiResult.Success(buildWarehouse(qrUrl = "https://cdn.example/qr.png"))

        val vm = PaymentConfigViewModel(context, getCurrentUserUseCase, warehouseRepository, authRepository)

        vm.uiState.test {
            awaitItem() // loading
            val loaded = awaitItem()
            assertTrue(loaded.canEdit)
            assertEquals("https://cdn.example/qr.png", loaded.qrImageUrl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `warehouse load error surfaces in state`() = runTest(dispatcher) {
        coEvery { getCurrentUserUseCase() } returns buildUser(UserRole.ADMIN)
        coEvery { authRepository.reloadAndCheckVerified() } returns true
        coEvery { warehouseRepository.getWarehouseById("w1") } returns ApiResult.Error("net fail")

        val vm = PaymentConfigViewModel(context, getCurrentUserUseCase, warehouseRepository, authRepository)

        vm.uiState.test {
            awaitItem() // loading
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals("net fail", loaded.error)
            assertNull(loaded.qrImageUrl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removeQrImage updates state when success`() = runTest(dispatcher) {
        coEvery { getCurrentUserUseCase() } returns buildUser(UserRole.ADMIN)
        coEvery { authRepository.reloadAndCheckVerified() } returns true
        coEvery { warehouseRepository.getWarehouseById("w1") } returns
            ApiResult.Success(buildWarehouse(qrUrl = "https://cdn.example/qr.png"))
        coEvery { warehouseRepository.updateQrImageUrl("w1", null) } returns ApiResult.Success(Unit)

        val vm = PaymentConfigViewModel(context, getCurrentUserUseCase, warehouseRepository, authRepository)

        // let init finish
        dispatcher.scheduler.advanceUntilIdle()
        vm.removeQrImage()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.qrImageUrl)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `uploading new qr deletes previous image`() = runTest(dispatcher) {
        val contentResolver: ContentResolver = mockk()
        val uri: Uri = mockk()
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))

        coEvery { getCurrentUserUseCase() } returns buildUser(UserRole.ADMIN)
        coEvery { authRepository.reloadAndCheckVerified() } returns true
        coEvery { warehouseRepository.getWarehouseById("w1") } returns
            ApiResult.Success(buildWarehouse(qrUrl = "https://cdn.example/old.png"))
        coEvery { warehouseRepository.uploadQrImage("w1", any()) } returns ApiResult.Success("https://cdn.example/new.png")
        coEvery { warehouseRepository.updateQrImageUrl("w1", "https://cdn.example/new.png") } returns ApiResult.Success(Unit)
        coEvery { warehouseRepository.deleteQrImage("https://cdn.example/old.png") } returns ApiResult.Success(Unit)

        val vm = PaymentConfigViewModel(context, getCurrentUserUseCase, warehouseRepository, authRepository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPickImage(uri)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            warehouseRepository.deleteQrImage("https://cdn.example/old.png")
            warehouseRepository.uploadQrImage("w1", any())
        }
        coVerify { warehouseRepository.updateQrImageUrl("w1", "https://cdn.example/new.png") }
    }

    @Test
    fun `new upload is cleaned up when save fails`() = runTest(dispatcher) {
        val contentResolver: ContentResolver = mockk()
        val uri: Uri = mockk()
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))

        coEvery { getCurrentUserUseCase() } returns buildUser(UserRole.ADMIN)
        coEvery { authRepository.reloadAndCheckVerified() } returns true
        coEvery { warehouseRepository.getWarehouseById("w1") } returns ApiResult.Success(buildWarehouse())
        coEvery { warehouseRepository.uploadQrImage("w1", any()) } returns ApiResult.Success("https://cdn.example/new.png")
        coEvery { warehouseRepository.updateQrImageUrl("w1", "https://cdn.example/new.png") } returns Error("db fail")
        coEvery { warehouseRepository.deleteQrImage("https://cdn.example/new.png") } returns ApiResult.Success(Unit)

        val vm = PaymentConfigViewModel(context, getCurrentUserUseCase, warehouseRepository, authRepository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPickImage(uri)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            warehouseRepository.deleteQrImage("https://cdn.example/new.png")
            warehouseRepository.uploadQrImage("w1", any())
        }
        coVerify { warehouseRepository.updateQrImageUrl("w1", "https://cdn.example/new.png") }
        assertEquals("db fail", vm.uiState.value.error)
    }
}
