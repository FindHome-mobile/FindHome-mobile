package com.example.devmobile.api;

import com.example.devmobile.models.User;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    @POST("utilisateurs/login")
    Call<User> loginUser(@Body JsonObject body);

    @POST("utilisateurs/register")
    Call<User> registerUser(@Body JsonObject body);
}
