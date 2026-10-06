package com.hanaretamae.kaede.core.rust

import com.hanaretamae.kaede.core.repository.RepositoryError
import com.hanaretamae.kaede.core.repository.RepositoryResult
import com.hanaretamae.kaede.core.repository.VaultSelectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RustVaultSelectionRepository : VaultSelectionRepository {
    override suspend fun prepare(
        privateDataDirectory: String,
        vaultLocator: String,
    ): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        try {
            preparePrivateData(privateDataDirectory, vaultLocator)
            RepositoryResult.Success(Unit)
        } catch (error: GalleryException) {
            RepositoryResult.Failure(error.toRepositoryError())
        }
    }

    override suspend fun loadSelected(
        privateDataDirectory: String,
    ): RepositoryResult<String?> = withContext(Dispatchers.IO) {
        try {
            RepositoryResult.Success(loadSelectedVaultPath(privateDataDirectory))
        } catch (error: GalleryException) {
            RepositoryResult.Failure(error.toRepositoryError())
        }
    }

    override suspend fun saveSelected(
        privateDataDirectory: String,
        vaultLocator: String,
    ): RepositoryResult<String> = withContext(Dispatchers.IO) {
        try {
            RepositoryResult.Success(saveSelectedVaultPath(privateDataDirectory, vaultLocator))
        } catch (error: GalleryException) {
            RepositoryResult.Failure(error.toRepositoryError())
        }
    }

    override suspend fun clearSelectedData(
        privateDataDirectory: String,
        expectedVaultLocator: String,
    ): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        try {
            forgetSelectedVaultData(privateDataDirectory, expectedVaultLocator)
            RepositoryResult.Success(Unit)
        } catch (error: GalleryException) {
            RepositoryResult.Failure(error.toRepositoryError())
        }
    }
}

private fun GalleryException.toRepositoryError(): RepositoryError = when (this) {
    is GalleryException.VaultUnavailable -> RepositoryError.VAULT_UNAVAILABLE
    is GalleryException.InvalidVault -> RepositoryError.INVALID_VAULT
    is GalleryException.StorageUnavailable -> RepositoryError.STORAGE_UNAVAILABLE
    is GalleryException.IndexUnavailable -> RepositoryError.INDEX_UNAVAILABLE
    is GalleryException.InvalidRequest -> RepositoryError.INVALID_REQUEST
    is GalleryException.OperationFailed -> RepositoryError.OPERATION_FAILED
}
