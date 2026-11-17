package io.quarkiverse.modulith.it.moments;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(GermanWeeks.class)
public class GermanWeeksIT extends GermanWeeksTest {
}
