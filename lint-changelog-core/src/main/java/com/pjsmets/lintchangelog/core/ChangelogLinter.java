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
            List<String> fileLines = Files.readAllLines(file);
            result.addAll(generateDoubleVersionWarnings(fileLines));
            result.addAll(generateGitMergeLeftoversWarnings(fileLines));
        } catch (IOException e) {
            result.add(() -> "File does not exist: " + file.toString());
        }

        return result;
    }

    private List<ValidationMessage> generateGitMergeLeftoversWarnings(List<String> fileLines) {
        ArrayList<ValidationMessage> validationMessages = new ArrayList<>();
        Pattern oursLinePattern = Pattern.compile("<<<<<<< .+");
        Pattern separatorLinePattern = Pattern.compile("=======");
        Pattern theirsLinePattern = Pattern.compile(">>>>>>> .+");

        int lineNumber = 1;
        for (String line : fileLines) {
            checkLineWithMatcher(line, oursLinePattern, lineNumber, validationMessages);
            checkLineWithMatcher(line, separatorLinePattern, lineNumber, validationMessages);
            checkLineWithMatcher(line, theirsLinePattern, lineNumber, validationMessages);
            lineNumber++;
        }
        return validationMessages;
    }

    private void checkLineWithMatcher(final String line,
                                      final Pattern invalidLinePattern,
                                      final int lineNumber,
                                      final List<ValidationMessage> validationMessages) {
        if (invalidLinePattern.matcher(line).matches())
            validationMessages.add(() -> "Found git merge leftover at line " + lineNumber + ": '" +line + "'");
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
