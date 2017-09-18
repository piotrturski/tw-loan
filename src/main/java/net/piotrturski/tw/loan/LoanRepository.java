package net.piotrturski.tw.loan;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.util.streamex.EntryStream;
import org.eclipse.collections.impl.factory.Maps;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.schedulers.Schedulers;

import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Repository
class LoanRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(100,
            new ThreadFactoryBuilder().setNameFormat("jdbc-pool-%d").build()));

    public LoanRepository(DataSource dataSource) {
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public Single<Boolean> addApplication(LoanApplicationDto loan,
                                          OffsetDateTime windowBegin,
                                          long maxPermittedApplications) {

        String sql = "insert into loan_application " +
                " values (default, :amount, :term, :personal_id, :country_code, :application_date," +
                " ((select count(1)from loan_application where " +
                "     country_code = :country_code and application_date >= :window_begin) < :max_app_per_window) and not" +
                "  exists (select 1 from blacklisted_id where personal_id = :personal_id)," +
                " :name, :surname) " +
                " returning accepted";

        return Single.fromCallable(() ->
                namedParameterJdbcTemplate.queryForObject(
                    sql,
                    EntryStream.of(
                            "amount", loan.loanApplicationRequest.amount,
                            "term", loan.loanApplicationRequest.term,
                            "personal_id", loan.loanApplicationRequest.personalId,
                            "country_code", loan.countryCode,
                            "application_date", loan.applicationDate,
                            "window_begin", windowBegin,
                            "name", loan.loanApplicationRequest.name,
                            "surname", loan.loanApplicationRequest.surname,
                            "max_app_per_window", maxPermittedApplications
                    ).toMap(),
                    boolean.class
        ));
    };

    public Observable<Map<String, Object>> approvedLoans(Long personalId) {
        return Observable.fromCallable(() -> namedParameterJdbcTemplate.queryForList(
                    "select * from loan_application where accepted " +
                        (personalId != null ? "and personal_id = :personal_id" : ""),
                    Maps.mutable.of("personal_id", personalId)))
                    // lack of simpler immutable map builder accepting nulls
                .flatMapIterable(x -> x)
                .subscribeOn(scheduler);
    }

}
