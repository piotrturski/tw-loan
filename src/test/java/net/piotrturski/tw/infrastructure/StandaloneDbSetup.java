package net.piotrturski.tw.infrastructure;

import net.piotrturski.testegration.postgres.Postgres;
import net.piotrturski.testegration.postgres.PostgresConf;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;

public class StandaloneDbSetup {

    /** it will use your postgres or start one using docker and return DS to that DB */
    public static DataSource prepareTestDataSource() {
        return Postgres.docker("tw-loan-standalone", "9.6.5-alpine")
                .postConfigure(runtime -> {
                    PostgresConf cfg = runtime.getConfig();
                    SingleConnectionDataSource dataSource =
                            new SingleConnectionDataSource(cfg.getJdbcUrl(), cfg.user, cfg.passwd, true);
                    runtime.putShared("ds", dataSource);
                })
                .buildSchema(runtime -> {
                    DataSource ds = runtime.getShared("ds");
                    Flyway flyway = new Flyway();
                    flyway.setDataSource(ds);
                    flyway.migrate();
                })
                .closeConnector(runtime -> {
                    SingleConnectionDataSource ds = runtime.getShared("ds");
                    ds.destroy();
                })
                .run()
                .getShared("ds");
    }
}