package com.example.devmobile.api;

import com.example.devmobile.models.DemandeProprietaire;
import com.example.devmobile.models.DemandeListResponse;
import com.example.devmobile.models.DemandeResponse;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DemandeProprietaireService {
    @POST("demandes-proprietaire")
    Call<DemandeResponse> createDemande(@Body JsonObject body);

    @GET("demandes-proprietaire")
    Call<DemandeListResponse> getAllDemandes(@Query("statut") String statut);

    @GET("demandes-proprietaire/{demandeId}")
    Call<DemandeProprietaire> getDemande(@Path("demandeId") String demandeId);

    @GET("demandes-proprietaire/utilisateur/{utilisateurId}")
    Call<DemandeProprietaire> getDemandeByUtilisateur(@Path("utilisateurId") String utilisateurId);

    @POST("demandes-proprietaire/{demandeId}/approve")
    Call<DemandeProprietaire> approveDemande(@Path("demandeId") String demandeId, @Body JsonObject body);

    @POST("demandes-proprietaire/{demandeId}/reject")
    Call<DemandeProprietaire> rejectDemande(@Path("demandeId") String demandeId, @Body JsonObject body);
}

