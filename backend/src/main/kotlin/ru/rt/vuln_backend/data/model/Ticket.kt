package ru.rt.vuln_backend.data.model

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "ticket")
class Ticket(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "jira_url", nullable = false, unique = true, length = 512)
    var jiraUrl: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false)
    var artifact: Artifact = Artifact(),

    var deadline: LocalDate? = null,

    var actual: Boolean? = null,

    var solved: Boolean = false,

    @Column(columnDefinition = "text")
    var comment: String? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ticket_vulnerability",
        joinColumns = [JoinColumn(name = "ticket_id")],
        inverseJoinColumns = [JoinColumn(name = "vulnerability_id")]
    )
    var vulnerabilities: MutableSet<Vulnerability> = mutableSetOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() { updatedAt = Instant.now() }
}
