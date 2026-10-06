package com.hanaretamae.kaede.core.settings

import com.hanaretamae.kaede.core.repository.RepositoryResult

interface SettingsRepository {
    suspend fun load(): RepositoryResult<GallerySettings>
    suspend fun save(settings: GallerySettings): RepositoryResult<Unit>
}
