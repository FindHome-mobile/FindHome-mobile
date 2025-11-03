package com.example.devmobile.api;

import com.example.devmobile.models.AnnonceListResponse;
import com.example.devmobile.models.Annonce;

import retrofit2.Call;
import retrofit2.http.GET;
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
}
