package com.example.domain.usecase

import com.example.domain.repository.SyncRepository

class SyncUserDataUseCase(
    private val syncRepository: SyncRepository
) {
    suspend fun execute() {
        syncRepository.syncAll()
    }

    suspend fun getPendingCount(): Int {
        return 0
    }
}
