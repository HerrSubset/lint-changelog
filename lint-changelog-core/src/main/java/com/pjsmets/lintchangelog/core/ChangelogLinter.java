package com.pjsmets.lintchangelog.core;

import com.google.common.base.Joiner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChangelogLinter {

    private static final Pattern GIT_MERGE_OURS_SECTION_PATTERN = Pattern.compile("<<<<<<< .+");
    private static final Pattern GIT_MERGE_SECTION_SEPARATOR = Pattern.compile("=======");
    private static final Pattern GIT_MERGE_THEIRS_SECTION_PATTERN = Pattern.compile(">>>>>>> .+");


    private final Path file;

    public ChangelogLinter(Path file) {
        this.file = file;
    }

    public List<ValidationMessage> validate() {
        List<ValidationMessage> result = new ArrayList<>();

        try {
            List<String> fileLines = Files.readAllLines(file);
            result.addAll(generateDoubleVersionWarnings(fileLines));
            result.addAll(generateGitMergeLeftoversWarnings(fileLines));
            result.addAll(generateWhiteLineWarnings(fileLines));
        } catch (IOException e) {
            result.add(() -> "File does not exist: " + file.toString());
        }

        return result;
    }

    private List<ValidationMessage> generateWhiteLineWarnings(List<String> fileLines) {
        ArrayList<ValidationMessage> validationMessages = new ArrayList<>();

        int lineNumber = 0;
        String previousLine = null;
        boolean headerIsProcessed = false;
        for (String line : fileLines) {
            lineNumber++;
            headerIsProcessed = headerIsProcessed || line.startsWith("##");
            if (!headerIsProcessed) continue;

            if (previousLineIsInvalidWhiteLine(previousLine, line)
                    && isRegularEntry(line)
                    && previousLine.isEmpty()) {
                validationMessages.add(unexpectedWhiteSpaceMessage(lineNumber - 1));

            } else if (line.trim().isEmpty() && previousLine.isEmpty()) {
                validationMessages.add(unexpectedWhiteSpaceMessage(lineNumber));

            } else if (isVersionHeader(line) && previousLine != null && !previousLine.isEmpty()) {
                validationMessages.add(expectedWhiteSpaceMessage(lineNumber));

            } else if (isVersionSectionheader(line)
                    && previousLine != null
                    && !previousLine.isEmpty()
                    && !isVersionHeader(previousLine)
                    && !isGitMergeLeftover(previousLine)) {
                validationMessages.add(expectedWhiteSpaceMessage(lineNumber));

            }
            previousLine = line.trim();
        }

        return validationMessages;
    }

    private boolean isVersionSectionheader(String line) {
        return line != null && line.startsWith("### ");
    }

    private boolean isVersionHeader(String line) {
        return line != null && line.startsWith("## ");
    }

    private ValidationMessage expectedWhiteSpaceMessage(int lineNumber) {
        return () -> "Line " + lineNumber + " should be preceded by white line";
    }

    private ValidationMessage unexpectedWhiteSpaceMessage(int lineNumber) {
        return () -> "Unexpected whitespace @ line " + lineNumber;
    }

    private boolean previousLineIsInvalidWhiteLine(final String previousLine,
                                                   final String currentLine) {
        return isRegularEntry(currentLine) && previousLine.isEmpty();
    }

    private boolean isRegularEntry(String line) {
        return !line.trim().isEmpty()
                && !line.startsWith("#")
                && !line.startsWith("[");
    }

    private List<ValidationMessage> generateGitMergeLeftoversWarnings(List<String> fileLines) {
        ArrayList<ValidationMessage> validationMessages = new ArrayList<>();

        int lineNumber = 1;
        for (String line : fileLines) {
            if (isGitMergeLeftover(line)) {
                int errorLine = lineNumber;
                validationMessages.add(() -> "Found git merge leftover at line " + errorLine + ": '" + line + "'");
            }
            lineNumber++;
        }
        return validationMessages;
    }

    private boolean isGitMergeLeftover(String line)  {
        return GIT_MERGE_OURS_SECTION_PATTERN.matcher(line).matches()
                || GIT_MERGE_SECTION_SEPARATOR.matcher(line).matches()
                || GIT_MERGE_THEIRS_SECTION_PATTERN.matcher(line).matches();
    }

    private List<ValidationMessage> generateDoubleVersionWarnings(List<String> fileLines) {
        // build a map of how many times a version occurs
        Map<String, List<Integer>> versionCounts = new HashMap<>();
        Pattern versionHeaderLine = Pattern.compile("##\\s+\\[(?<version>.*)\\]\\s+-\\s+\\d{4}-\\d{2}-\\d{2}");

        int lineNumber = 1;
        for (String line : fileLines) {
            Matcher matcher = versionHeaderLine.matcher(line);
            if (matcher.matches()) {
                String versionNumber = matcher.group("version");
                List<Integer> registeredLineNumbers = versionCounts.getOrDefault(versionNumber, new ArrayList<>());
                registeredLineNumbers.add(lineNumber);
                versionCounts.put(versionNumber, registeredLineNumbers);
            }
            lineNumber++;
        }

        // return versions that occur more than once
        return versionCounts.keySet().stream()
                .filter(version -> versionCounts.get(version).size() > 1)
                .map(version -> createDuplicateVersionMessage(version, versionCounts.get(version)))
                .collect(Collectors.toList());
    }

    private ValidationMessage createDuplicateVersionMessage(String version, List<Integer> lineNumbers) {
        return () -> "Version " + version + " appeared multiple lines @ line "
                + Joiner.on(", ").join(lineNumbers);
    }
}
