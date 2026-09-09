package uz.falconmobile.bilim_scan.eduplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EduPlanTopicResponseDto {
    private String planId;
    private String id;
    @JsonProperty("t/r")
    private Integer tr;
    private String name;
    private Integer soat;
    private String type;
}
