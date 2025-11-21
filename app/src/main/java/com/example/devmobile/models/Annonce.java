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
    @SerializedName("telephone")
    private String telephone;
    @SerializedName("facebook")
    private String facebook;
    @SerializedName("nbPieces")
    private int nbPieces;
    @SerializedName("surface")
    private double surface;
    @SerializedName("typeBien")
    private String typeBien;
    @SerializedName("meublee")
    private boolean meublee;
    @SerializedName("statut")
    private String statut;

    public String getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public String getLocalisation() { return localisation; }
    public double getPrix() { return prix; }
    public List<String> getImages() { return images; }
    public List<String> getImagesUrls() { return imagesUrls; }
    public ProprietaireInfo getProprietaireInfo() { return proprietaireInfo; }
    public String getTelephone() { return telephone; }
    public String getFacebook() { return facebook; }
    public int getNbPieces() { return nbPieces; }
    public double getSurface() { return surface; }
    public String getTypeBien() { return typeBien; }
    public boolean isMeublee() { return meublee; }
    public String getStatut() { return statut; }

    public static class ProprietaireInfo {
        @SerializedName("nom")
        private String nom;
        @SerializedName("prenom")
        private String prenom;
        @SerializedName("email")
        private String email;
        @SerializedName("photo")
        private String photo;
        @SerializedName("numTel")
        private String numTel;

        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        public String getEmail() { return email; }
        public String getPhoto() { return photo; }
        public String getNumTel() { return numTel; }
    }
}
