package ru.rt.vuln_backend.data.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import ru.rt.vuln_backend.data.model.EEcosystem

data class ArtifactRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotNull val ecosystem: EEcosystem
)

data class ArtifactResponse(
    val id: Long, val name: String, val ecosystem: EEcosystem
)
