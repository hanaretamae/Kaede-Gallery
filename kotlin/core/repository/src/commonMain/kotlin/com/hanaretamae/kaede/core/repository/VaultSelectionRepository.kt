package com.hanaretamae.kaede.core.repository

interface VaultSelectionRepository {
    suspend fun prepare(
        privateDataDirectory: String,
        vaultLocator: String,
    ): RepositoryResult<Unit>

    suspend fun loadSelected(
        privateDataDirectory: String,
    ): RepositoryResult<String?>

    suspend fun saveSelected(
        privateDataDirectory: String,
        vaultLocator: String,
    ): RepositoryResult<String>

    suspend fun clearSelectedData(
        privateDataDirectory: String,
        expectedVaultLocator: String,
    ): RepositoryResult<Unit>
}
