package com.safix.checkout.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SyllabusService {

    private static final Pattern SECTOR_LINE = Pattern.compile("^\\s*(\\d+)\\.\\s*(.+?)\\s*$");
    private static final Pattern COURSE_HEADING = Pattern.compile("^\\s*\\d+\\s*[\\.)]\\s*(.+?)\\s*$");
    private static final Pattern COURSE_FEE_PATTERN = Pattern.compile("(?im)\\b(?:fees?|course\\s*fee|tuition)\\b[^\\r\\n]{0,30}?(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]{2,})");
    private static final Pattern RUPEE_FEE_PATTERN = Pattern.compile("(?im)(?:₹|rs\\.?|inr)\\s*([0-9][0-9,]{2,})");

    private final Map<Integer, String> sectorFileByIndex = new LinkedHashMap<>();
    private final Map<Integer, String> sectorNameByIndex = new LinkedHashMap<>();
    private final Map<Integer, List<String>> sectorCoursesByIndex = new LinkedHashMap<>();
    private final Map<String, Integer> courseToSectorByNormalized = new HashMap<>();
    private final Map<String, String> courseFeeByNormalized = new HashMap<>();
    private final Map<Integer, String> sectorContentCache = new HashMap<>();
    private final Map<Integer, Map<String, String>> sectorCourseSectionsByNormalized = new HashMap<>();

    @PostConstruct
    public void init() {
        sectorFileByIndex.put(1, "BEAUTY SECTOR1.txt");
        sectorFileByIndex.put(2, "IT Sector 2.txt");
        sectorFileByIndex.put(3, "healthcare sector3.txt");
        sectorFileByIndex.put(4, "construction and engineering ssksills sector4.txt");
        sectorFileByIndex.put(5, "management n finance sector5.txt");
        sectorFileByIndex.put(6, "media and entertain sector6.txt");
        sectorFileByIndex.put(7, "hospitality and tourism sector7.txt");
        sectorFileByIndex.put(8, "apparel and fashion design sector8.txt");
        sectorFileByIndex.put(9, "electronic n hardware sector9.txt");
        sectorFileByIndex.put(10, "logistic and suply chain sector 10.txt");

        parseWebsiteCourseList();
        parseAllSectorSections();
        applyDefaultCourseFees();
    }

    public String getSectorByCourse(String course) {
        Integer sector = courseToSectorByNormalized.get(normalize(course));
        if (sector == null) {
            Integer inferredSector = inferSectorByCourseHeading(course);
            if (inferredSector == null) {
                return "Course Syllabus";
            }
            return sectorNameByIndex.getOrDefault(inferredSector, "Course Syllabus");
        }
        return sectorNameByIndex.getOrDefault(sector, "Course Syllabus");
    }

    public String getSyllabusByCourse(String course) {
        if (course == null || course.isBlank()) {
            return "Syllabus not found: course name is missing.";
        }

        String normalizedCourse = normalize(course);
        Integer sector = courseToSectorByNormalized.get(normalizedCourse);

        if (sector != null) {
            Map<String, String> sections = sectorCourseSectionsByNormalized.getOrDefault(sector, Collections.emptyMap());
            String section = sections.get(normalizedCourse);
            if (section != null && !section.isBlank()) {
                return section;
            }

            String fuzzySection = findBestSectionForCourse(normalizedCourse, sections);
            if (fuzzySection != null) {
                return fuzzySection;
            }

            String fullSectorText = sectorContentCache.get(sector);
            if (fullSectorText != null && !fullSectorText.isBlank()) {
                return fullSectorText;
            }
        }

        for (Map<String, String> sections : sectorCourseSectionsByNormalized.values()) {
            String fuzzySection = findBestSectionForCourse(normalizedCourse, sections);
            if (fuzzySection != null) {
                return fuzzySection;
            }
        }

        String headingBasedSection = findSectionByHeadingAcrossSectors(course);
        if (headingBasedSection != null) {
            return headingBasedSection;
        }

        return "Syllabus not found for \"" + course + "\".";
    }

    public String getFeeByCourse(String course) {
        if (course == null || course.isBlank()) {
            return "5000";
        }

        String normalizedCourse = normalize(course);
        String exact = courseFeeByNormalized.get(normalizedCourse);
        if (exact != null && !exact.isBlank()) {
            return exact;
        }

        String bestKey = null;
        int bestScore = -1;
        for (String key : courseFeeByNormalized.keySet()) {
            int score = similarityScore(normalizedCourse, key);
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }

        if (bestKey != null && bestScore >= 4) {
            return courseFeeByNormalized.get(bestKey);
        }

        return "5000";
    }

    private String findBestSectionForCourse(String normalizedCourse, Map<String, String> sections) {
        String bestKey = null;
        int bestScore = -1;
        for (String key : sections.keySet()) {
            int score = similarityScore(normalizedCourse, key);
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        if (bestKey != null && bestScore >= 4) {
            return sections.get(bestKey);
        }
        return null;
    }

    private void parseWebsiteCourseList() {
        Path path = resolvePath("website course.txt");
        if (path == null) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            Integer currentSector = null;
            for (String rawLine : lines) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }

                Matcher sectorMatcher = SECTOR_LINE.matcher(line);
                if (sectorMatcher.matches()) {
                    currentSector = Integer.parseInt(sectorMatcher.group(1));
                    String sectorName = sectorMatcher.group(2).trim();
                    sectorNameByIndex.put(currentSector, sectorName);
                    sectorCoursesByIndex.putIfAbsent(currentSector, new ArrayList<>());
                    continue;
                }

                if (currentSector == null) {
                    continue;
                }

                String course = extractBulletText(line);
                if (course == null || course.isBlank()) {
                    continue;
                }

                sectorCoursesByIndex.get(currentSector).add(course);
                courseToSectorByNormalized.put(normalize(course), currentSector);
            }
        } catch (IOException ignored) {
            // Keep app running even if course text file is unavailable.
        }
    }

    private String extractBulletText(String line) {
        String cleaned = line
                .replaceFirst("^(â€¢|•|\\-|\\*)\\s*", "")
                .trim();

        if (cleaned.equals(line)) {
            return null;
        }
        return cleaned;
    }

    private Integer inferSectorByCourseHeading(String course) {
        if (course == null || course.isBlank()) {
            return null;
        }

        String normalizedRequested = normalize(course);
        Integer bestSector = null;
        int bestScore = -1;

        for (Map.Entry<Integer, String> entry : sectorContentCache.entrySet()) {
            Integer sector = entry.getKey();
            String content = entry.getValue();
            if (content == null || content.isBlank()) {
                continue;
            }

            for (String line : content.split("\\R")) {
                Matcher matcher = COURSE_HEADING.matcher(line == null ? "" : line);
                if (!matcher.matches()) {
                    continue;
                }
                int score = similarityScore(normalizedRequested, normalize(matcher.group(1)));
                if (score > bestScore) {
                    bestScore = score;
                    bestSector = sector;
                }
            }
        }

        return bestScore >= 4 ? bestSector : null;
    }

    private String findSectionByHeadingAcrossSectors(String course) {
        if (course == null || course.isBlank()) {
            return null;
        }

        for (String content : sectorContentCache.values()) {
            String section = extractSectionByHeading(content, course);
            if (section != null && !section.isBlank()) {
                return section;
            }
        }

        return null;
    }

    private String extractSectionByHeading(String content, String requestedCourse) {
        if (content == null || content.isBlank() || requestedCourse == null || requestedCourse.isBlank()) {
            return null;
        }

        List<String> lines = Arrays.asList(content.split("\\R"));
        String normalizedRequested = normalize(requestedCourse);
        int bestScore = -1;
        int bestStart = -1;

        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = COURSE_HEADING.matcher(lines.get(i) == null ? "" : lines.get(i));
            if (!matcher.matches()) {
                continue;
            }

            int score = similarityScore(normalizedRequested, normalize(matcher.group(1)));
            if (score > bestScore) {
                bestScore = score;
                bestStart = i;
            }
        }

        if (bestStart < 0 || bestScore < 4) {
            return null;
        }

        int end = lines.size();
        for (int i = bestStart + 1; i < lines.size(); i++) {
            Matcher matcher = COURSE_HEADING.matcher(lines.get(i) == null ? "" : lines.get(i));
            if (matcher.matches()) {
                end = i;
                break;
            }
        }

        StringBuilder block = new StringBuilder();
        for (int i = bestStart; i < end; i++) {
            block.append(lines.get(i)).append(System.lineSeparator());
        }

        String section = block.toString().trim();
        return section.isBlank() ? null : section;
    }

    private void parseAllSectorSections() {
        for (Map.Entry<Integer, String> entry : sectorFileByIndex.entrySet()) {
            Integer sectorIndex = entry.getKey();
            Path path = resolvePath(entry.getValue());
            if (path == null) {
                continue;
            }

            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                sectorContentCache.put(sectorIndex, content);
                Map<String, String> sections = extractSections(content, sectorCoursesByIndex.getOrDefault(sectorIndex, Collections.emptyList()));
                sectorCourseSectionsByNormalized.put(sectorIndex, sections);
                populateFeesFromSections(sections);
            } catch (IOException ignored) {
                // Keep app running even if one sector file is unavailable.
            }
        }
    }

    private void populateFeesFromSections(Map<String, String> sections) {
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String fee = extractFee(entry.getValue());
            if (fee != null) {
                courseFeeByNormalized.putIfAbsent(entry.getKey(), fee);
            }
        }
    }

    private String extractFee(String sectionText) {
        if (sectionText == null || sectionText.isBlank()) {
            return null;
        }

        Matcher taggedMatcher = COURSE_FEE_PATTERN.matcher(sectionText);
        if (taggedMatcher.find()) {
            return sanitizeAmount(taggedMatcher.group(1));
        }

        Matcher rupeeMatcher = RUPEE_FEE_PATTERN.matcher(sectionText);
        if (rupeeMatcher.find()) {
            return sanitizeAmount(rupeeMatcher.group(1));
        }

        return null;
    }

    private String sanitizeAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        String digits = rawAmount.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private void applyDefaultCourseFees() {
        Map<Integer, List<String>> sectorFees = new HashMap<>();
        sectorFees.put(1, List.of("4999", "7999", "12999", "6999", "6999", "6999", "6999", "6999"));
        sectorFees.put(2, List.of("3499", "6999", "6999", "6999", "6999", "6999", "6999", "6999"));
        sectorFees.put(3, List.of("6999", "7999", "7999", "7999", "6999", "6999", "6999", "6999"));
        sectorFees.put(4, List.of("6999", "6999", "6999", "6999", "7999", "7999", "7999", "7999"));
        sectorFees.put(5, List.of("9999", "5999", "7999", "6999", "5999", "6999", "6999", "7999"));
        sectorFees.put(6, List.of("8999", "8999", "8999", "7999", "7999", "8999", "6999", "8999"));
        sectorFees.put(7, List.of("8999", "8999", "7999", "6999", "6999", "7999", "6999", "7999"));
        sectorFees.put(8, List.of("8999", "6999", "6999", "6999", "7999", "7999", "7999", "8999"));
        sectorFees.put(9, List.of("6999", "7999", "7999", "7999", "8999", "8999", "7999", "7999"));
        sectorFees.put(10, List.of("7999", "6999", "6999", "6999", "6999", "7999", "6999", "6999"));

        for (Map.Entry<Integer, List<String>> entry : sectorFees.entrySet()) {
            Integer sectorIndex = entry.getKey();
            List<String> fees = entry.getValue();
            List<String> courses = sectorCoursesByIndex.getOrDefault(sectorIndex, Collections.emptyList());

            for (int i = 0; i < courses.size() && i < fees.size(); i++) {
                String normalizedCourse = normalize(courses.get(i));
                courseFeeByNormalized.putIfAbsent(normalizedCourse, fees.get(i));
            }
        }
    }

    private Map<String, String> extractSections(String content, List<String> expectedCourses) {
        Map<String, String> sections = new HashMap<>();
        if (content == null || content.isBlank()) {
            return sections;
        }

        List<String> lines = Arrays.asList(content.split("\\R"));
        String currentCourse = null;
        StringBuilder currentBlock = new StringBuilder();

        for (String line : lines) {
            Matcher matcher = COURSE_HEADING.matcher(line == null ? "" : line);
            if (matcher.matches()) {
                String candidate = matcher.group(1).trim();
                String matchedExpectedCourse = findBestExpectedCourse(candidate, expectedCourses);
                if (matchedExpectedCourse != null) {
                    saveSection(sections, currentCourse, currentBlock);
                    currentCourse = matchedExpectedCourse;
                    currentBlock = new StringBuilder();
                    currentBlock.append(line).append(System.lineSeparator());
                    continue;
                }
            }

            if (currentCourse != null) {
                currentBlock.append(line).append(System.lineSeparator());
            }
        }

        saveSection(sections, currentCourse, currentBlock);
        return sections;
    }

    private void saveSection(Map<String, String> sections, String course, StringBuilder block) {
        if (course == null) {
            return;
        }
        String text = block.toString().trim();
        if (!text.isBlank()) {
            sections.put(normalize(course), text);
        }
    }

    private String findBestExpectedCourse(String candidate, List<String> expectedCourses) {
        if (expectedCourses == null || expectedCourses.isEmpty()) {
            return null;
        }

        String normalizedCandidate = normalize(candidate);
        String bestCourse = null;
        int bestScore = -1;

        for (String expected : expectedCourses) {
            int score = similarityScore(normalizedCandidate, normalize(expected));
            if (score > bestScore) {
                bestScore = score;
                bestCourse = expected;
            }
        }

        return bestScore >= 4 ? bestCourse : null;
    }

    private int similarityScore(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return 0;
        }
        if (a.equals(b)) {
            return 100;
        }

        int score = 0;
        if (a.contains(b) || b.contains(a)) {
            score += 30;
        }

        Set<String> tokensA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> tokensB = new HashSet<>(Arrays.asList(b.split("\\s+")));
        tokensA.remove("");
        tokensB.remove("");

        int overlap = 0;
        for (String token : tokensA) {
            if (tokensB.contains(token)) {
                overlap++;
            }
        }
        score += overlap * 5;
        return score;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private Path resolvePath(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        Path path = Path.of(filename);
        if (Files.exists(path)) {
            return path;
        }

        Path userDirPath = Path.of(System.getProperty("user.dir"), filename);
        if (Files.exists(userDirPath)) {
            return userDirPath;
        }

        return null;
    }
}

