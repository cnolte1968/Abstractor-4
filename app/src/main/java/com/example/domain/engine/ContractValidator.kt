package com.example.domain.engine

import com.example.domain.model.DomainSummary

interface ContractValidator {
    fun validate(output: DomainSummary)
}
