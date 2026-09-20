package uz.falconmobile.bilim_scan.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.analytics.dto.GlobalStatisticsDto;
import uz.falconmobile.bilim_scan.analytics.dto.GroupStatisticsDto;
import uz.falconmobile.bilim_scan.analytics.dto.StudentMonitoringDto;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.repository.FanRepository;
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
    private final EduTestRepository eduTestRepository;
    private final UserRepository userRepository;
    private final FanRepository fanRepository; // fanId orqali fanni topish uchun qo'shildi

    // 1. Talaba monitoringi (Yillar, oylar, fanlar kesimida)
    public StudentMonitoringDto getStudentMonitoring(String studentId) {
        List<StudentExam> allExams = studentExamRepository.findByStudentId(studentId);

        if (allExams.isEmpty()) {
            return StudentMonitoringDto.builder().studentId(studentId).build();
        }

        Map<String, Map<String, Map<String, List<StudentExam>>>> groupedData = new HashMap<>();
        double totalPercentageSum = 0;

        for (StudentExam studentExam : allExams) {
            double currentPercentage = studentExam.getPercentage() != null ? studentExam.getPercentage() : 0.0;
            totalPercentageSum += currentPercentage;

            ExamSession session = examSessionRepository.findById(studentExam.getExamSessionId()).orElse(null);
            if (session == null) continue;

            // Yil va oyni to'g'ridan-to'g'ri ExamSession'dan olamiz
            String year = session.getOquv_yili() != null ? session.getOquv_yili() : "Noma'lum yil";
            String month = session.getOquv_oyi() != null ? session.getOquv_oyi() : "Noma'lum oy";

            // Fanni ExamSession'dagi fanId orqali olamiz
            String subjectName = "Noma'lum fan";
            if (session.getFanId() != null) {
                Fan fan = fanRepository.findById(session.getFanId()).orElse(null);
                if (fan != null && fan.getName() != null) {
                    subjectName = fan.getName();
                }
            }

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
                        double currentPercentage = exam.getPercentage() != null ? exam.getPercentage() : 0.0;
                        subjectTotal += currentPercentage;

                        ExamSession s = examSessionRepository.findById(exam.getExamSessionId()).orElse(null);
                        examDtos.add(StudentMonitoringDto.ExamResultDto.builder()
                                .examSessionId(exam.getExamSessionId())
                                .examName(s != null ? s.getName() : "Noma'lum imtihon")
                                .date(exam.getFinishedAt())
                                .percentage(currentPercentage)
                                .masteryLevel(exam.getMasteryLevel() != null ? exam.getMasteryLevel() : MasteryLevel.FAILED)

                                .isSuspicious(exam.getIsSuspicious())
                                .suspicionReason(exam.getSuspicionReason())
                                .timeTakenSeconds(exam.getTimeTakenSeconds())
                                // -----------------------------

                                .build());
                    }

                    double subjectAverage = examsList.isEmpty() ? 0 : (subjectTotal / examsList.size());

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
        List<String> allStudentIds = userRepository.findByRole(Role.USER).stream()
                .map(User::getId)
                .toList();
        List<StudentExam> allExams = studentExamRepository.findByStudentIdIn(allStudentIds);
        return buildGlobalStatistics("LYCEUM", null, allExams);
    }

    // 4. BOSQICH (KURS) BO'YICHA STATISTIKA
    public GlobalStatisticsDto getStageStatistics(String bosqichId) {
        List<String> stageStudentIds = userRepository.findByBosqichId_IdAndRole(bosqichId, Role.USER).stream()
                .map(User::getId)
                .toList();
        List<StudentExam> stageExams = studentExamRepository.findByStudentIdIn(stageStudentIds);
        return buildGlobalStatistics("STAGE", bosqichId, stageExams);
    }

    // 5. FAN BO'YICHA UMUMIY STATISTIKA (Barcha kurslar va guruhlar kesimida)
    public GlobalStatisticsDto getSubjectStatistics(String fanId) {
        // Endi ExamSession o'zida fanId saqlaydi, shu fanId ga teng sessiyalarni ajratib olamiz
        List<String> sessionIds = examSessionRepository.findAll().stream()
                .filter(session -> fanId.equals(session.getFanId()))
                .map(ExamSession::getId)
                .toList();

        List<StudentExam> subjectExams = studentExamRepository.findByExamSessionIdIn(sessionIds);
        return buildGlobalStatistics("SUBJECT", fanId, subjectExams);
    }

    // 2. GURUH UCHUN UMUMIY STATISTIKA (Fanlar kesimida)
    public GroupStatisticsDto getGroupStatistics(String guruhId) {
        List<ExamSession> groupSessions = examSessionRepository.findAll().stream()
                .filter(s -> s.getGuruh() != null && s.getGuruh().getId().equals(guruhId))
                .toList();

        Set<String> uniqueStudents = new HashSet<>();
        Map<String, GroupStatisticsDto.SubjectStatsDto> subjectStatsMap = new HashMap<>();
        double allStudentsTotalPercentage = 0;
        int examCount = 0;

        for (ExamSession session : groupSessions) {
            String subjectName = "Boshqa fanlar";
            if (session.getFanId() != null) {
                Fan fan = fanRepository.findById(session.getFanId()).orElse(null);
                if (fan != null && fan.getName() != null) {
                    subjectName = fan.getName();
                }
            }

            List<StudentExam> exams = studentExamRepository.findAll().stream()
                    .filter(e -> e.getExamSessionId().equals(session.getId()))
                    .toList();

            for (StudentExam exam : exams) {
                uniqueStudents.add(exam.getStudentId());

                double currentPercentage = exam.getPercentage() != null ? exam.getPercentage() : 0.0;
                allStudentsTotalPercentage += currentPercentage;
                examCount++;

                subjectStatsMap.putIfAbsent(subjectName, GroupStatisticsDto.SubjectStatsDto.builder()
                        .subjectName(subjectName).averagePercentage(0.0)
                        .suspiciousCount(0) // <-- YANGI
                        .masteredCount(0).satisfactoryCount(0).failedCount(0).build());

                GroupStatisticsDto.SubjectStatsDto stats = subjectStatsMap.get(subjectName);
                stats.setAveragePercentage(stats.getAveragePercentage() + currentPercentage);

                if (currentPercentage >= 80.0) stats.setMasteredCount(stats.getMasteredCount() + 1);
                else if (currentPercentage >= 60.0) stats.setSatisfactoryCount(stats.getSatisfactoryCount() + 1);
                else stats.setFailedCount(stats.getFailedCount() + 1);

                if (Boolean.TRUE.equals(exam.getIsSuspicious())) {
                    stats.setSuspiciousCount(stats.getSuspiciousCount() + 1);
                }

            }
        }

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

        Map<String, List<Double>> timeDynamicMap = new HashMap<>();
        Map<String, GroupStatisticsDto.SubjectStatsDto> subjectStatsMap = new HashMap<>();

        for (StudentExam exam : exams) {
            uniqueStudents.add(exam.getStudentId());
            double percentage = exam.getPercentage() != null ? exam.getPercentage() : 0.0;
            totalPercentage += percentage;

            if (percentage >= 80.0) mastered++;
            else if (percentage >= 60.0) satisfactory++;
            else failed++;

            ExamSession session = examSessionRepository.findById(exam.getExamSessionId()).orElse(null);
            if (session != null) {
                // Dinamika kalitini yaratishda yil va oyni ExamSession'dan olamiz
                String year = session.getOquv_yili() != null ? session.getOquv_yili() : "Noma'lum yil";
                String month = session.getOquv_oyi() != null ? session.getOquv_oyi() : "Noma'lum oy";
                String timeKey = year + "_" + month;

                timeDynamicMap.putIfAbsent(timeKey, new ArrayList<>());
                timeDynamicMap.get(timeKey).add(percentage);

                if (!scope.equals("SUBJECT") && session.getFanId() != null) {
                    String subjectName = "Noma'lum fan";
                    Fan fan = fanRepository.findById(session.getFanId()).orElse(null);
                    if (fan != null && fan.getName() != null) {
                        subjectName = fan.getName();
                    }

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