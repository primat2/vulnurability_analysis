package ru.rt.vuln_backend.data.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "artifact")
class Artifact(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var ecosystem: EEcosystem = EEcosystem.MAVEN,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
