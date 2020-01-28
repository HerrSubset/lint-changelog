package com.pjsmets.lintchangelog.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LinterTest {

    @Test
    public void emptyFileIsValid() throws IOException {
        File file = new File(getClass()
                .getClassLoader()
                .getResource("testfiles/01_empty_file.md")
                .getFile()
        );

        List<ValidationMessage> validationFailures = new ChangelogLinter(file).validate();

        assertTrue(validationFailures.isEmpty());
    }

    @Test
    public void doubleVersionEntriesAreReported() {
        File file = new File(getClass()
                .getClassLoader()
                .getResource("testfiles/02_double_version.md")
                .getFile()
        );

        List<ValidationMessage> validationFailures = new ChangelogLinter(file).validate();

        assertFalse(validationFailures.isEmpty());
        assertTrue(validationFailures.get(0).getMessage().contains("1.0.1"));
    }

    // TODO: test path to file that doesn't exist (change API to take path instead of File)
}
