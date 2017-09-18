package net.piotrturski.tw.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Builder
@NoArgsConstructor @AllArgsConstructor
class LoanApplicationDto {

    LoanController.LoanApplicationRequest loanApplicationRequest;
    String countryCode;
    OffsetDateTime applicationDate;
}
