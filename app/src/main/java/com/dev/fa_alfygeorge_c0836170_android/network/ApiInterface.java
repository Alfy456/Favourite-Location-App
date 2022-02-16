package com.dev.fa_alfygeorge_c0836170_android.network;

import com.dev.fa_alfygeorge_c0836170_android.model.Result;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiInterface {
    @GET("maps/api/distancematrix/json")
    Call<Result> getDistance(@Query("origins") String origin,
                             @Query("destinations") String destination,
                             @Query("key") String key);
}
