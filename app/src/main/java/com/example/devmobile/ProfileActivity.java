package com.example.devmobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.api.UtilisateurService;
import com.example.devmobile.api.DemandeProprietaireService;
import com.example.devmobile.models.User;
import com.example.devmobile.models.DemandeProprietaire;
import com.example.devmobile.models.DemandeResponse;
import com.google.gson.JsonObject;

import androidx.annotation.NonNull;

import com.google.android.material.imageview.ShapeableImageView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.net.Uri;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private ShapeableImageView profileImage;
    private EditText etPrenom, etNom, etEmail, etPhone, etLocation, etFacebook;
    private View btnSave, btnCancel, btnChangePhoto, btnChangePassword;
    private com.google.android.material.button.MaterialButton btnDemandeProprietaire;
    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private android.view.View cardDemandeProprietaire;
    private android.widget.TextView tvDemandeStatus;
    private UtilisateurService utilisateurService;
    private DemandeProprietaireService demandeProprietaireService;
    private String userId;
    private String userType;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Configuration de la barre d'outils
        Toolbar toolbar = findViewById(R.id.toolbar_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mon Profil");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        profileImage = findViewById(R.id.profile_image);
        etPrenom = findViewById(R.id.et_prenom);
        etNom = findViewById(R.id.et_nom);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etLocation = findViewById(R.id.et_location);
        etFacebook = findViewById(R.id.et_facebook);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        btnChangePassword = findViewById(R.id.btn_change_password);
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        cardDemandeProprietaire = findViewById(R.id.card_demande_proprietaire);
        btnDemandeProprietaire = findViewById(R.id.btn_demande_proprietaire);
        tvDemandeStatus = findViewById(R.id.tv_demande_status);

        utilisateurService = RetrofitClient.getInstance().getUtilisateurService();
        demandeProprietaireService = RetrofitClient.getInstance().getDemandeProprietaireService();

        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", null);
        userType = prefs.getString("USER_TYPE", "client");
        String nom = prefs.getString("USER_NOM", "");
        String prenom = prefs.getString("USER_PRENOM", "");
        String email = prefs.getString("USER_EMAIL", "");
        String location = prefs.getString("USER_LOCATION", "");
        String photoBase64 = prefs.getString("USER_PHOTO_BASE64", null);
        if (!TextUtils.isEmpty(photoBase64)) {
            setImageFromBase64(photoBase64);
        }

        // Prefill from cache
        tvName.setText((nom + " " + prenom).trim());
        tvEmail.setText(email);
        etNom.setText(nom);
        etPrenom.setText(prenom);
        etEmail.setText(email);
        etLocation.setText(location);

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_LONG).show();
            return;
        }

        loadUserFromBackend(userId);

        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
        setupImagePicker();
        btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnDemandeProprietaire.setOnClickListener(v -> createDemandeProprietaire());
    }

    private void loadUserFromBackend(String id) {
        utilisateurService.getUtilisateur(id).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User u = response.body();
                    userType = u.getType();
                    tvName.setText((u.getNom() + " " + u.getPrenom()).trim());
                    tvEmail.setText(u.getEmail());
                    etNom.setText(u.getNom());
                    etPrenom.setText(u.getPrenom());
                    etEmail.setText(u.getEmail());
                    etLocation.setText(u.getLocation());
                    if (u.getNumTel() != null) etPhone.setText(u.getNumTel());
                    if (u.getFacebook() != null) etFacebook.setText(u.getFacebook());

                    // Photo: prefer base64 if provided; else ignore (could handle URL with an image loader)
                    if (!TextUtils.isEmpty(u.getPhotoDeProfile())) {
                        setImageFromBase64(u.getPhotoDeProfile());
                    }

                    // Update cache
                    SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("USER_NOM", u.getNom())
                            .putString("USER_PRENOM", u.getPrenom())
                            .putString("USER_EMAIL", u.getEmail())
                            .putString("USER_LOCATION", u.getLocation())
                            .putString("USER_TYPE", u.getType())
                            .putString("USER_PHOTO_BASE64", u.getPhotoDeProfile())
                            .apply();

                    // Gérer l'affichage de la demande de propriétaire
                    checkDemandeProprietaire();
                } else {
                    Toast.makeText(ProfileActivity.this, "Impossible de charger le profil", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveProfile() {
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String numTel = etPhone.getText().toString().trim();
        String facebook = etFacebook.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (TextUtils.isEmpty(nom) || TextUtils.isEmpty(prenom) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Nom, prénom et email sont requis", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("nom", nom);
        body.addProperty("prenom", prenom);
        body.addProperty("email", email);
        if (!TextUtils.isEmpty(numTel)) body.addProperty("numTel", numTel);
        if (!TextUtils.isEmpty(facebook)) body.addProperty("facebook", facebook);
        if (!TextUtils.isEmpty(location)) body.addProperty("location", location);

        utilisateurService.updateProfile(userId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Profil mis à jour", Toast.LENGTH_LONG).show();
                    // Mettre à jour cache minimal
                    SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("USER_NOM", nom)
                            .putString("USER_PRENOM", prenom)
                            .putString("USER_EMAIL", email)
                            .putString("USER_LOCATION", location)
                            .apply();
                    tvName.setText((nom + " " + prenom).trim());
                    tvEmail.setText(email);
                } else {
                    Toast.makeText(ProfileActivity.this, "Échec de la mise à jour", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    uploadPhoto(uri);
                } catch (Exception e) {
                    Toast.makeText(this, "Erreur image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void uploadPhoto(Uri uri) throws IOException {
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String numTel = etPhone.getText().toString().trim();
        String facebook = etFacebook.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        ContentResolver cr = getContentResolver();
        String mime = cr.getType(uri);
        if (mime == null) mime = "image/jpeg";
        byte[] bytes = readAllBytes(cr.openInputStream(uri));

        RequestBody rbNom = RequestBody.create(MediaType.parse("text/plain"), nom);
        RequestBody rbPrenom = RequestBody.create(MediaType.parse("text/plain"), prenom);
        RequestBody rbEmail = RequestBody.create(MediaType.parse("text/plain"), email);
        RequestBody rbNumTel = RequestBody.create(MediaType.parse("text/plain"), numTel);
        RequestBody rbFacebook = RequestBody.create(MediaType.parse("text/plain"), facebook);
        RequestBody rbLocation = RequestBody.create(MediaType.parse("text/plain"), location);
        RequestBody photoRB = RequestBody.create(MediaType.parse(mime), bytes);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo_de_profile", "profile.jpg", photoRB);

        utilisateurService.updateProfileWithPhoto(userId, rbNom, rbPrenom, rbEmail, rbNumTel, rbFacebook, rbLocation, photoPart)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Photo mise à jour", Toast.LENGTH_LONG).show();
                            // Recharger le profil pour récupérer le base64 et rafraîchir le cache
                            loadUserFromBackend(userId);
                        } else {
                            Toast.makeText(ProfileActivity.this, "Échec de l'upload", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(ProfileActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private void setImageFromBase64(String dataUrl) {
        try {
            // dataUrl may be in format: data:image/png;base64,XXXXX
            String base64 = dataUrl;
            int comma = dataUrl.indexOf(",");
            if (comma != -1) {
                base64 = dataUrl.substring(comma + 1);
            }
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            if (bmp != null) {
                profileImage.setImageBitmap(bmp);
            }
        } catch (Exception ignored) { }
    }

    private void changePassword() {
        String current = etCurrentPassword.getText().toString();
        String next = etNewPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();
        if (TextUtils.isEmpty(current) || TextUtils.isEmpty(next) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Tous les champs sont requis", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!next.equals(confirm)) {
            Toast.makeText(this, "Confirmation différente du nouveau mot de passe", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("currentPassword", current);
        body.addProperty("newPassword", next);

        utilisateurService.changePassword(userId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Mot de passe modifié", Toast.LENGTH_LONG).show();
                    etCurrentPassword.setText("");
                    etNewPassword.setText("");
                    etConfirmPassword.setText("");
                } else {
                    Toast.makeText(ProfileActivity.this, "Échec modification: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkDemandeProprietaire() {
        if ("client".equals(userType)) {
            cardDemandeProprietaire.setVisibility(android.view.View.VISIBLE);
            demandeProprietaireService.getDemandeByUtilisateur(userId).enqueue(new Callback<DemandeProprietaire>() {
                @Override
                public void onResponse(Call<DemandeProprietaire> call, Response<DemandeProprietaire> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        DemandeProprietaire demande = response.body();
                        String statut = demande.getStatut();
                        if ("en_attente".equals(statut)) {
                            tvDemandeStatus.setText("Votre demande est en attente d'approbation");
                            btnDemandeProprietaire.setEnabled(false);
                            btnDemandeProprietaire.setText("Demande en attente");
                        } else if ("approuvee".equals(statut)) {
                            tvDemandeStatus.setText("Votre demande a été approuvée ! Vous êtes maintenant propriétaire.");
                            btnDemandeProprietaire.setEnabled(false);
                            btnDemandeProprietaire.setText("Demande approuvée");
                        } else if ("rejetee".equals(statut)) {
                            tvDemandeStatus.setText("Votre demande a été rejetée. Vous pouvez faire une nouvelle demande.");
                            btnDemandeProprietaire.setEnabled(true);
                            btnDemandeProprietaire.setText("Faire une nouvelle demande");
                        }
                    } else {
                        // Pas de demande existante
                        tvDemandeStatus.setText("Faites une demande pour devenir propriétaire et publier des annonces");
                        btnDemandeProprietaire.setEnabled(true);
                        btnDemandeProprietaire.setText("Faire une demande");
                    }
                }

                @Override
                public void onFailure(Call<DemandeProprietaire> call, Throwable t) {
                    // Pas de demande existante ou erreur
                    tvDemandeStatus.setText("Faites une demande pour devenir propriétaire et publier des annonces");
                    btnDemandeProprietaire.setEnabled(true);
                    btnDemandeProprietaire.setText("Faire une demande");
                }
            });
        } else {
            cardDemandeProprietaire.setVisibility(android.view.View.GONE);
        }
    }

    private void createDemandeProprietaire() {
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        // Désactiver le bouton pour éviter les doubles clics
        btnDemandeProprietaire.setEnabled(false);
        btnDemandeProprietaire.setText("Création en cours...");

        android.util.Log.d("ProfileActivity", "Création demande pour utilisateur: " + userId);
        android.util.Log.d("ProfileActivity", "URL complète: " + RetrofitClient.getInstance().getBaseUrl() + "demandes-proprietaire");
        
        JsonObject body = new JsonObject();
        body.addProperty("utilisateurId", userId);
        android.util.Log.d("ProfileActivity", "Body: " + body.toString());

        demandeProprietaireService.createDemande(body).enqueue(new Callback<DemandeResponse>() {
            @Override
            public void onResponse(Call<DemandeResponse> call, Response<DemandeResponse> response) {
                android.util.Log.d("ProfileActivity", "Réponse demande: " + response.code());
                android.util.Log.d("ProfileActivity", "Headers: " + response.headers());
                
                if (response.isSuccessful() && response.body() != null) {
                    DemandeResponse demandeResponse = response.body();
                    String message = demandeResponse.getMessage() != null ? demandeResponse.getMessage() : "Demande créée avec succès !";
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
                    checkDemandeProprietaire();
                } else {
                    String errorMsg = "Erreur " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("ProfileActivity", "Erreur body complet: " + errorBody);
                            
                            // Essayer de parser le JSON
                            if (errorBody.contains("\"message\"")) {
                                int start = errorBody.indexOf("\"message\"");
                                if (start != -1) {
                                    int msgStart = errorBody.indexOf("\"", start + 10) + 1;
                                    int msgEnd = errorBody.indexOf("\"", msgStart);
                                    if (msgEnd > msgStart) {
                                        errorMsg = errorBody.substring(msgStart, msgEnd);
                                    }
                                }
                            } else if (errorBody.contains("message")) {
                                // Format alternatif
                                int start = errorBody.indexOf("message");
                                if (start != -1) {
                                    int msgStart = errorBody.indexOf(":", start) + 1;
                                    int msgEnd = errorBody.indexOf(",", msgStart);
                                    if (msgEnd == -1) msgEnd = errorBody.indexOf("}", msgStart);
                                    if (msgEnd > msgStart) {
                                        String msg = errorBody.substring(msgStart, msgEnd).trim();
                                        if (msg.startsWith("\"")) msg = msg.substring(1);
                                        if (msg.endsWith("\"")) msg = msg.substring(0, msg.length() - 1);
                                        errorMsg = msg;
                                    }
                                }
                            }
                            
                            if (response.code() == 404) {
                                errorMsg = "Endpoint non trouvé. Vérifiez que le backend est démarré sur le port 5000.";
                            } else if (response.code() == 400) {
                                // Erreur de validation - garder le message du serveur
                            } else {
                                errorMsg = "Erreur " + response.code() + ": " + (errorMsg.length() > 50 ? errorMsg.substring(0, 50) : errorMsg);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ProfileActivity", "Erreur parsing: " + e.getMessage(), e);
                        if (response.code() == 404) {
                            errorMsg = "Endpoint non trouvé (404). Vérifiez que le backend est démarré.";
                        }
                    }
                    Toast.makeText(ProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    btnDemandeProprietaire.setEnabled(true);
                    btnDemandeProprietaire.setText("Faire une demande");
                }
            }

            @Override
            public void onFailure(Call<DemandeResponse> call, Throwable t) {
                android.util.Log.e("ProfileActivity", "Erreur réseau complète", t);
                String errorMsg = "Erreur réseau";
                if (t.getMessage() != null) {
                    android.util.Log.e("ProfileActivity", "Message d'erreur: " + t.getMessage());
                    if (t.getMessage().contains("404") || t.getMessage().contains("Not Found")) {
                        errorMsg = "Endpoint non trouvé (404). Vérifiez que:\n1. Le backend est démarré\n2. L'URL est correcte: " + RetrofitClient.getInstance().getBaseUrl();
                    } else if (t.getMessage().contains("Failed to connect") || t.getMessage().contains("Unable to resolve host")) {
                        errorMsg = "Impossible de se connecter au serveur.\nVérifiez l'URL: " + RetrofitClient.getInstance().getBaseUrl();
                    } else {
                        errorMsg = "Erreur: " + t.getMessage();
                    }
                }
                Toast.makeText(ProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                btnDemandeProprietaire.setEnabled(true);
                btnDemandeProprietaire.setText("Faire une demande");
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
