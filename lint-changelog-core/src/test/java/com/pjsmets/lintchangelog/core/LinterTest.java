package com.pjsmets.lintchangelog.core;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LinterTest {

    /**
     * An empty CHANGELOG is valid.
     */
    @Test
    public void emptyFileIsValid() throws URISyntaxException {
        Path file = loadTestFile("testfiles/01_empty_file.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertTrue(validationFailures.isEmpty());
    }

    /**
     * When a path is given that does not exist, return a validation
     * message which includes said path.
     */
    @Test
    public void testNonExistingFileMessage() {
        String nonExistingPath = "my/non/existing/file.md";
        Path nonExistingFile = Paths.get(nonExistingPath);
        List<ValidationMessage> validationFailures = new ChangelogLinter(nonExistingFile)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains(nonExistingPath));
    }

    /**
     * When duplicate versions exist in the file, report them and
     * make sure the version number that's used multiple times is
     * in the validation message.
     */
    @Test
    public void doubleVersionEntriesAreReported() throws URISyntaxException {
        Path file = loadTestFile("testfiles/02_double_version.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains("1.0.1"));
    }

    /**
     * When duplicate versions are found, also report on which lines
     * they occur, so the user can find them more easily.
     */
    @Test
    public void doubleVersionsGetLineNumbersInValidationMessage() throws URISyntaxException {
        Path file = loadTestFile("testfiles/03_triple_version.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains(" 10"));
        assertTrue(validationFailures.get(0).getMessage().contains(" 18"));
        assertTrue(validationFailures.get(0).getMessage().contains(" 36"));
    }

    /**
     * Detect the line that git prints to show where
     * the 'ours' section of a git merge conflict starts.
     *
     * Looks like: '<<<<<<< HEAD'
     */
    @Test
    public void detectOursSectionFromGitMerge() throws URISyntaxException {
        Path file = loadTestFile("testfiles/04_detect_git_ours_line.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains("<<<<<<< HEAD"));    // print the git merge leftover in error message
        assertTrue(validationFailures.get(0).getMessage().contains("7"));    // print the line number in error message
    }

    /**
     * Detect the separator line that git prints when
     * it encounters a merge conflict.
     *
     * Looks like: '======='
     */
    @Test
    public void detectSectionSeparatorFromGitMerge() throws URISyntaxException {
        Path file = loadTestFile("testfiles/05_detect_git_merge_section_separator.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains("======="));    // print the git merge leftover in error message
        assertTrue(validationFailures.get(0).getMessage().contains("8"));    // print the line number in error message
    }

    /**
     * Detect the line that git prints to show where
     * the 'theirs' section of a git merge conflict starts.
     *
     * Looks like: '>>>>>>> branch-2'
     */
    @Test
    public void detectTheirsSectionFromGitMerge() throws URISyntaxException {
        Path file = loadTestFile("testfiles/06_detect_git_theirs_line.md");
        List<ValidationMessage> validationFailures = new ChangelogLinter(file)
                .validate();

        assertEquals(1, validationFailures.size());
        assertTrue(validationFailures.get(0).getMessage().contains(">>>>>>> branch-2"));    // print the git merge leftover in error message
        assertTrue(validationFailures.get(0).getMessage().contains("19"));    // print the line number in error message
    }


    /******************************************************
     * Utilities
     *****************************************************/
    private Path loadTestFile(String path) throws URISyntaxException {
        return Paths.get(getClass()
                .getClassLoader()
                .getResource(path)
                .toURI()
        );
    }
}
