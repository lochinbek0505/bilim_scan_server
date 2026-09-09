package uz.falconmobile.bilim_scan.catalog.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CatalogItemResponseDto {

    private String id;
    private String name;

}
