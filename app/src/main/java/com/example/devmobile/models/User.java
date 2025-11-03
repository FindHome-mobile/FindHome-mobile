package com.example.devmobile.models;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle de données pour l'utilisateur, adapté aux réponses du backend.
 */
public class User {

    @SerializedName("id")
    private String id;
    @SerializedName("nom")
    private String nom;
    @SerializedName("prenom")
    private String prenom;
    @SerializedName("email")
    private String email;
    @SerializedName("type")
    private String type;
    @SerializedName("photo_de_profile")
    private String photoDeProfile;
    @SerializedName("photo_url")
    private String photoUrl;
    @SerializedName("location")
    private String location;

    // Constructeur
    public User(String id, String nom, String prenom, String email, String type, String photoDeProfile, String photoUrl, String location) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.type = type;
        this.photoDeProfile = photoDeProfile;
        this.photoUrl = photoUrl;
        this.location = location;
    }

    // Getters et Setters (nécessaires pour Gson/Retrofit)
    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getType() { return type; }
    public String getPhotoDeProfile() { return photoDeProfile; }
    public String getPhotoUrl() { return photoUrl; }
    public String getLocation() { return location; }
}
