package com.github.spotbugs;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatabaseDropMissingDetectorTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void testGoodCase() {
        Path path = Paths.get("target/test-classes", "com.github.spotbugs".replace('.', '/'), "DatabaseDropMissingGoodCase.class");
        BugCollection bugCollection = spotbugs.performAnalysis(path);
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("TEST_DB_DROP_MISSING").build();
        assertThat(bugCollection, containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void testBadCase() {
        Path path = Paths.get("target/test-classes", "com.github.spotbugs".replace('.', '/'), "DatabaseDropMissingBadCase.class");
        BugCollection bugCollection = spotbugs.performAnalysis(path);

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("TEST_DB_DROP_MISSING").build();
        assertThat(bugCollection, containsExactly(1, bugTypeMatcher));
    }
}