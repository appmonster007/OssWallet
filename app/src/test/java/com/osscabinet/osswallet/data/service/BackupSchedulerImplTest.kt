package com.osscabinet.ossWallet.data.service

import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupSchedulerImplTest {

    @Test
    fun `autoBackupConfig_defaults_enabledIsFalse`() {
        val config = AutoBackupConfig()
        assertFalse(config.enabled)
    }

    @Test
    fun `autoBackupConfig_defaults_intervalHoursIs24`() {
        val config = AutoBackupConfig()
        assertEquals(24, config.intervalHours)
    }

    @Test
    fun `autoBackupConfig_defaults_maxRetainedIs7`() {
        val config = AutoBackupConfig()
        assertEquals(7, config.maxRetained)
    }

    @Test
    fun `autoBackupConfig_defaults_storageModeIsPrivate`() {
        val config = AutoBackupConfig()
        assertEquals(AutoBackupConfig.StorageMode.PRIVATE, config.storageMode)
    }

    @Test
    fun `autoBackupConfig_defaults_customFolderUriIsNull`() {
        val config = AutoBackupConfig()
        assertEquals(null, config.customFolderUri)
    }

    @Test
    fun `storageMode_values_hasThreeExpectedValues`() {
        val modes = AutoBackupConfig.StorageMode.entries
        assertEquals(3, modes.size)
        assert(AutoBackupConfig.StorageMode.PRIVATE in modes)
        assert(AutoBackupConfig.StorageMode.CUSTOM_FOLDER in modes)
        assert(AutoBackupConfig.StorageMode.BOTH in modes)
    }

    @Test
    fun `notifyMode_values_hasThreeExpectedValues`() {
        val modes = AutoBackupConfig.NotifyMode.entries
        assertEquals(3, modes.size)
        assert(AutoBackupConfig.NotifyMode.ALWAYS in modes)
        assert(AutoBackupConfig.NotifyMode.FAILURE_ONLY in modes)
        assert(AutoBackupConfig.NotifyMode.NEVER in modes)
    }

    @Test
    fun `autoBackupConfig_canBeCreatedWithCustomValues`() {
        val config = AutoBackupConfig(
            enabled = true,
            intervalHours = 12,
            maxRetained = 3,
            storageMode = AutoBackupConfig.StorageMode.BOTH,
            customFolderUri = "content://some/path",
            notifyMode = AutoBackupConfig.NotifyMode.FAILURE_ONLY
        )
        assert(config.enabled)
        assertEquals(12, config.intervalHours)
        assertEquals(3, config.maxRetained)
        assertEquals(AutoBackupConfig.StorageMode.BOTH, config.storageMode)
        assertEquals("content://some/path", config.customFolderUri)
        assertEquals(AutoBackupConfig.NotifyMode.FAILURE_ONLY, config.notifyMode)
    }
}
