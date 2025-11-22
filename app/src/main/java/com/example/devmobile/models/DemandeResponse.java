package com.example.devmobile.models;

import com.google.gson.annotations.SerializedName;

public class DemandeResponse {
    @SerializedName("message")
    private String message;
    @SerializedName("demande")
    private DemandeProprietaire demande;

    public String getMessage() { return message; }
    public DemandeProprietaire getDemande() { return demande; }
}

