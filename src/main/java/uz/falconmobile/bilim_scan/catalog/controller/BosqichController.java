package uz.falconmobile.bilim_scan.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemRequestDto;
import uz.falconmobile.bilim_scan.catalog.model.Bosqich;
import uz.falconmobile.bilim_scan.catalog.service.BosqichService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bosqichlar")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class BosqichController {
    private final BosqichService bosqichService;

    @GetMapping
    public List<Bosqich> getAll() {
        return bosqichService.getAll();
    }

    @GetMapping("/{id}")
    public Bosqich getById(@PathVariable String id) {
        return bosqichService.getById(id);
    }

    @PostMapping
    public Bosqich create(@RequestBody CatalogItemRequestDto dto) {
        return bosqichService.create(dto);
    }

    @PutMapping("/{id}")
    public Bosqich update(@PathVariable String id, @RequestBody CatalogItemRequestDto dto) {
        return bosqichService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> delete(@PathVariable String id) {
        bosqichService.delete(id);
        return ResponseEntity.ok(true);
    }
}
