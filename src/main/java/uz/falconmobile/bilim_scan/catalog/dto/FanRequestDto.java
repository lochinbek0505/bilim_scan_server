package uz.falconmobile.bilim_scan.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FanRequestDto {
    private String name;
    private String kafedraId;
}
