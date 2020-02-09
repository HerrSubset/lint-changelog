package com.pjsmets.lintchangelog.core;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LintWhitespaceTest {


    /**
     * No white lines in between entries
     *
     * ### Added
     * - item 1
     * - item 2
     *               <--- not allowed
     * - item 3
     */
    @Test
    public void emptyLinesBetweenEntriesIsNotAllowed() {
        Path file = loadTestFile("testfiles/07_empty_line_between_entries.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains("17"), "Line number is not correct");
    }

    /**
     * No white lines in between entries
     *
     * ## [1.0.0] 2012-05-16
     *                        <--- not allowed
     * ### Added
     * - item 1
     * - item 2
     * - item 3
     */
    @Test
    public void emptyLinesAfterVersionHeadingsIsNotAllowed() {
        Path file = loadTestFile("testfiles/08_no_white_lines_after_header.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains("10"), "Line number is not correct");
    }

    /**
     * Nowhere in the file should two consecutive white lines
     * appear, except for the header.
     */
    @Test
    public void doubleWhiteLinesAreInvalid() {
        Path file = loadTestFile("testfiles/09_double_white_line.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains("35"), "Line number is not correct");
    }

    /**
     * Before the line marking a new section for a
     * release, a white line is expected.
     *
     * - some item
     *                        <--- white line expected
     * ## [1.0.0] 2020-02-03
     * ### Added
     * - ...
     */
    @Test
    public void versionHeaderShouldBePrecededByWhiteLine() {
        Path file = loadTestFile("testfiles/10_white_line_before_version_header.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains("67"), "Line number is not correct");
    }

    /**
     * Before the line marking a new section for a
     * release, a white line is expected.
     *
     * - some item
     *                        <--- white line expected
     * ### Fixed
     * - ...
     */
    @Test
    public void versionSectionheaderShouldBePrecededByWhiteLine() {
        Path file = loadTestFile("testfiles/11_white_line_before_version_section_header.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(2, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains("34"), "Line number is not correct");
        assertTrue(validationFailures.get(1).getMessage().contains("70"), "Line number is not correct");
    }


    /******************************************************
     * Utilities
     *****************************************************/
    private Path loadTestFile(String path) {
        try {
            return Paths.get(getClass()
                    .getClassLoader()
                    .getResource(path)
                    .toURI()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error trying to open test resource.", e);
        }
    }
}
