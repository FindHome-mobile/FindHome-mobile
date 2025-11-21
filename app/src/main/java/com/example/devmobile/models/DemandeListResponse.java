package com.example.devmobile.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DemandeListResponse {
    @SerializedName("count")
    private int count;
    @SerializedName("demandes")
    private List<DemandeProprietaire> demandes;

    public int getCount() { return count; }
    public List<DemandeProprietaire> getDemandes() { return demandes; }
}

