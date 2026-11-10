package ru.rt.vuln_backend.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import ru.rt.vuln_backend.data.model.Artifact
import ru.rt.vuln_backend.data.model.EEcosystem
import ru.rt.vuln_backend.data.model.Ticket
import ru.rt.vuln_backend.data.model.Vulnerability

interface ArtifactRepository : JpaRepository<Artifact, Long> {
    fun findByNameIgnoreCaseAndEcosystem(name: String, ecosystem: EEcosystem): Artifact?
    fun findAllByNameContainingIgnoreCase(name: String): List<Artifact>
}

interface VulnerabilityRepository : JpaRepository<Vulnerability, Long> {
    fun findByCveIdIgnoreCase(cveId: String): Vulnerability?
    fun findAllByCveIdContainingIgnoreCase(cveId: String): List<Vulnerability>
    fun findAllByCveIdIn(cveIds: Collection<String>): List<Vulnerability>
}

interface TicketRepository : JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    fun existsByArtifactId(artifactId: Long): Boolean
}
