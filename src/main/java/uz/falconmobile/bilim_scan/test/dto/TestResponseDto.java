package uz.falconmobile.bilim_scan.test.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemResponseDto;

import java.time.Instant;

@Builder
@Data
public class TestResponseDto {
    private String id;
    private String name;
    private Instant createAt;
    private Instant updateAt;
    private CatalogItemResponseDto fan;
    private CatalogItemResponseDto kafedra;
    private EduPlanSummaryDto oquvReja;
    private Integer ajratilganVaqt;
}
