package uz.falconmobile.bilim_scan.exam.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class StudentAnswerSubmitDto {

    private Map<String, List<String>> answers;
}