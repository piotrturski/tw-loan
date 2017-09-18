package net.piotrturski.tw.loan;

import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Single;

import java.time.*;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Service
@FieldDefaults(makeFinal = true, level = PRIVATE)
class LoanService {

    Clock clock;
    CountryCodeResolver countryCodeResolver;
    LoanRepository loanRepository;
    Duration windowSize;
    long maxAllowedApplicationsPerWindow;

    LoanService(
            Clock clock,
            CountryCodeResolver countryCodeResolver,
            LoanRepository loanRepository,
            @Value("${loan.window-duration}") String windowSize,
            @Value("${loan.max-applications-per-window}") long maxAllowedApplicationsPerWindow) {
        this.clock = clock;
        this.countryCodeResolver = countryCodeResolver;
        this.loanRepository = loanRepository;
        this.windowSize = Duration.parse(windowSize);
        this.maxAllowedApplicationsPerWindow = maxAllowedApplicationsPerWindow;
    }

    Single<Boolean> applyForLoan(LoanController.LoanApplicationRequest loanApplicationRequest, String ip) {

        return countryCodeResolver.getCountryCode(ip)
                .flatMap(code -> loanRepository.addApplication(
                        LoanApplicationDto.builder()
                                .loanApplicationRequest(loanApplicationRequest)
                                .countryCode(code)
                                .applicationDate(OffsetDateTime.now(clock))
                                .build(),
                        OffsetDateTime.now(clock).minus(windowSize),
                        maxAllowedApplicationsPerWindow
                ));
    }

    public Observable<Map<String, Object>> listApprovedLoans(Long personalId) {
        return loanRepository.approvedLoans(personalId);
    }

}
