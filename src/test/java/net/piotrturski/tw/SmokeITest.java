package net.piotrturski.tw;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.piotrturski.tw.infrastructure.SpringIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static net.piotrturski.tw.infrastructure.JsonUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@RunWith(SpringRunner.class)
public class SmokeITest extends SpringIntegrationTest {

    @Test
    public void should_return_accepted_loans() throws Exception {

        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        Response response = RestAssured.with()
                .contentType(ContentType.JSON)
                .body(toJson("{'term':1,'amount':25.43, 'personalId':7, 'name':'john', 'surname':'doe'}"))
                .post("/v1/apply-for-loan");

        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

        assert response.statusCode() == 200;
        assert response.path("accepted").equals(true);

        response = RestAssured.with().get("/v1/approved-loans");

        assert response.statusCode() == 200;
        response.body().print();

        JSONAssert.assertEquals(
                toJson("[{'id':1,'term':1,'amount':25.43, 'personal_id':7, 'name':'john', " +
                        " 'surname':'doe','country_code':'LV'}]"),
                response.body().asString(),
                false);


        OffsetDateTime date = OffsetDateTime.parse(response.path("[0].application_date"),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxx"));

        assertThat(date.getOffset()).isEqualTo(ZoneOffset.UTC);

        assertThat(date)
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @LocalServerPort
    public void setPort(int port) {
        RestAssured.port = port;
        RestAssured.config = config().redirect(redirectConfig().followRedirects(false));
    }
}