package com.mangamover.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChapterParser {

    private static final Pattern CHAPTER_PATTERN =
            Pattern.compile("Chapter\\s+(\\d+(?:\\.\\d+)?)(?:[_ ].*)?\\.(cb[zr])$", Pattern.CASE_INSENSITIVE);

    private ChapterParser() {}

    public static Optional<String> extractChapterNumber(String filename) {
        Matcher m = CHAPTER_PATTERN.matcher(filename);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    public static String padChapter(String chapter) {
        int dot = chapter.indexOf('.');
        if (dot >= 0) {
            String intPart = chapter.substring(0, dot);
            String fracPart = chapter.substring(dot);
            return String.format("%03d", Integer.parseInt(intPart)) + fracPart;
        }
        return String.format("%03d", Integer.parseInt(chapter));
    }

    public static String buildKavitaFilename(String seriesName, String paddedChapter, String ext) {
        return seriesName + " Ch." + paddedChapter + "." + ext;
    }

    public static Optional<String> rename(String seriesName, String filename) {
        Matcher m = CHAPTER_PATTERN.matcher(filename);
        if (!m.find()) return Optional.empty();
        String chapter = m.group(1);
        String ext = m.group(2);
        return Optional.of(buildKavitaFilename(seriesName, padChapter(chapter), ext));
    }
}
