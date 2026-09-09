package uz.falconmobile.bilim_scan.eduplan.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemResponseDto;

import java.time.Instant;
import java.util.List;

@Builder
@Data
public class EduPlanResponseDto {
    private String id;
    private String name;
    private Instant createAt;
    private Instant updateAt;
    private CatalogItemResponseDto fan;
    private CatalogItemResponseDto kafedra;
    private String oquvYili;
    private String oquvOyi;
    private List<EduPlanTopicResponseDto> topics;
}
