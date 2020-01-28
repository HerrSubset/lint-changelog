package com.pjsmets.lintchangelog.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
}
