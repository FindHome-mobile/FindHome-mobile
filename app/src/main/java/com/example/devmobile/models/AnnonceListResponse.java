package com.example.devmobile.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AnnonceListResponse {
    @SerializedName("count")
    private int count;
    @SerializedName("annonces")
    private List<Annonce> annonces;

    public int getCount() { return count; }
    public List<Annonce> getAnnonces() { return annonces; }
}
