package ru.rt.vuln_backend.csvimport

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.rt.vuln_backend.data.model.Artifact
import ru.rt.vuln_backend.data.model.EEcosystem
import ru.rt.vuln_backend.data.model.Ticket
import ru.rt.vuln_backend.data.model.Vulnerability
import ru.rt.vuln_backend.repo.ArtifactRepository
import ru.rt.vuln_backend.repo.TicketRepository
import ru.rt.vuln_backend.repo.VulnerabilityRepository
import java.io.FileReader
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
@Profile("import")
class CsvImporter(
    private val tickets: TicketRepository,
    private val artifacts: ArtifactRepository,
    private val vulnerabilities: VulnerabilityRepository,
    private val props: ImportProperties
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    // dd.MM.yyyy, dd.MM (год = текущий), с точкой/без в конце
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd.MM")
    )

    @Transactional
    override fun run(vararg args: String) {
        // TODO: костыль, путь захардкожен
        val path = Path.of("D:\\data.csv")
        log.info("Импорт из {}", path.toAbsolutePath())

        var imported = 0; var skipped = 0; var cveCreated = 0; var artCreated = 0
        val seenJiraPerArtifact = mutableSetOf<Pair<String, Long>>() // (jiraUrl, artifactId) — защита от повторного импорта

        FileReader(path.toFile()).use { reader ->
            CSVParser.parse(
                reader,
                CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build()
            ).use { parser ->
                for (record in parser) {
                    val jiraUrl = record.get("Ссылка на задачу").trim()
                    if (jiraUrl.isBlank()) { skipped++; continue }

                    val rawArtifact = record.get("Артефакт").trim()
                    val ecosystem = parseEcosystem(record.get("Формат").trim())
                    val artifactNames = splitArtifacts(rawArtifact)
                    if (artifactNames.isEmpty()) { skipped++; continue }

                    val deadline = parseDeadline(record.get("Дедлайн").trim())
                    val actual = parseActual(record.get("Актуальна уязвимость для нас или нет 0/1").trim())
                    val comment = record.get("Комментарий").trim().ifBlank { null }
                    val cveIds = parseCves(record.get("CVE").trim())

                    for (artName in artifactNames) {
                        val artifact = artifacts.findByNameIgnoreCaseAndEcosystem(artName, ecosystem)
                            ?: artifacts.save(Artifact(name = artName, ecosystem = ecosystem)).also { artCreated++ }

                        if (!seenJiraPerArtifact.add(jiraUrl to artifact.id!!)) {
                            log.debug("Пропуск дубля {} / {}", jiraUrl, artName); continue
                        }

                        val ticket = Ticket(
                            jiraUrl = jiraUrl,
                            artifact = artifact,
                            deadline = deadline,
                            actual = actual,
                            comment = comment,
                            solved = deadline == null
                        )
                        ticket.vulnerabilities = resolveVulnerabilities(cveIds) { cveCreated++ }
                        tickets.save(ticket)
                        imported++
                    }
                }
            }
        }
        log.info("Готово: тикетов={}, пропущено строк={}, создано артефактов={}, создано CVE={}",
            imported, skipped, artCreated, cveCreated)
    }

    private fun resolveVulnerabilities(cveIds: List<String>, onCreated: () -> Unit): MutableSet<Vulnerability> {
        if (cveIds.isEmpty()) return mutableSetOf()
        val existing = vulnerabilities.findAllByCveIdIn(cveIds).associateBy { it.cveId.uppercase() }
        val result = mutableSetOf<Vulnerability>()
        for (cve in cveIds) {
            val v = existing[cve.uppercase()] ?: vulnerabilities.save(Vulnerability(cveId = cve)).also { onCreated() }
            result += v
        }
        return result
    }

    private fun parseEcosystem(raw: String): EEcosystem = when (raw.lowercase()) {
        "npm" -> EEcosystem.NPM
        "maven" -> EEcosystem.MAVEN
        else -> {
            log.warn("Неизвестная экосистема '{}', считаем MAVEN", raw); EEcosystem.MAVEN
        }
    }

    /** "helpers,runtime" -> [helpers, runtime]; "tomcat-coyote / tomcat-catalina" -> [tomcat-coyote, tomcat-catalina];
     *  "brace-expansion:5.0.5" -> [brace-expansion]; "tomcat" -> [tomcat] */
    private fun splitArtifacts(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',', '/')
            .map { it.trim().substringBefore(':').trim() } // отрезаем :version
            .filter { it.isNotEmpty() }
            .distinct()
    }

    /** CVE через запятую и/или пробел; нормализуем юникод-дефисы; убираем мусор. */
    private fun parseCves(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',', ' ', '\n', '\t')
            .map { it.trim().replace(Regex("[\\u2010-\\u2015]"), "-") }
            .filter { it.isNotEmpty() }
            .filter { it.startsWith("CVE-", true) || it.startsWith("GHSA-", true) }
            .map { it.uppercase() }
            .distinct()
    }

    private fun parseActual(raw: String): Boolean? = when (raw) {
        "1" -> true
        "0" -> false
        else -> null   // "-", "", "—" и прочее
    }

    private fun parseDeadline(raw: String): LocalDate? {
        if (raw.isBlank() || raw == "-") return null
        val cleaned = raw.trim().removeSuffix(".")
        for (fmt in dateFormats) {
            try {
                val parsed = LocalDate.parse(cleaned, fmt)
                // если формат без года — LocalDate.parse не сработает, см. ниже
                return parsed
            } catch (_: Exception) { /* try next */ }
        }
        // dd.MM (без года)
        val m = Regex("""^(\d{1,2})\.(\d{1,2})$""").find(cleaned)
        if (m != null) {
            val day = m.groupValues[1].toInt(); val month = m.groupValues[2].toInt()
            return runCatching { LocalDate.of(LocalDate.now().year, month, day) }.getOrNull()
        }
        log.warn("Не удалось распарсить дедлайн '{}'", raw)
        return null
    }
}
