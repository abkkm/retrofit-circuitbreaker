package uk.co.diffa.retrofitcb;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;

/**
 * Tests the integration of the Retrofit HTTP client and JavaSlang-circuitbreaker.
 * Validates that connection timeouts will trip circuit breaking
 */
public class RetrofitCircuitBreakerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    private RetrofitService service;
    private final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(3)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();
    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);


    @Before
    public void setUp() {
        final long TIMEOUT = 300; // ms
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();

        this.service = new Retrofit.Builder()
                .addCallAdapterFactory(new CircuitBreakerCallAdapterFactory(circuitBreaker))
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl("http://localhost:8080/")
                .client(client)
                .build()
                .create(RetrofitService.class);
    }

    @Test
    public void decorateSuccessfulCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));

        service.greeting().execute();

        verify(1, getRequestedFor(urlPathEqualTo("/greeting")));
    }

    @Test
    public void decorateTimingOutCall() throws Exception {
        stubFor(get(urlPathEqualTo("/greeting"))
                .willReturn(aResponse()
                        .withFixedDelay(500)
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("hello world")));

        try {
            service.greeting().execute();
        } catch (Throwable t) {}

        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertEquals(1, metrics.getNumberOfFailedCalls());

        // Circuit breaker should still be closed, not hit open threshold
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        try {
        service.greeting().execute();
        } catch (Throwable t) {}

        try {
        service.greeting().execute();
        } catch (Throwable t) {}

        assertEquals(3, metrics.getNumberOfFailedCalls());
        // Circuit breaker should be OPEN, threshold met
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

}