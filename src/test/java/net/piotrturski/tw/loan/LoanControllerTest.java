package net.piotrturski.tw.loan;

import com.googlecode.zohhak.api.*;
import com.googlecode.zohhak.api.runners.ZohhakRunner;
import net.piotrturski.tw.loan.LoanController.LoanApplicationRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.cloud.netflix.rx.SingleReturnValueHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rx.Observable;
import rx.Single;

import java.math.BigDecimal;

import static net.piotrturski.tw.infrastructure.JsonUtils.toJson;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(ZohhakRunner.class)
@Configure(separator = "\\|")
public class LoanControllerTest {

    final LoanService loanService = Mockito.mock(LoanService.class);
    final LoanController loanController = new LoanController(loanService);
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(loanController)
            .setCustomReturnValueHandlers(new SingleReturnValueHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .alwaysExpect(forwardedUrl(null))
            .alwaysDo(print())
            .build();


    @TestWith({
            "",
            "{}",
            "{",
            "{'term':1,'amount':25.433, 'personalId':7, 'name':'john', 'surname':'doe'}",
            "{'term':1,'amount':0, 'personalId':7, 'name':'john', 'surname':'doe'}",
            "{'term':1,'amount':0.00, 'personalId':7, 'name':'john', 'surname':'doe'}",
            "{'term':1,'amount':-25, 'personalId':7, 'name':'john', 'surname':'doe'}",
            "{         'amount':25.43, 'personalId':7, 'name':'john', 'surname':'doe'}",
            "{'term':1,                'personalId':7, 'name':'john', 'surname':'doe'}",
            "{'term':1,'amount':25.43,                 'name':'john', 'surname':'doe'}",
            "{'term':1,'amount':25.43, 'personalId':7,                'surname':'doe'}",
            "{'term':1,'amount':25.43, 'personalId':7, 'name':'john'                }",
    })
    public void should_reject_wrong_applicatio_request_content(String json) throws Exception {

        mockMvc.perform(post("/v1/apply-for-loan")
                .contentType(APPLICATION_JSON_UTF8)
                .content(toJson(json)))
                .andExpect(status().isBadRequest());

    }

    @TestWith({
            "{'term':1,'amount':25.43, 'personalId':7, 'name':'john', 'surname':'doe'}",
            "{'term':1,'amount':25.3, 'personalId':7, 'name':'john', 'surname':'doe'}",
            "{'term':1,'amount':25, 'personalId':7, 'name':'john', 'surname':'doe'}",
            "{'term':1,'amount':25, 'personalId':7, 'name':'', 'surname':'doe'}",
    })
    public void should_accept_correct_application_request_content(String content) throws Exception {

        when(loanService.applyForLoan(any(LoanApplicationRequest.class), anyString())).thenReturn(Single.just(true));

        MvcResult async = mockMvc.perform(post("/v1/apply-for-loan")
                .contentType(APPLICATION_JSON_UTF8)
                .content(toJson(content)))
                .andReturn();

        mockMvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson("{'accepted':true}")));
    }

    @Test
    public void should_pass_parameters_to_service() throws Exception {

        final String remoteIp = "123.456.78.90";

        final LoanApplicationRequest loanApplicationRequest = LoanApplicationRequest.builder()
                .term(1L)
                .amount(new BigDecimal("25.43"))
                .personalId(7L)
                .name("john")
                .surname("doe")
                .build();

        when(loanService.applyForLoan(loanApplicationRequest, remoteIp)).thenReturn(Single.just(false));

        MvcResult async = mockMvc.perform(post("/v1/apply-for-loan")
                .with(mockRequest -> {
                    mockRequest.setRemoteAddr(remoteIp);
                    return mockRequest;
                })
                .contentType(APPLICATION_JSON_UTF8)
                .content(toJson("{'term':1,'amount':25.43, 'personalId':7, 'name':'john', 'surname':'doe'}")))
                .andReturn();

        mockMvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson("{'accepted':false}")));

        verify(loanService).applyForLoan(loanApplicationRequest, remoteIp);
    }


    @TestWith({
            " /7 | 7",
            " /  | null",
            "    | null"
    })
    public void should_accept_request_for_loans(String urlSuffix, Long id) throws Exception {

        when(loanService.listApprovedLoans(id)).thenReturn(Observable.empty());

        MvcResult async = mockMvc.perform(get("/v1/approved-loans"+urlSuffix))
                                    .andReturn();

        mockMvc.perform(asyncDispatch(async))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

}