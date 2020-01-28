package com.pjsmets.lintchangelog.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChangelogLinter {
    public ChangelogLinter(File file) {
    }

    public List<ValidationMessage> validate() {
        return new ArrayList<>();
    }
}
