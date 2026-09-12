package uz.falconmobile.bilim_scan.catalog.dto;

import lombok.*;
import lombok.Data;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FanResponseDto {

    private String id;
    private String name;
    private Kafedra kafedra;

}
