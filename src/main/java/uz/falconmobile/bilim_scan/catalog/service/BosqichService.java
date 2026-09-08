package uz.falconmobile.bilim_scan.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemRequestDto;
import uz.falconmobile.bilim_scan.catalog.model.Bosqich;
import uz.falconmobile.bilim_scan.catalog.repository.BosqichRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BosqichService {
    private final BosqichRepository bosqichRepository;

    public List<Bosqich> getAll() {
        return bosqichRepository.findAll();
    }

    public Bosqich getById(String id) {
        return bosqichRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bosqich topilmadi: " + id));
    }

    public Bosqich create(CatalogItemRequestDto dto) {
        Bosqich bosqich = new Bosqich();
        bosqich.setName(requireName(dto.getName()));
        return bosqichRepository.save(bosqich);
    }

    public Bosqich update(String id, CatalogItemRequestDto dto) {
        Bosqich bosqich = getById(id);
        bosqich.setName(requireName(dto.getName()));
        return bosqichRepository.save(bosqich);
    }

    public void delete(String id) {
        bosqichRepository.deleteById(id);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Name bo'sh bo'lishi mumkin emas");
        }
        return name.trim();
    }
}
