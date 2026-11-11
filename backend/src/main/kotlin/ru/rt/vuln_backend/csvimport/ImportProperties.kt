package ru.rt.vuln_backend.csvimport

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "import")
class ImportProperties {
    var file: String = ""
}
