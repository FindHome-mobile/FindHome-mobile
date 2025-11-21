package com.example.devmobile.api;

import com.example.devmobile.models.AnnonceListResponse;
import com.example.devmobile.models.Annonce;

import com.google.gson.JsonObject;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AnnonceService {
    @GET("annonces")
    Call<AnnonceListResponse> getAnnonces(
            @Query("localisation") String localisation,
            @Query("prixMin") String prixMin,
            @Query("prixMax") String prixMax,
            @Query("typeBien") String typeBien,
            @Query("meublee") String meublee,
            @Query("statut") String statut
    );

    @GET("annonces/{id}")
    Call<Annonce> getAnnonce(@Path("id") String id);

    @Multipart
    @POST("annonces")
    Call<JsonObject> createAnnonce(
            @Part("titre") okhttp3.RequestBody titre,
            @Part("description") okhttp3.RequestBody description,
            @Part("localisation") okhttp3.RequestBody localisation,
            @Part("prix") okhttp3.RequestBody prix,
            @Part("nbPieces") okhttp3.RequestBody nbPieces,
            @Part("surface") okhttp3.RequestBody surface,
            @Part("typeBien") okhttp3.RequestBody typeBien,
            @Part("proprietaire") okhttp3.RequestBody proprietaire,
            @Part("meublee") okhttp3.RequestBody meublee,
            @Part("ascenseur") okhttp3.RequestBody ascenseur,
            @Part("parking") okhttp3.RequestBody parking,
            @Part("climatisation") okhttp3.RequestBody climatisation,
            @Part("chauffage") okhttp3.RequestBody chauffage,
            @Part("balcon") okhttp3.RequestBody balcon,
            @Part("jardin") okhttp3.RequestBody jardin,
            @Part("piscine") okhttp3.RequestBody piscine,
            @Part("etage") okhttp3.RequestBody etage,
            @Part("telephone") okhttp3.RequestBody telephone,
            @Part MultipartBody.Part... images
    );

    @GET("annonces/proprietaire/{proprietaireId}")
    Call<AnnonceListResponse> getAnnoncesByProprietaire(@Path("proprietaireId") String proprietaireId);
}
