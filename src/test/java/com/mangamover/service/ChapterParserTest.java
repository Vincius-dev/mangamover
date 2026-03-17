package com.mangamover.service;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ChapterParserTest {

    @Test
    void extractChapterNumber_simpleInteger() {
        assertEquals(Optional.of("7"), ChapterParser.extractChapterNumber("Chapter 7.cbz"));
    }

    @Test
    void extractChapterNumber_decimal() {
        assertEquals(Optional.of("72.5"), ChapterParser.extractChapterNumber("Chapter 72.5.cbz"));
    }

    @Test
    void extractChapterNumber_caseInsensitive() {
        assertEquals(Optional.of("10"), ChapterParser.extractChapterNumber("chapter 10.cbz"));
        assertEquals(Optional.of("10"), ChapterParser.extractChapterNumber("CHAPTER 10.cbz"));
    }

    @Test
    void extractChapterNumber_withSuffix() {
        assertEquals(Optional.of("5"), ChapterParser.extractChapterNumber("Chapter 5_extra stuff.cbz"));
        assertEquals(Optional.of("5"), ChapterParser.extractChapterNumber("Chapter 5 extra stuff.cbz"));
    }

    @Test
    void extractChapterNumber_cbr() {
        assertEquals(Optional.of("3"), ChapterParser.extractChapterNumber("Chapter 3.cbr"));
    }

    @Test
    void extractChapterNumber_noMatch() {
        assertEquals(Optional.empty(), ChapterParser.extractChapterNumber("random_file.cbz"));
        assertEquals(Optional.empty(), ChapterParser.extractChapterNumber("manga.txt"));
    }

    @Test
    void padChapter_singleDigit() {
        assertEquals("007", ChapterParser.padChapter("7"));
    }

    @Test
    void padChapter_twoDigits() {
        assertEquals("072", ChapterParser.padChapter("72"));
    }

    @Test
    void padChapter_threeDigits() {
        assertEquals("100", ChapterParser.padChapter("100"));
    }

    @Test
    void padChapter_fourDigits() {
        assertEquals("1234", ChapterParser.padChapter("1234"));
    }

    @Test
    void padChapter_decimal() {
        assertEquals("072.5", ChapterParser.padChapter("72.5"));
    }

    @Test
    void padChapter_singleDigitDecimal() {
        assertEquals("007.5", ChapterParser.padChapter("7.5"));
    }

    @Test
    void buildKavitaFilename() {
        assertEquals("My Series Ch.007.cbz",
                ChapterParser.buildKavitaFilename("My Series", "007", "cbz"));
    }

    @Test
    void rename_validChapter() {
        Optional<String> result = ChapterParser.rename("Solo Leveling", "Chapter 7.cbz");
        assertTrue(result.isPresent());
        assertEquals("Solo Leveling Ch.007.cbz", result.get());
    }

    @Test
    void rename_decimalChapter() {
        Optional<String> result = ChapterParser.rename("One Piece", "Chapter 72.5.cbz");
        assertTrue(result.isPresent());
        assertEquals("One Piece Ch.072.5.cbz", result.get());
    }

    @Test
    void rename_cbrFile() {
        Optional<String> result = ChapterParser.rename("Naruto", "Chapter 100.cbr");
        assertTrue(result.isPresent());
        assertEquals("Naruto Ch.100.cbr", result.get());
    }

    @Test
    void rename_noMatch() {
        Optional<String> result = ChapterParser.rename("Series", "random_file.cbz");
        assertTrue(result.isEmpty());
    }

    @Test
    void rename_withSuffix() {
        Optional<String> result = ChapterParser.rename("Bleach", "Chapter 5_en.cbz");
        assertTrue(result.isPresent());
        assertEquals("Bleach Ch.005.cbz", result.get());
    }
}
