package uz.falconmobile.bilim_scan.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemRequestDto;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;
import uz.falconmobile.bilim_scan.catalog.repository.KafedraRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KafedraService {
    private final KafedraRepository kafedraRepository;

    public List<Kafedra> getAll() {
        return kafedraRepository.findAll();
    }

    public Kafedra getById(String id) {
        return kafedraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kafedra topilmadi: " + id));
    }

    public Kafedra create(CatalogItemRequestDto dto) {
        Kafedra kafedra = new Kafedra();
        kafedra.setName(requireName(dto.getName()));
        return kafedraRepository.save(kafedra);
    }

    public Kafedra update(String id, CatalogItemRequestDto dto) {
        Kafedra kafedra = getById(id);
        kafedra.setName(requireName(dto.getName()));
        return kafedraRepository.save(kafedra);
    }

    public void delete(String id) {
        kafedraRepository.deleteById(id);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Name bo'sh bo'lishi mumkin emas");
        }
        return name.trim();
    }
}
