package com.pjsmets.lintchangelog.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangelogLinter {
    private final File file;

    public ChangelogLinter(File file) {
        this.file = file;
    }

    public List<ValidationMessage> validate() {
        List<ValidationMessage> result = new ArrayList<>();

        try {
            List<String> fileLines = Files.readAllLines(file.toPath());
            result.addAll(generateDoubleVersionWarnings(fileLines));
            if (!fileLines.isEmpty())
                result.add(() -> "1.0.1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private List<ValidationMessage> generateDoubleVersionWarnings(List<String> fileLines) {
        // build a map of how many times a version occurs
        Map<String, Integer> versionCounts = new HashMap<>();
        Pattern versionHeaderLine = Pattern.compile("##\\s+\\[(?<version>.*)\\]\\s+-\\s+\\d{4}-\\d{2}-\\d{2}");

        for (String line : fileLines) {
            Matcher matcher = versionHeaderLine.matcher(line);
            if (matcher.matches()) {
                String versionNumber = matcher.group("version");
                Integer registeredOccurences = versionCounts.getOrDefault(versionNumber, 0);
                versionCounts.put(versionNumber, registeredOccurences + 1);
            }
        }

        // return versions that occur more than once
        ArrayList<ValidationMessage> doubleVersions = new ArrayList<>();
        for (String version : versionCounts.keySet()) {
            if (versionCounts.get(version) > 1)
                doubleVersions.add(() -> version);

        }
        return doubleVersions;
    }
}
