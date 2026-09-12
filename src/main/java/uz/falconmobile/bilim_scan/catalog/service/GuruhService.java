package uz.falconmobile.bilim_scan.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemRequestDto;
import uz.falconmobile.bilim_scan.catalog.dto.GuruhRequestDto;
import uz.falconmobile.bilim_scan.catalog.model.Guruh;
import uz.falconmobile.bilim_scan.catalog.repository.BosqichRepository;
import uz.falconmobile.bilim_scan.catalog.repository.GuruhRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuruhService {
    private final GuruhRepository guruhRepository;
    private final BosqichRepository bosqichRepository;

    public List<Guruh> getAll() {
        return guruhRepository.findAll();
    }

    public Guruh getById(String id) {
        return guruhRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + id));
    }

    public Guruh create(GuruhRequestDto dto) {
        Guruh guruh = new Guruh();
        if (dto.getBosqichId() != null) {
            guruh.setBosqich(bosqichRepository.findById(dto.getBosqichId())
                    .orElseThrow(() -> new RuntimeException("Bosqich topilmadi: " + dto.getBosqichId())));
        }

        guruh.setName(requireName(dto.getName()));
        return guruhRepository.save(guruh);
    }

    public Guruh update(String id, GuruhRequestDto dto) {
        Guruh guruh = getById(id);
        if (dto.getBosqichId() != null) {
            guruh.setBosqich(bosqichRepository.findById(dto.getBosqichId())
                    .orElseThrow(() -> new RuntimeException("Bosqich topilmadi: " + dto.getBosqichId())));
        }
        guruh.setName(requireName(dto.getName()));
        return guruhRepository.save(guruh);
    }

    public void delete(String id) {
        guruhRepository.deleteById(id);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Name bo'sh bo'lishi mumkin emas");
        }
        return name.trim();
    }
}
