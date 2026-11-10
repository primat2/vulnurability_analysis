package ru.rt.vuln_backend.service

import jakarta.persistence.criteria.Subquery
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.rt.vuln_backend.data.dto.TicketRequest
import ru.rt.vuln_backend.data.model.Artifact
import ru.rt.vuln_backend.data.model.EEcosystem
import ru.rt.vuln_backend.data.model.Ticket
import ru.rt.vuln_backend.data.model.Vulnerability
import ru.rt.vuln_backend.repo.ArtifactRepository
import ru.rt.vuln_backend.repo.TicketRepository
import ru.rt.vuln_backend.repo.VulnerabilityRepository

@Service
class TicketService(
    private val tickets: TicketRepository,
    private val artifacts: ArtifactRepository,
    private val vulnerabilities: VulnerabilityRepository
) {

    @Transactional(readOnly = true)
    fun list(
        actual: Boolean?, ecosystem: EEcosystem?, artifactName: String?, cve: String?, pageable: Pageable
    ): Page<Ticket> {
        var spec: Specification<Ticket> = Specification.where(null)

        if (actual != null) spec = spec.and { root, _, cb -> cb.equal(root.get<Boolean?>("actual"), actual) }
        if (ecosystem != null) spec = spec.and { root, _, cb ->
            cb.equal(root.get<Artifact>("artifact").get<EEcosystem>("ecosystem"), ecosystem)
        }
        if (!artifactName.isNullOrBlank()) spec = spec.and { root, _, cb ->
            cb.like(cb.lower(root.get<Artifact>("artifact").get("name")), "%${artifactName.lowercase()}%")
        }
        if (!cve.isNullOrBlank()) spec = spec.and { root, _, cb ->
            val sub = cb.createQuery(String::class.java)
            val tv = sub.from(Ticket::class.java).join<Ticket, Vulnerability>("vulnerabilities")
            sub.select(tv.get("cveId"))
                .where(cb.equal(sub.from(Ticket::class.java).get<Long>("id"), root.get<Long>("id")))
                .where(cb.like(cb.lower(tv.get("cveId")), "%${cve.lowercase()}%"))
            cb.exists(sub as Subquery<*>?)
        }
        return tickets.findAll(spec, pageable)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Ticket = tickets.findById(id).orElseThrow {
        NoSuchElementException("Ticket $id not found")
    }

    @Transactional
    fun create(req: TicketRequest): Ticket {
        val artifact = artifacts.findById(req.artifactId).orElseThrow {
            NoSuchElementException("Artifact ${req.artifactId} not found")
        }
        val ticket = Ticket(
            jiraUrl = req.jiraUrl.trim(),
            artifact = artifact,
            deadline = req.deadline,
            actual = req.actual,
            comment = req.comment?.trim()?.ifBlank { null }
        )
        ticket.vulnerabilities = resolveVulnerabilities(req.cveIds)
        return tickets.save(ticket)
    }

    @Transactional
    fun update(id: Long, req: TicketRequest): Ticket {
        val ticket = get(id)
        val artifact = artifacts.findById(req.artifactId).orElseThrow {
            NoSuchElementException("Artifact ${req.artifactId} not found")
        }
        ticket.jiraUrl = req.jiraUrl.trim()
        ticket.artifact = artifact
        ticket.deadline = req.deadline
        ticket.actual = req.actual
        ticket.comment = req.comment?.trim()?.ifBlank { null }
        ticket.vulnerabilities = resolveVulnerabilities(req.cveIds)
        return ticket
    }

    @Transactional
    fun patchActual(id: Long, actual: Boolean?): Ticket {
        val t = get(id); t.actual = actual; return t
    }

    @Transactional
    fun delete(id: Long) = tickets.deleteById(id)

    private fun resolveVulnerabilities(raw: List<String>): MutableSet<Vulnerability> {
        val cveIds = raw.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val existing = vulnerabilities.findAllByCveIdIn(cveIds).associateBy { it.cveId.uppercase() }
        val result = mutableSetOf<Vulnerability>()
        for (cve in cveIds) {
            val v = existing[cve.uppercase()] ?: vulnerabilities.save(Vulnerability(cveId = cve))
            result += v
        }
        return result
    }
}
