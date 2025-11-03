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
import com.example.devmobile.models.User;
import com.google.gson.JsonObject;

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
    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private UtilisateurService utilisateurService;
    private String userId;
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

        utilisateurService = RetrofitClient.getInstance().getUtilisateurService();

        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", null);
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
    }

    private void loadUserFromBackend(String id) {
        utilisateurService.getUtilisateur(id).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User u = response.body();
                    tvName.setText((u.getNom() + " " + u.getPrenom()).trim());
                    tvEmail.setText(u.getEmail());
                    etNom.setText(u.getNom());
                    etPrenom.setText(u.getPrenom());
                    etEmail.setText(u.getEmail());
                    etLocation.setText(u.getLocation());
                    // etPhone, etFacebook non retournés dans User model actuel; laissés vides

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
                            .putString("USER_PHOTO_BASE64", u.getPhotoDeProfile())
                            .apply();
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
