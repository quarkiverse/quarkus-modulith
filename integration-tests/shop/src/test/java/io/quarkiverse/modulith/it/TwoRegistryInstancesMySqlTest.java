package io.quarkiverse.modulith.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(MySqlProfile.class)
public class TwoRegistryInstancesMySqlTest extends TwoRegistryInstancesTest {
}
