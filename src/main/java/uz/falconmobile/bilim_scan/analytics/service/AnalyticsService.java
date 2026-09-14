package uz.falconmobile.bilim_scan.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.analytics.dto.GroupStatisticsDto;
import uz.falconmobile.bilim_scan.analytics.dto.StudentMonitoringDto;
import uz.falconmobile.bilim_scan.exam.model.ExamSession;
import uz.falconmobile.bilim_scan.exam.model.MasteryLevel;
import uz.falconmobile.bilim_scan.exam.model.StudentExam;
import uz.falconmobile.bilim_scan.exam.repository.ExamSessionRepository;
import uz.falconmobile.bilim_scan.exam.repository.StudentExamRepository;
import uz.falconmobile.bilim_scan.test.model.EduTest;
import uz.falconmobile.bilim_scan.test.repository.EduTestRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final StudentExamRepository studentExamRepository;
    private final ExamSessionRepository examSessionRepository;
    private final EduTestRepository eduTestRepository; // EduTest modelini bazadan olish uchun

    // 1. Talaba monitoringi (Yillar, oylar, fanlar kesimida)
    public StudentMonitoringDto getStudentMonitoring(String studentId) {
        List<StudentExam> allExams = studentExamRepository.findByStudentId(studentId);
        System.out.println("Talaba ID: " + studentId + ", Topilgan imtihonlar soni: " + allExams.size());
        if (allExams.isEmpty()) {
            return StudentMonitoringDto.builder().studentId(studentId).build();
        }

        Map<String, Map<String, Map<String, List<StudentExam>>>> groupedData = new HashMap<>();
        double totalPercentageSum = 0;
        for (StudentExam studentExam : allExams) {
            // Null-check (agar null bo'lsa 0.0 deb olamiz)
            double currentPercentage = studentExam.getPercentage() != null ? studentExam.getPercentage() : 0.0;
            totalPercentageSum += currentPercentage;

            ExamSession session = examSessionRepository.findById(studentExam.getExamSessionId()).orElse(null);
            if (session == null) continue;
            // ... qolgan kodlar o'zgarishsiz qoladi
            System.out.println("Imtihon sessiyasi ID: " + studentExam.getExamSessionId());

            // EduTest orqali fan va vaqt ma'lumotlarini olamiz
            EduTest test = eduTestRepository.findById(session.getTest()).orElse(null); //[cite: 6]
            if (test == null || test.getOquvReja() == null) continue;
            System.out.println("EduTest ID: " + session.getTest() + ", O'quv rejasi: " + test.getOquvReja().getId());
            String year = test.getOquvYili();
            String month = test.getOquvOyi();
            String subjectName = test.getFan() != null ? test.getFan().getName() : "Noma'lum fan";

            groupedData.putIfAbsent(year, new HashMap<>());
            groupedData.get(year).putIfAbsent(month, new HashMap<>());
            groupedData.get(year).get(month).putIfAbsent(subjectName, new ArrayList<>());
            
            groupedData.get(year).get(month).get(subjectName).add(studentExam);
        }

        // DTO'ga yig'ish jarayoni
        List<StudentMonitoringDto.YearlyMonitoringDto> yearlyList = new ArrayList<>();
        
        groupedData.forEach((year, monthsMap) -> {
            List<StudentMonitoringDto.MonthlyMonitoringDto> monthlyList = new ArrayList<>();
            
            monthsMap.forEach((month, subjectsMap) -> {
                List<StudentMonitoringDto.SubjectMonitoringDto> subjectList = new ArrayList<>();

                subjectsMap.forEach((subjectName, examsList) -> {
                    double subjectTotal = 0;
                    List<StudentMonitoringDto.ExamResultDto> examDtos = new ArrayList<>();

                    for (StudentExam exam : examsList) {
                        // Null-check bu yerda ham o'rnatiladi
                        double currentPercentage = exam.getPercentage() != null ? exam.getPercentage() : 0.0;
                        subjectTotal += currentPercentage;

                        ExamSession s = examSessionRepository.findById(exam.getExamSessionId()).orElse(null);

                        examDtos.add(StudentMonitoringDto.ExamResultDto.builder()
                                .examSessionId(exam.getExamSessionId())
                                .examName(s != null ? s.getName() : "Noma'lum imtihon")
                                .date(exam.getFinishedAt()) // Agar submit qilinmagan bo'lsa, date null bo'lishi normal holat
                                .percentage(currentPercentage)
                                .masteryLevel(exam.getMasteryLevel() != null ? exam.getMasteryLevel() : MasteryLevel.FAILED) // MasteryLevel null bo'lsa FAILED deymiz
                                .build());
                    }

                    double subjectAverage = subjectTotal / examsList.size();
                    // ... qolgan kodlar o'zgarishsiz qoladi
                    subjectList.add(StudentMonitoringDto.SubjectMonitoringDto.builder()
                            .subjectName(subjectName)
                            .averagePercentage(subjectAverage)
                            .subjectMastery(calculateMastery(subjectAverage))
                            .exams(examDtos)
                            .build());
                });

                monthlyList.add(StudentMonitoringDto.MonthlyMonitoringDto.builder()
                        .month(month)
                        .subjects(subjectList)
                        .build());
            });
            
            yearlyList.add(StudentMonitoringDto.YearlyMonitoringDto.builder()
                    .year(year)
                    .months(monthlyList)
                    .build());
        });

        double overallAverage = totalPercentageSum / allExams.size();

        return StudentMonitoringDto.builder()
                .studentId(studentId)
                .overallPercentage(overallAverage)
                .overallMastery(calculateMastery(overallAverage))
                .academicYears(yearlyList)
                .build();
    }

    // 2. Guruh yoki bosqich uchun umumiy statistika (Fanlar kesimida)
    public GroupStatisticsDto getGroupStatistics(String guruhId) {
        // Bu yerda guruhga tegishli barcha sessionlarni topamiz
        List<ExamSession> groupSessions = examSessionRepository.findAll().stream()
                .filter(s -> s.getGuruh() != null && s.getGuruh().getId().equals(guruhId)) //[cite: 6]
                .toList();

        Set<String> uniqueStudents = new HashSet<>();
        Map<String, GroupStatisticsDto.SubjectStatsDto> subjectStatsMap = new HashMap<>();
        double allStudentsTotalPercentage = 0;
        int examCount = 0;

        for (ExamSession session : groupSessions) {
            EduTest test = eduTestRepository.findById(session.getTest()).orElse(null); //[cite: 6]
            String subjectName = (test != null && test.getFan() != null) ? test.getFan().getName() : "Boshqa fanlar";

            // Ushbu sessiyadagi barcha talaba natijalari
            // studentExamRepository'ga findByExamSessionId ni qo'shishingiz kerak (agar yo'q bo'lsa)
            List<StudentExam> exams = studentExamRepository.findAll().stream()
                    .filter(e -> e.getExamSessionId().equals(session.getId())) //[cite: 7]
                    .toList();

            for (StudentExam exam : exams) {
                uniqueStudents.add(exam.getStudentId());

                // XAVFSIZ QILIB OLISH (Agar null bo'lsa 0 deb qabul qilinadi)
                double currentPercentage = exam.getPercentage() != null ? exam.getPercentage() : 0.0;

                allStudentsTotalPercentage += currentPercentage;
                examCount++;

                // Fan bo'yicha yig'ish
                subjectStatsMap.putIfAbsent(subjectName, GroupStatisticsDto.SubjectStatsDto.builder()
                        .subjectName(subjectName).averagePercentage(0.0)
                        .masteredCount(0).satisfactoryCount(0).failedCount(0).build());

                GroupStatisticsDto.SubjectStatsDto stats = subjectStatsMap.get(subjectName);

                // Vaqtinchalik average ga xavfsiz qiymatni qo'shamiz
                stats.setAveragePercentage(stats.getAveragePercentage() + currentPercentage);

                // Qolgan tekshiruvlarni ham currentPercentage bilan qilamiz
                if (currentPercentage >= 80.0) stats.setMasteredCount(stats.getMasteredCount() + 1);
                else if (currentPercentage >= 60.0) stats.setSatisfactoryCount(stats.getSatisfactoryCount() + 1);
                else stats.setFailedCount(stats.getFailedCount() + 1);
            }
        }

        // Foizlarni o'rtacha qiymatga aylantirish
        List<GroupStatisticsDto.SubjectStatsDto> finalStats = new ArrayList<>(subjectStatsMap.values());
        for (GroupStatisticsDto.SubjectStatsDto stat : finalStats) {
            int totalExamsInSubject = stat.getMasteredCount() + stat.getSatisfactoryCount() + stat.getFailedCount();
            if (totalExamsInSubject > 0) {
                stat.setAveragePercentage(stat.getAveragePercentage() / totalExamsInSubject);
            }
        }

        double overall = examCount > 0 ? (allStudentsTotalPercentage / examCount) : 0;

        return GroupStatisticsDto.builder()
                .guruhId(guruhId)
                .totalStudents(uniqueStudents.size())
                .overallAverage(overall)
                .subjectStats(finalStats)
                .build();
    }

    private MasteryLevel calculateMastery(double percentage) {
        if (percentage < 60.0) return MasteryLevel.FAILED;
        if (percentage < 80.0) return MasteryLevel.SATISFACTORY;
        return MasteryLevel.MASTERED;
    }
}