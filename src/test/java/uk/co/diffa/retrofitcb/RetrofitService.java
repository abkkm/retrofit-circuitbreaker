package uk.co.diffa.retrofitcb;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RetrofitService {

    @GET("greeting")
    Call<String> greeting();

}
