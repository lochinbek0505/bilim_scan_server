package uz.falconmobile.bilim_scan.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemRequestDto;
import uz.falconmobile.bilim_scan.catalog.dto.GuruhRequestDto;
import uz.falconmobile.bilim_scan.catalog.model.Guruh;
import uz.falconmobile.bilim_scan.catalog.service.GuruhService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/guruhlar")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class GuruhController {
    private final GuruhService guruhService;

    @GetMapping
    public List<Guruh> getAll() {
        return guruhService.getAll();
    }

    @GetMapping("/{id}")
    public Guruh getById(@PathVariable String id) {
        return guruhService.getById(id);
    }

    @PostMapping
    public Guruh create(@RequestBody GuruhRequestDto dto) {
        return guruhService.create(dto);
    }

    @PutMapping("/{id}")
    public Guruh update(@PathVariable String id, @RequestBody GuruhRequestDto dto) {
        return guruhService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        guruhService.delete(id);
        return "Guruh o'chirildi";
    }
}
