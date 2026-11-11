package ru.rt.vuln_backend.data.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.rt.vuln_backend.data.model.EEcosystem
import java.time.Instant
import java.time.LocalDate

data class ArtifactRef(
    val id: Long,
    val name: String,
    val ecosystem: EEcosystem
)

data class VulnerabilityRef(
    val id: Long,
    val cveId: String,
    val description: String?
)

data class TicketResponse(
    val id: Long,
    val jiraUrl: String,
    val artifact: ArtifactRef,
    val deadline: LocalDate?,
    val actual: Boolean?,
    val solved: Boolean,
    val comment: String?,
    val vulnerabilities: List<VulnerabilityRef>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class TicketRequest(
    @field:NotBlank @field:Size(max = 512)
    val jiraUrl: String,

    val artifactId: Long,
    val deadline: LocalDate?,
    val actual: Boolean?,

    val comment: String?,

    /** Список CVE-идентификаторов. Отсутствующие в БД будут созданы. */
    val cveIds: List<String> = emptyList()
)

data class TicketPatchActual(
    val actual: Boolean?
)
