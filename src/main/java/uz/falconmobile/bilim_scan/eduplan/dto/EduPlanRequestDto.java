package uz.falconmobile.bilim_scan.eduplan.dto;

import lombok.Data;

@Data
public class EduPlanRequestDto {
    private String name;
    private String fanId;
    private String kafedraId;
    private String oquvYili;
    private String oquvOyi;
}
