package com.realestate;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    @Test
    void domainAndApplicationKeepDependencyDirection() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/com/realestate"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String normalized = file.toString().replace('\\', '/');
                if (normalized.contains("/domain/")) {
                    for (String line : Files.readAllLines(file))
                        if (line.startsWith("import ")) {
                            assertThat(line)
                                    .as(file.toString())
                                    .matches(
                                            "import (java\\..*|com\\.realestate\\..*\\.domain\\..*);");
                        }
                }
                if (normalized.contains("/application/")) {
                    String source = Files.readString(file);
                    assertThat(source)
                            .as(file.toString())
                            .doesNotContain(
                                    ".infrastructure.",
                                    "jakarta.persistence.",
                                    "org.springframework.data.",
                                    "jakarta.servlet.",
                                    "software.amazon.",
                                    "org.springframework.security.");
                }
            }
        }
    }
}
