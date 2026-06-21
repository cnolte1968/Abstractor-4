package com.example.domain.usecase

import com.example.domain.repository.SyncRepository

class SyncUserDataUseCase(private val repository: SyncRepository) {
    suspend fun execute() {
        repository.syncAll()
    }

    suspend fun getPendingCount(): Int {
        return repository.getPendingQueueSize()
    }
}
