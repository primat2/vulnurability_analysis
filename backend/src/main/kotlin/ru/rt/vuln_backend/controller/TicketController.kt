package ru.rt.vuln_backend.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import ru.rt.vuln_backend.data.dto.TicketPatchActual
import ru.rt.vuln_backend.data.dto.TicketRequest
import ru.rt.vuln_backend.data.dto.TicketResponse
import ru.rt.vuln_backend.data.model.EEcosystem
import ru.rt.vuln_backend.service.TicketService

@RestController
@RequestMapping("/api/tickets")
class TicketController(
    private val service: TicketService
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
        val sortOrder = when (sort) {
            "deadline" -> Sort.by(
                Sort.Order(direction, "deadline").nullsLast(),
                Sort.Order(Sort.Direction.DESC, "createdAt")
            )
            "actual" -> Sort.by(
                Sort.Order(direction, "actual").nullsLast(),
                Sort.Order(Sort.Direction.DESC, "createdAt")
            )
            else -> Sort.by(Sort.Order(Sort.Direction.DESC, "createdAt"))
        }
        val pageable = PageRequest.of(page, size, sortOrder)
        return service.list(actual, ecosystem, artifact, cve, pageable)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): TicketResponse = service.get(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody @jakarta.validation.Valid req: TicketRequest): TicketResponse =
        service.create(req)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody @jakarta.validation.Valid req: TicketRequest): TicketResponse =
        service.update(id, req)

    @PatchMapping("/{id}/actual")
    fun patchActual(@PathVariable id: Long, @RequestBody body: TicketPatchActual): TicketResponse =
        service.patchActual(id, body.actual)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
