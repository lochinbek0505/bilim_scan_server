package uz.falconmobile.bilim_scan.eduplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EduPlanTopicRequestDto {
    @JsonProperty("t/r")
    private Integer tr;
    private String name;
    private Integer soat;
    private String type;
}
