package net.piotrturski.tw.loan;

import com.googlecode.zohhak.api.TestWith;
import com.googlecode.zohhak.api.runners.ZohhakRunner;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Single;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

@RunWith(ZohhakRunner.class)
public class CountryCodeResolverITest {

    CountryCodeResolver countryCodeResolver = new CountryCodeResolver(
            "http://freegeoip.net/json/{ip}", "LV");

    @TestWith({
            "8.8.8.8,   US",
            "127.0.0.1, LV"
    })
    public void should_resolve_ip(String ip, String expectedCountryCode) throws Exception {
        Single<String> countryCode = countryCodeResolver.getCountryCode(ip);

        assertThat(countryCode.toBlocking().value()).isEqualTo(expectedCountryCode);
    }
}