package uz.falconmobile.bilim_scan.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemRequestDto;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.repository.FanRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FanService {
    private final FanRepository fanRepository;

    public List<Fan> getAll() {
        return fanRepository.findAll();
    }

    public Fan getById(String id) {
        return fanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fan topilmadi: " + id));
    }

    public Fan create(CatalogItemRequestDto dto) {
        Fan fan = new Fan();
        fan.setName(requireName(dto.getName()));
        return fanRepository.save(fan);
    }

    public Fan update(String id, CatalogItemRequestDto dto) {
        Fan fan = getById(id);
        fan.setName(requireName(dto.getName()));
        return fanRepository.save(fan);
    }

    public void delete(String id) {
        fanRepository.deleteById(id);
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Name bo'sh bo'lishi mumkin emas");
        }
        return name.trim();
    }
}
