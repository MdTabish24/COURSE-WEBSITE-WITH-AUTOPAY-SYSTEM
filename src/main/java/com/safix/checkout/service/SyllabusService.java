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

    private final Map<Integer, String> sectorFileByIndex = new LinkedHashMap<>();
    private final Map<Integer, String> sectorNameByIndex = new LinkedHashMap<>();
    private final Map<Integer, List<String>> sectorCoursesByIndex = new LinkedHashMap<>();
    private final Map<String, Integer> courseToSectorByNormalized = new HashMap<>();
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
    }

    public String getSectorByCourse(String course) {
        Integer sector = courseToSectorByNormalized.get(normalize(course));
        if (sector == null) {
            return "Course Syllabus";
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

        return "Syllabus not found for \"" + course + "\".";
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
            } catch (IOException ignored) {
                // Keep app running even if one sector file is unavailable.
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

