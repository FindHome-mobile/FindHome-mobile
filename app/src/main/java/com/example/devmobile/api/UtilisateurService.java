package com.example.devmobile.api;

import com.google.gson.JsonObject;
import com.example.devmobile.models.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.POST;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface UtilisateurService {
    @GET("utilisateurs/{utilisateurId}")
    Call<User> getUtilisateur(@Path("utilisateurId") String utilisateurId);

    @PUT("utilisateurs/{utilisateurId}/profile")
    Call<JsonObject> updateProfile(
            @Path("utilisateurId") String utilisateurId,
            @Body JsonObject body
    );

    @Multipart
    @PUT("utilisateurs/{utilisateurId}/profile")
    Call<JsonObject> updateProfileWithPhoto(
            @Path("utilisateurId") String utilisateurId,
            @Part("nom") RequestBody nom,
            @Part("prenom") RequestBody prenom,
            @Part("email") RequestBody email,
            @Part("numTel") RequestBody numTel,
            @Part("facebook") RequestBody facebook,
            @Part("location") RequestBody location,
            @Part MultipartBody.Part photo_de_profile
    );

    @PUT("utilisateurs/{utilisateurId}/password")
    Call<JsonObject> changePassword(
            @Path("utilisateurId") String utilisateurId,
            @Body JsonObject body
    );

    @POST("utilisateurs/{utilisateurId}/change-password")
    Call<JsonObject> changePasswordPost(
            @Path("utilisateurId") String utilisateurId,
            @Body JsonObject body
    );
}
