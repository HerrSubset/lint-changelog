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

    private final Path file;

    public ChangelogLinter(Path file) {
        this.file = file;
    }

    public List<ValidationMessage> validate() {
        List<ValidationMessage> result = new ArrayList<>();

        try {
            List<ChangelogLine> fileLines = Files.readAllLines(file).stream()
                    .map(ChangelogLine::new)
                    .collect(Collectors.toList());
            result.addAll(generateDoubleVersionWarnings(fileLines));
            result.addAll(generateGitMergeLeftoversWarnings(fileLines));
            result.addAll(generateWhiteLineWarnings(fileLines));
        } catch (IOException e) {
            result.add(() -> "File does not exist: " + file.toString());
        }

        return result;
    }

    private List<ValidationMessage> generateWhiteLineWarnings(List<ChangelogLine> fileLines) {
        ArrayList<ValidationMessage> validationMessages = new ArrayList<>();

        int lineNumber = 0;
        ChangelogLine previousLine = null;
        ChangelogLine twoLinesBack = null;
        boolean headerIsProcessed = false;
        for (ChangelogLine line : fileLines) {
            lineNumber++;
            headerIsProcessed = headerIsProcessed || line.toString().startsWith("##");
            if (!headerIsProcessed) continue;

            // white line before entry
            if (line.isEntry() && previousLine.isWhiteLine()) {
                validationMessages.add(unexpectedWhiteSpaceMessage(lineNumber - 1));

            // double white line
            } else if (line.isWhiteLine() && previousLine.isWhiteLine()) {
                validationMessages.add(unexpectedWhiteSpaceMessage(lineNumber));

            // no white line before version header
            } else if (line.isVersionHeader()
                    && previousLine != null
                    && !previousLine.isWhiteLine()) {
                validationMessages.add(expectedWhiteSpaceMessage(lineNumber));

            // no white line before version section header (except right after version header)
            } else if (line.isVersionSectionheader()
                    && previousLine != null && !previousLine.isWhiteLine()
                    && !previousLine.isVersionHeader()
                    && !previousLine.isGitLeftover()) {
                validationMessages.add(expectedWhiteSpaceMessage(lineNumber));

            // white line right after version header
            } else if (line.isVersionSectionheader()
                    && previousLine != null && previousLine.isWhiteLine()
                    && twoLinesBack != null && twoLinesBack.isVersionHeader()) {
                validationMessages.add(unexpectedWhiteSpaceMessage(lineNumber - 1));
            }
            twoLinesBack = previousLine;
            previousLine = line;
        }

        return validationMessages;
    }

    private ValidationMessage expectedWhiteSpaceMessage(int lineNumber) {
        return () -> "Line " + lineNumber + " should be preceded by white line";
    }

    private ValidationMessage unexpectedWhiteSpaceMessage(int lineNumber) {
        return () -> "Unexpected whitespace @ line " + lineNumber;
    }

    private List<ValidationMessage> generateGitMergeLeftoversWarnings(List<ChangelogLine> fileLines) {
        ArrayList<ValidationMessage> validationMessages = new ArrayList<>();

        int lineNumber = 1;
        for (ChangelogLine line : fileLines) {
            if (line.isGitLeftover()) {
                int errorLine = lineNumber;
                validationMessages.add(() -> "Found git merge leftover at line " + errorLine + ": '" + line + "'");
            }
            lineNumber++;
        }
        return validationMessages;
    }

    private List<ValidationMessage> generateDoubleVersionWarnings(List<ChangelogLine> fileLines) {
        // build a map of how many times a version occurs
        Map<String, List<Integer>> versionCounts = new HashMap<>();
        Pattern versionHeaderLine = Pattern.compile("##\\s+\\[(?<version>.*)\\]\\s+-\\s+\\d{4}-\\d{2}-\\d{2}");

        int lineNumber = 1;
        for (ChangelogLine line : fileLines) {
            Matcher matcher = versionHeaderLine.matcher(line.toString());
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
