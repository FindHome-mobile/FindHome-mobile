package com.example.devmobile.api;

import com.example.devmobile.models.Annonce;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FavoriService {
    @POST("favoris/add")
    Call<JsonObject> addToFavorites(@Body JsonObject body);

    @DELETE("favoris/{clientId}/{annonceId}")
    Call<JsonObject> removeFromFavorites(@Path("clientId") String clientId, @Path("annonceId") String annonceId);

    @GET("favoris/client/{clientId}")
    Call<JsonObject> getClientFavorites(@Path("clientId") String clientId);

    @GET("favoris/check/{clientId}/{annonceId}")
    Call<JsonObject> checkIfFavorite(@Path("clientId") String clientId, @Path("annonceId") String annonceId);

    @GET("favoris/count/{clientId}")
    Call<JsonObject> getFavoritesCount(@Path("clientId") String clientId);
}

