package net.piotrturski.tw.loan;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rx.Scheduler;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executors;

@Service
class CountryCodeResolver {

    private final String urlPattern;
    private final String defaultCountry;

    private final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(100,
                new ThreadFactoryBuilder().setNameFormat("country-resolver-pool-%d").build()));

    public CountryCodeResolver(
            @Value("${ip-resolver.url-pattern}") String urlPattern,
            @Value("${ip-resolver.default-country}") String defaultCountry) {

        this.urlPattern = urlPattern;
        this.defaultCountry = defaultCountry.toUpperCase();
    }

    private static class IpDetails {
        public String country_code;
    }

    private Single<String> defaultCountryCode() {
        return Single.just(defaultCountry);
    }

    @HystrixCommand(defaultFallback = "defaultCountryCode")
    public Single<String> getCountryCode(String ip) {

        return Single.fromCallable(() ->
                new RestTemplate().getForObject(urlPattern, IpDetails.class, ip)
                        .country_code)
                .map(StringUtils::upperCase)
                .flatMap(code -> StringUtils.isEmpty(code) ? defaultCountryCode() : Single.just(code))
                .subscribeOn(scheduler);
    }
}
