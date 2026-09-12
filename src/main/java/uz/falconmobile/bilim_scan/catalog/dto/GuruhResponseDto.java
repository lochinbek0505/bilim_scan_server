package uz.falconmobile.bilim_scan.catalog.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.catalog.model.Bosqich;

@Data
@Builder
public class GuruhResponseDto {

    private String id;
    private String name;
    private Bosqich bosqich;
}
