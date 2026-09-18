package uz.falconmobile.bilim_scan.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemRequestDto;
import uz.falconmobile.bilim_scan.catalog.dto.FanRequestDto;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.service.FanService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/fanlar")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class FanController {
    private final FanService fanService;

    @GetMapping
    public List<Fan> getAll() {
        return fanService.getAll();
    }

    @GetMapping("/{id}")
    public Fan getById(@PathVariable String id) {
        return fanService.getById(id);
    }

    @PostMapping
    public Fan create(@RequestBody FanRequestDto dto) {
        return fanService.create(dto);
    }

    @PutMapping("/{id}")
    public Fan update(@PathVariable String id, @RequestBody FanRequestDto dto) {
        return fanService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> delete(@PathVariable String id) {
        fanService.delete(id);
        return ResponseEntity.ok(true);
    }
}
