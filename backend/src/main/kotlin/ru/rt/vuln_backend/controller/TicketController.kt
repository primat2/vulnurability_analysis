package ru.rt.vuln_backend.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import ru.rt.vuln_backend.data.dto.ArtifactRef
import ru.rt.vuln_backend.data.dto.TicketPatchActual
import ru.rt.vuln_backend.data.dto.TicketRequest
import ru.rt.vuln_backend.data.dto.TicketResponse
import ru.rt.vuln_backend.data.dto.VulnerabilityRef
import ru.rt.vuln_backend.data.model.EEcosystem
import ru.rt.vuln_backend.data.model.Ticket
import ru.rt.vuln_backend.repo.ArtifactRepository
import ru.rt.vuln_backend.repo.VulnerabilityRepository
import ru.rt.vuln_backend.service.TicketService

@RestController
@RequestMapping("/api/tickets")
class TicketController(
    private val service: TicketService,
    private val artifacts: ArtifactRepository,
    private val vulnerabilities: VulnerabilityRepository
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) actual: Boolean?,
        @RequestParam(required = false) ecosystem: EEcosystem?,
        @RequestParam(required = false) artifact: String?,
        @RequestParam(required = false) cve: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sort: String,
        @RequestParam(defaultValue = "desc") dir: String
    ): Page<TicketResponse> {
        val direction = if (dir.equals("asc", true)) Sort.Direction.ASC else Sort.Direction.DESC
        val order = when (sort) {
            "deadline" -> Sort.Order(direction, "deadline").nullsLast()
            "actual"   -> Sort.Order(direction, "actual").nullsLast()
            else       -> Sort.Order(direction, "createdAt")
        }
        val pageable = PageRequest.of(page, size, Sort.by(order))
        return service.list(actual, ecosystem, artifact, cve, pageable).map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): TicketResponse = service.get(id).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @jakarta.validation.Valid req: TicketRequest): TicketResponse =
        service.create(req).toResponse()

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody @jakarta.validation.Valid req: TicketRequest): TicketResponse =
        service.update(id, req).toResponse()

    @PatchMapping("/{id}/actual")
    fun patchActual(@PathVariable id: Long, @RequestBody body: TicketPatchActual): TicketResponse =
        service.patchActual(id, body.actual).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)

    private fun Ticket.toResponse() = TicketResponse(
        id = id!!, jiraUrl = jiraUrl,
        artifact = ArtifactRef(artifact.id!!, artifact.name, artifact.ecosystem),
        deadline = deadline, actual = actual, comment = comment,
        vulnerabilities = vulnerabilities.map { VulnerabilityRef(it.id!!, it.cveId, it.description) }
            .sortedBy { it.cveId },
        createdAt = createdAt, updatedAt = updatedAt
    )
}
