package uk.co.diffa.retrofitcb;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.utils.CircuitBreakerUtils;
import io.github.robwin.metrics.StopWatch;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Decorates a Retrofit {@link Call} to inform a Javaslang {@link CircuitBreaker} when an exception is thrown
 * To mark non HTTP 200-300 responses as successful implement a custom Predicate<Response> to determine success.
 *
 * <code>
 *  RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, (r) -> r.code() < 500);
 * </code>
 *
 */
public interface RetrofitCircuitBreaker {

    /**
     * Decorate {@link Call}s allow {@link CircuitBreaker} functionality.
     * @param circuitBreaker {@link CircuitBreaker} to apply
     * @param call Call to decorate
     * @param responseSuccess determines whether the response should be considered an expected response
     * @param <T>
     * @return
     */
    static <T> Call<T> decorateCall(final CircuitBreaker circuitBreaker, final Call<T> call, final Predicate<Response> responseSuccess) {
        return new Call<T>() {
            @Override
            public Response<T> execute() throws IOException {
                CircuitBreakerUtils.isCallPermitted(circuitBreaker);
                final StopWatch stopWatch = StopWatch.start(circuitBreaker.getName());
                try {
                    final Response<T> response = call.execute();

                    if (responseSuccess.test(response)) {
                        circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration());
                    } else {
                        final Throwable throwable = new Throwable("Response error: HTTP " + response.code() + " - " + response.message());
                        circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                    }

                    return response;
                } catch (Throwable throwable) {
                    circuitBreaker.onError(stopWatch.stop().getProcessingDuration(), throwable);
                    throw throwable;
                }
            }

            @Override
            public void enqueue(Callback<T> callback) {
                call.enqueue(callback);
            }

            @Override
            public boolean isExecuted() {
                return call.isExecuted();
            }

            @Override
            public void cancel() {
                call.cancel();
            }

            @Override
            public boolean isCanceled() {
                return call.isCanceled();
            }

            @Override
            public Call<T> clone() {
                return decorateCall(circuitBreaker, call.clone());
            }

            @Override
            public Request request() {
                return call.request();
            }
        };
    }

    /**
     * Decorate {@link Call}s allow {@link CircuitBreaker} functionality.
     * Exceptions and !{@link Response#isSuccessful()} calls are marked as errors for the circuit breaker
     * @param circuitBreaker
     * @param call
     * @param <T>
     * @return
     */
    static <T> Call<T> decorateCall(final CircuitBreaker circuitBreaker, final Call<T> call) {
        return RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, (r) -> r.isSuccessful());
    }



}
