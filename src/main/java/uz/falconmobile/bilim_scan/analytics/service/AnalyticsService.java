package uz.falconmobile.bilim_scan.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.analytics.dto.GlobalStatisticsDto;
import uz.falconmobile.bilim_scan.analytics.dto.GroupStatisticsDto;
import uz.falconmobile.bilim_scan.analytics.dto.StudentMonitoringDto;
import uz.falconmobile.bilim_scan.exam.model.ExamSession;
import uz.falconmobile.bilim_scan.exam.model.MasteryLevel;
import uz.falconmobile.bilim_scan.exam.model.StudentExam;
import uz.falconmobile.bilim_scan.exam.repository.ExamSessionRepository;
import uz.falconmobile.bilim_scan.exam.repository.StudentExamRepository;
import uz.falconmobile.bilim_scan.test.model.EduTest;
import uz.falconmobile.bilim_scan.test.repository.EduTestRepository;
import uz.falconmobile.bilim_scan.user.model.Role;
import uz.falconmobile.bilim_scan.user.model.User;
import uz.falconmobile.bilim_scan.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final StudentExamRepository studentExamRepository;
    private final ExamSessionRepository examSessionRepository;
    private final EduTestRepository eduTestRepository; // EduTest modelini bazadan olish uchun
    private final UserRepository userRepository; // Konstruktorga qo'shishni unutmang
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
    public GlobalStatisticsDto getLyceumStatistics() {
        // Barcha USER roldagi o'quvchilarni olamiz
        List<String> allStudentIds = userRepository.findByRole(Role.USER).stream()
                .map(User::getId)
                .toList();

        List<StudentExam> allExams = studentExamRepository.findByStudentIdIn(allStudentIds);

        return buildGlobalStatistics("LYCEUM", null, allExams);
    }

    // =========================================================================
    // 4. BOSQICH (KURS) BO'YICHA STATISTIKA
    // =========================================================================
    public GlobalStatisticsDto getStageStatistics(String bosqichId) {
        // Faqat bitta bosqichdagi talabalarni olamiz
        List<String> stageStudentIds = userRepository.findByBosqichId_IdAndRole(bosqichId, Role.USER).stream()
                .map(User::getId)
                .toList();

        List<StudentExam> stageExams = studentExamRepository.findByStudentIdIn(stageStudentIds);

        return buildGlobalStatistics("STAGE", bosqichId, stageExams);
    }

    // =========================================================================
    // 5. FAN BO'YICHA UMUMIY STATISTIKA (Barcha kurslar va guruhlar kesimida)
    // =========================================================================
    public GlobalStatisticsDto getSubjectStatistics(String fanId) {
        // Shu fanga tegishli barcha EduTest larni topamiz
        List<String> testIds = eduTestRepository.findAll().stream()
                .filter(test -> test.getFan() != null && test.getFan().getId().equals(fanId))
                .map(EduTest::getId)
                .toList();

        // Shu testlarga ulangan barcha sessiyalarni olamiz
        List<String> sessionIds = examSessionRepository.findByTestIn(testIds).stream()
                .map(ExamSession::getId)
                .toList();

        // Ushbu sessiyalardagi barcha talaba natijalari
        List<StudentExam> subjectExams = studentExamRepository.findByExamSessionIdIn(sessionIds);

        return buildGlobalStatistics("SUBJECT", fanId, subjectExams);
    }
    // =========================================================================
    // 2. GURUH UCHUN UMUMIY STATISTIKA (Fanlar kesimida)
    // =========================================================================
    public GroupStatisticsDto getGroupStatistics(String guruhId) {
        // Bu yerda guruhga tegishli barcha sessionlarni topamiz
        List<ExamSession> groupSessions = examSessionRepository.findAll().stream()
                .filter(s -> s.getGuruh() != null && s.getGuruh().getId().equals(guruhId))
                .toList();

        Set<String> uniqueStudents = new HashSet<>();
        Map<String, GroupStatisticsDto.SubjectStatsDto> subjectStatsMap = new HashMap<>();
        double allStudentsTotalPercentage = 0;
        int examCount = 0;

        for (ExamSession session : groupSessions) {
            EduTest test = eduTestRepository.findById(session.getTest()).orElse(null);
            String subjectName = (test != null && test.getFan() != null) ? test.getFan().getName() : "Boshqa fanlar";

            // Ushbu sessiyadagi barcha talaba natijalari
            List<StudentExam> exams = studentExamRepository.findAll().stream()
                    .filter(e -> e.getExamSessionId().equals(session.getId()))
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
    private GlobalStatisticsDto buildGlobalStatistics(String scope, String scopeId, List<StudentExam> exams) {
        Set<String> uniqueStudents = new HashSet<>();
        double totalPercentage = 0.0;
        int mastered = 0, satisfactory = 0, failed = 0;

        // Vaqt dinamikasi uchun (Yil_Oy -> O'zlashtirishlar ro'yxati)
        Map<String, List<Double>> timeDynamicMap = new HashMap<>();
        // Fanlar kesimi uchun
        Map<String, GroupStatisticsDto.SubjectStatsDto> subjectStatsMap = new HashMap<>();

        for (StudentExam exam : exams) {
            uniqueStudents.add(exam.getStudentId());
            double percentage = exam.getPercentage() != null ? exam.getPercentage() : 0.0;
            totalPercentage += percentage;

            // Mastery Level hisoblash
            if (percentage >= 80.0) mastered++;
            else if (percentage >= 60.0) satisfactory++;
            else failed++;

            // Imtihon sessiyasi va Test orqali vaqt/fan ma'lumotlarini olish
            ExamSession session = examSessionRepository.findById(exam.getExamSessionId()).orElse(null);
            if (session != null && session.getTest() != null) {
                EduTest test = eduTestRepository.findById(session.getTest()).orElse(null);
                if (test != null) {
                    // Dinamika uchun kalit (Masalan: "2023-2024_Sentyabr")
                    String timeKey = test.getOquvYili() + "_" + test.getOquvOyi();
                    timeDynamicMap.putIfAbsent(timeKey, new ArrayList<>());
                    timeDynamicMap.get(timeKey).add(percentage);

                    // Fanlar reytingi uchun yig'ish (faqat butun litsey yoki bosqich uchun)
                    if (!scope.equals("SUBJECT") && test.getFan() != null) {
                        String subjectName = test.getFan().getName();
                        subjectStatsMap.putIfAbsent(subjectName, GroupStatisticsDto.SubjectStatsDto.builder()
                                .subjectName(subjectName).averagePercentage(0.0)
                                .masteredCount(0).satisfactoryCount(0).failedCount(0).build());

                        GroupStatisticsDto.SubjectStatsDto sDto = subjectStatsMap.get(subjectName);
                        sDto.setAveragePercentage(sDto.getAveragePercentage() + percentage);
                        if (percentage >= 80.0) sDto.setMasteredCount(sDto.getMasteredCount() + 1);
                        else if (percentage >= 60.0) sDto.setSatisfactoryCount(sDto.getSatisfactoryCount() + 1);
                        else sDto.setFailedCount(sDto.getFailedCount() + 1);
                    }
                }
            }
        }



        // Dinamika ma'lumotlarini DTO ga o'girish
        List<GlobalStatisticsDto.TimeDynamicDto> dynamics = timeDynamicMap.entrySet().stream().map(entry -> {
            String[] parts = entry.getKey().split("_");
            List<Double> scores = entry.getValue();
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            return GlobalStatisticsDto.TimeDynamicDto.builder()
                    .year(parts[0])
                    .month(parts.length > 1 ? parts[1] : "Noma'lum")
                    .averagePercentage(avg)
                    .examCount(scores.size())
                    .build();
        }).toList();

        // Fanlar ko'rsatkichlarining o'rtacha qiymatini hisoblash
        List<GroupStatisticsDto.SubjectStatsDto> subjectPerformances = new ArrayList<>(subjectStatsMap.values());
        for (GroupStatisticsDto.SubjectStatsDto stat : subjectPerformances) {
            int count = stat.getMasteredCount() + stat.getSatisfactoryCount() + stat.getFailedCount();
            if (count > 0) stat.setAveragePercentage(stat.getAveragePercentage() / count);
        }

        return GlobalStatisticsDto.builder()
                .scope(scope)
                .scopeId(scopeId)
                .totalStudentsParticipated(uniqueStudents.size())
                .totalExamsTaken(exams.size())
                .overallAveragePercentage(exams.isEmpty() ? 0.0 : totalPercentage / exams.size())
                .masteredCount(mastered)
                .satisfactoryCount(satisfactory)
                .failedCount(failed)
                .timeDynamics(dynamics)
                .subjectPerformances(subjectPerformances)
                .build();
    }
    private MasteryLevel calculateMastery(double percentage) {
        if (percentage < 60.0) return MasteryLevel.FAILED;
        if (percentage < 80.0) return MasteryLevel.SATISFACTORY;
        return MasteryLevel.MASTERED;
    }
}