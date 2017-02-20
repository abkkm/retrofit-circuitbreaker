# Retrofit Circuitbreaker  

[Retrofit](https://square.github.io/retrofit/) client circuit breaking using [Javaslang-circuitbreaker](https://github.com/robwin/javaslang-circuitbreaker)

```java
// Create a CircuitBreaker
private final CircuitBreaker.ofDefaults("testName");

// Create a retrofit instance with CircuitBreaker call adapter
Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(new CircuitBreakerCallAdapterFactory(circuitBreaker))
                .baseUrl("http://localhost:8080/")
                .build();
                
// Get an instance of your service with circuit breaking built in.
RetrofitService service = retrofit.create(RetrofitService.class);
```

By default, all exceptions and responses where `!Response.isSuccessful()` will be recorded as an error in the CircuitBreaker.

Customising what is considered a _successful_ response is possible like so:
```java
Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(new CircuitBreakerCallAdapterFactory(circuitBreaker, (r) -> r.code() < 500));
                .baseUrl("http://localhost:8080/")
                .build();

```
For more details on configuring a CircuitBreaker see the [Javaslang CircuitBreaker User Guide](http://robwin.github.io/javaslang-circuitbreaker/).
 
 

