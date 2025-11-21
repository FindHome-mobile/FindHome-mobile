package com.example.devmobile.models;

import com.google.gson.annotations.SerializedName;

public class DemandeProprietaire {
    @SerializedName("_id")
    private String id;
    @SerializedName("utilisateur")
    private User utilisateur;
    @SerializedName("statut")
    private String statut;
    @SerializedName("dateDemande")
    private String dateDemande;
    @SerializedName("dateTraitement")
    private String dateTraitement;
    @SerializedName("traitePar")
    private User traitePar;

    public String getId() { return id; }
    public User getUtilisateur() { return utilisateur; }
    public String getStatut() { return statut; }
    public String getDateDemande() { return dateDemande; }
    public String getDateTraitement() { return dateTraitement; }
    public User getTraitePar() { return traitePar; }
}

