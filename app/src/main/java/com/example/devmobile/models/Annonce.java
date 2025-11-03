package com.example.devmobile.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Annonce {
    @SerializedName("_id")
    private String id;
    @SerializedName("titre")
    private String titre;
    @SerializedName("description")
    private String description;
    @SerializedName("localisation")
    private String localisation;
    @SerializedName("prix")
    private double prix;
    @SerializedName("images")
    private List<String> images;
    @SerializedName("imagesUrls")
    private List<String> imagesUrls;
    @SerializedName("proprietaireInfo")
    private ProprietaireInfo proprietaireInfo;

    public String getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getLocalisation() { return localisation; }
    public double getPrix() { return prix; }
    public List<String> getImages() { return images; }
    public List<String> getImagesUrls() { return imagesUrls; }
    public ProprietaireInfo getProprietaireInfo() { return proprietaireInfo; }

    public static class ProprietaireInfo {
        @SerializedName("nom")
        private String nom;
        @SerializedName("prenom")
        private String prenom;
        @SerializedName("email")
        private String email;
        @SerializedName("photo")
        private String photo;

        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        public String getEmail() { return email; }
        public String getPhoto() { return photo; }
    }
}
