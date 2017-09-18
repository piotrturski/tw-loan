package net.piotrturski.tw.loan;

import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/")
@RequiredArgsConstructor
class LoanController {

    private final LoanService loanService;

    @Builder @Data @NoArgsConstructor @AllArgsConstructor
    static class LoanApplicationRequest {
        @NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer = 15, fraction = 2) BigDecimal amount;
        @NotNull Long term, personalId;
        @NotNull String name, surname;
    }

    @PostMapping("apply-for-loan")
    @HystrixCommand
    public Object applyForLoan(
            @Valid @RequestBody LoanApplicationRequest loanApplicationRequest,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();

        return loanService.applyForLoan(loanApplicationRequest, ip)
                .map(accepted -> ImmutableMap.of("accepted", accepted));
    }

    @GetMapping({"approved-loans/{personalId}", "approved-loans"})
    @HystrixCommand
    public Object listApprovedLoans(@PathVariable(required = false) Long personalId) {
        // in case you prefer SSE instead of a json list:
//        return RxResponse.sse(loanService.listApprovedLoans(personalId.orElse(null)));
        return loanService.listApprovedLoans(personalId).toList().toSingle();
    }

}
