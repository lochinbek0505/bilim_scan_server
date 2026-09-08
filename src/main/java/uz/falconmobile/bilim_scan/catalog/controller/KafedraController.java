package uz.falconmobile.bilim_scan.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemRequestDto;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;
import uz.falconmobile.bilim_scan.catalog.service.KafedraService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kafedralar")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class KafedraController {
    private final KafedraService kafedraService;

    @GetMapping
    public List<Kafedra> getAll() {
        return kafedraService.getAll();
    }

    @GetMapping("/{id}")
    public Kafedra getById(@PathVariable String id) {
        return kafedraService.getById(id);
    }

    @PostMapping
    public Kafedra create(@RequestBody CatalogItemRequestDto dto) {
        return kafedraService.create(dto);
    }

    @PutMapping("/{id}")
    public Kafedra update(@PathVariable String id, @RequestBody CatalogItemRequestDto dto) {
        return kafedraService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        kafedraService.delete(id);
        return "Kafedra o'chirildi";
    }
}
