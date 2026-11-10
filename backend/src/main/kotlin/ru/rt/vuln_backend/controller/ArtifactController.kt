package ru.rt.vuln_backend.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import ru.rt.vuln_backend.data.dto.ArtifactRequest
import ru.rt.vuln_backend.data.dto.ArtifactResponse
import ru.rt.vuln_backend.data.model.Artifact
import ru.rt.vuln_backend.data.model.EEcosystem
import ru.rt.vuln_backend.repo.ArtifactRepository
import ru.rt.vuln_backend.repo.TicketRepository

@RestController
@RequestMapping("/api/artifacts")
class ArtifactController(
    private val artifacts: ArtifactRepository,
    private val tickets: TicketRepository
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) ecosystem: EEcosystem?
    ): List<ArtifactResponse> {
        val all = if (!q.isNullOrBlank())
            artifacts.findAllByNameContainingIgnoreCase(q)
        else artifacts.findAll()
        return all.asSequence()
            .filter { ecosystem == null || it.ecosystem == ecosystem }
            .map { it.toResponse() }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ArtifactResponse =
        artifacts.findById(id).orElseThrow { NoSuchElementException("Artifact $id not found") }.toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @jakarta.validation.Valid req: ArtifactRequest): ArtifactResponse {
        artifacts.findByNameIgnoreCaseAndEcosystem(req.name, req.ecosystem)?.let {
            throw IllegalArgumentException("Artifact '${req.name}' (${req.ecosystem}) already exists with id=${it.id}")
        }
        val saved = artifacts.save(Artifact(name = req.name.trim(), ecosystem = req.ecosystem))
        return saved.toResponse()
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody @jakarta.validation.Valid req: ArtifactRequest): ArtifactResponse {
        val a = artifacts.findById(id).orElseThrow { NoSuchElementException("Artifact $id not found") }
        a.name = req.name.trim()
        a.ecosystem = req.ecosystem
        return a.toResponse()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        if (tickets.existsByArtifactId(id))
            throw IllegalArgumentException("Artifact $id is referenced by tickets")
        artifacts.deleteById(id)
    }
}

fun Artifact.toResponse() = ArtifactResponse(id!!, name, ecosystem)
