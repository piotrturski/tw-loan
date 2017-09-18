package net.piotrturski.tw.loan;

import net.piotrturski.tw.infrastructure.StandaloneDbSetup;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import rx.Observable;
import rx.Single;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LoanRepositoryTest {

    DataSource dataSource = StandaloneDbSetup.prepareTestDataSource();
    LoanRepository loanRepository = new LoanRepository(dataSource);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    OffsetDateTime since = OffsetDateTime.now();
    OffsetDateTime appDate = since.plusSeconds(1);

    LoanApplicationDto loan = LoanApplicationDto.builder()
            .countryCode("PL")
            .applicationDate(appDate)
            .loanApplicationRequest(LoanController.LoanApplicationRequest.builder()
                    .amount(BigDecimal.TEN)
                    .personalId(7L)
                    .name("john")
                    .surname("doe")
                    .term(1L)
                    .build())
            .build();


    @Test
    public void should_accept_based_on_max_permitted_per_country_and_time() {

        Single<Boolean> accepted = loanRepository.addApplication(loan, since, 1);
        assertThat(accepted.toBlocking().value()).isTrue();

        accepted = loanRepository.addApplication(loan, since, 1);
        assertThat(accepted.toBlocking().value()).isFalse();

        accepted = loanRepository.addApplication(loan, since, 2);
        assertThat(accepted.toBlocking().value()).isFalse();

        accepted = loanRepository.addApplication(loan, since, 4);
        assertThat(accepted.toBlocking().value()).isTrue();

        accepted = loanRepository.addApplication(loan, appDate.plusSeconds(1), 1);
        assertThat(accepted.toBlocking().value()).isTrue();

        loan.countryCode = "US";
        accepted = loanRepository.addApplication(loan, since, 1);
        assertThat(accepted.toBlocking().value()).isTrue();
    }

    @Test
    public void should_accept_based_on_blacklisted_id() {
        final long personalId = loan.loanApplicationRequest.personalId;
        jdbcTemplate.execute("insert into blacklisted_id values (" + personalId +" )");

        Single<Boolean> accepted = loanRepository.addApplication(loan, since, 10);
        assertThat(accepted.toBlocking().value()).isFalse();
    }

    @Test
    public void should_return_list_of_accepted_applications() {

        loanRepository.addApplication(loan, since, 2).toBlocking().value();
        loan.loanApplicationRequest.personalId++;
        loanRepository.addApplication(loan, since, 2).toBlocking().value();

        Observable<Map<String, Object>> rows = loanRepository.approvedLoans(null);

        assertThat(rows.toBlocking().toIterable()).hasSize(2);
    }

    @Test
    public void should_return_list_of_accepted_applications_per_user() {

        loanRepository.addApplication(loan, since, 2).toBlocking().value();
        loan.loanApplicationRequest.personalId++;
        loanRepository.addApplication(loan, since, 2).toBlocking().value();;

        Observable<Map<String, Object>> rows =
                loanRepository.approvedLoans(loan.loanApplicationRequest.personalId);

        assertThat(rows.toBlocking().toIterable()).hasSize(1);
    }
}