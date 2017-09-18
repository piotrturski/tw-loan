package net.piotrturski.tw.infrastructure;

import com.google.common.collect.ImmutableMap;
import net.piotrturski.testegration.postgres.Postgres;
import org.junit.Before;
import org.junit.BeforeClass;

public class SpringIntegrationTest {

    @BeforeClass
    public static void beforeSpringContext() {prepareDatabaseForTest();}

    @Before
    public void beforeEachTest() { prepareDatabaseForTest(); }

    private static void prepareDatabaseForTest() {
        Postgres.docker("tw-loan-boot", "9.6.5-alpine")
                .postConfigure(runtime ->
                        ImmutableMap.of(
                                "spring.datasource.url", runtime.getConfig().getJdbcUrl(),
                                "spring.datasource.username", runtime.getConfig().user,
                                "spring.datasource.password", runtime.getConfig().passwd)
                                .forEach(System::setProperty))
                .run();
    }
}
