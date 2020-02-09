package com.pjsmets.lintchangelog.core;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

class ChangelogLine {

    private static final Pattern GIT_MERGE_OURS_SECTION_PATTERN = Pattern.compile("<<<<<<< .+");
    private static final Pattern GIT_MERGE_SECTION_SEPARATOR = Pattern.compile("=======");
    private static final Pattern GIT_MERGE_THEIRS_SECTION_PATTERN = Pattern.compile(">>>>>>> .+");

    private final String fileLine;

    public ChangelogLine(String fileLine) {
        checkNotNull(fileLine, "Cannot make a ChangelogLine from null");
        this.fileLine = fileLine;
    }

    public boolean isGitLeftover() {
        return GIT_MERGE_OURS_SECTION_PATTERN.matcher(fileLine).matches()
                || GIT_MERGE_SECTION_SEPARATOR.matcher(fileLine).matches()
                || GIT_MERGE_THEIRS_SECTION_PATTERN.matcher(fileLine).matches();
    }

    /**
     * return true if the line is:
     *
     * ## [1.0.0] 2041-02-26              <---- this line
     * ### Added
     * - ...
     */
    public boolean isVersionHeader() {
        return fileLine.startsWith("## ");
    }

    /**
     * return true if the line is:
     *
     * ## [1.0.0] 2041-02-26
     * ### Added               <---- this line
     * - ...
     */
    public boolean isVersionSectionheader() {
        return fileLine.startsWith("### ");
    }

    /**
     * return true if the line is:
     *
     * ## [1.0.0] 2041-02-26
     * ### Added
     * - ...                  <---- this line
     */
    public boolean isEntry() {
        return !isWhiteLine()
                && !fileLine.startsWith("#")
                && !fileLine.startsWith("[");
    }

    public boolean isWhiteLine() {
        return fileLine.trim().isEmpty();
    }

    @Override
    public String toString() {
        return fileLine;
    }
}
