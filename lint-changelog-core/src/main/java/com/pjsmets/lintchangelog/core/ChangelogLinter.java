package com.pjsmets.lintchangelog.core;

import com.google.common.base.Joiner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        } catch (IOException e) {
            result.add(() -> "File does not exist: " + file.toString());
        }

        return result;
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
        ArrayList<ValidationMessage> doubleVersions = new ArrayList<>();
        for (String version : versionCounts.keySet()) {
            if (versionCounts.get(version).size() > 1)
                doubleVersions.add(createDuplicateVersionMessage(version, versionCounts.get(version)));

        }
        return doubleVersions;
    }

    private ValidationMessage createDuplicateVersionMessage(String version, List<Integer> lineNumbers) {
        return () -> "Version " + version + " appeared multiple lines @ line "
                + Joiner.on(", ").join(lineNumbers);
    }
}
