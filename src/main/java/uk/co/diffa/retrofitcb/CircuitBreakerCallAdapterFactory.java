package uk.co.diffa.retrofitcb;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Creates {@link CallAdapter} that decorates a {@link Call} to provide integration with a
 * Javaslang {@link CircuitBreaker} using {@link RetrofitCircuitBreaker}
 */
public final class CircuitBreakerCallAdapterFactory extends CallAdapter.Factory {

    final CircuitBreaker circuitBreaker;

    public CircuitBreakerCallAdapterFactory(final CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Call.class) {
            return null;
        }

        final Type responseType = getCallResponseType(returnType);
        return new CallAdapter<Call<?>>() {
            @Override public Type responseType() {
                return responseType;
            }

            @Override public <R> Call<R> adapt(Call<R> call) {
                return RetrofitCircuitBreaker.decorateCall(circuitBreaker, call);
            }
        };
    }

    static Type getCallResponseType(Type returnType) {
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
        }
        return getParameterUpperBound(0, (ParameterizedType) returnType);
    }

}