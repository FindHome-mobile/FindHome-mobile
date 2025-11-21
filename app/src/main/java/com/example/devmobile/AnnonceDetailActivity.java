package com.example.devmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import com.example.devmobile.api.AnnonceService;
import com.example.devmobile.api.FavoriService;
import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.models.Annonce;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnnonceDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvDescription, tvLocation, tvPrix, tvNbPieces, tvSurface, tvTypeBien, tvMeublee;
    private TextView tvProprietaireName, tvProprietaireEmail, tvTelephone;
    private ImageView ivImage;
    private ProgressBar progressBar;
    private View btnCall, btnEmail, btnFavorite;
    private AnnonceService annonceService;
    private FavoriService favoriService;
    private String annonceId;
    private String clientId;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annonce_detail);

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Détails de l'annonce");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        annonceId = getIntent().getStringExtra("annonceId");
        if (TextUtils.isEmpty(annonceId)) {
            Toast.makeText(this, "Annonce invalide", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        clientId = prefs.getString("USER_ID", null);

        initViews();
        annonceService = RetrofitClient.getInstance().getAnnonceService();
        favoriService = RetrofitClient.getInstance().getFavoriService();

        loadAnnonce();
        checkFavorite();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvDescription = findViewById(R.id.tv_description);
        tvLocation = findViewById(R.id.tv_location);
        tvPrix = findViewById(R.id.tv_prix);
        tvNbPieces = findViewById(R.id.tv_nb_pieces);
        tvSurface = findViewById(R.id.tv_surface);
        tvTypeBien = findViewById(R.id.tv_type_bien);
        tvMeublee = findViewById(R.id.tv_meublee);
        tvProprietaireName = findViewById(R.id.tv_proprietaire_name);
        tvProprietaireEmail = findViewById(R.id.tv_proprietaire_email);
        tvTelephone = findViewById(R.id.tv_telephone);
        ivImage = findViewById(R.id.iv_image);
        progressBar = findViewById(R.id.progress);
        btnCall = findViewById(R.id.btn_call);
        btnEmail = findViewById(R.id.btn_email);
        btnFavorite = findViewById(R.id.btn_favorite);

        btnCall.setOnClickListener(v -> makePhoneCall());
        btnEmail.setOnClickListener(v -> sendEmail());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void loadAnnonce() {
        progressBar.setVisibility(View.VISIBLE);
        annonceService.getAnnonce(annonceId).enqueue(new Callback<Annonce>() {
            @Override
            public void onResponse(@NonNull Call<Annonce> call, @NonNull Response<Annonce> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    displayAnnonce(response.body());
                } else {
                    Toast.makeText(AnnonceDetailActivity.this, "Erreur: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Annonce> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AnnonceDetailActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAnnonce(Annonce annonce) {
        tvTitle.setText(annonce.getTitre());
        tvDescription.setText(annonce.getDescription());
        tvLocation.setText(annonce.getLocalisation());
        tvPrix.setText(String.format("%.0f DT", annonce.getPrix()));
        tvNbPieces.setText(String.valueOf(annonce.getNbPieces()) + " pièces");
        tvSurface.setText(String.format("%.0f m²", annonce.getSurface()));
        tvTypeBien.setText(annonce.getTypeBien());
        tvMeublee.setText(annonce.isMeublee() ? "Meublé" : "Non meublé");

        if (annonce.getProprietaireInfo() != null) {
            String name = annonce.getProprietaireInfo().getNom() + " " + annonce.getProprietaireInfo().getPrenom();
            tvProprietaireName.setText(name);
            tvProprietaireEmail.setText(annonce.getProprietaireInfo().getEmail());
        }

        String phone = annonce.getTelephone();
        if (TextUtils.isEmpty(phone) && annonce.getProprietaireInfo() != null) {
            phone = annonce.getProprietaireInfo().getNumTel();
        }
        tvTelephone.setText(phone);

        // Charger la première image si disponible
        if (annonce.getImages() != null && !annonce.getImages().isEmpty()) {
            String imageData = annonce.getImages().get(0);
            if (imageData.startsWith("data:image")) {
                loadBase64Image(imageData);
            }
        }
    }

    private void loadBase64Image(String base64Data) {
        try {
            String base64 = base64Data;
            int comma = base64Data.indexOf(",");
            if (comma != -1) {
                base64 = base64Data.substring(comma + 1);
            }
            byte[] decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            if (bmp != null) {
                ivImage.setImageBitmap(bmp);
            }
        } catch (Exception e) {
            // Ignorer les erreurs d'image
        }
    }

    private void makePhoneCall() {
        String phone = tvTelephone.getText().toString();
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Numéro de téléphone non disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 100);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private void sendEmail() {
        String email = tvProprietaireEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email non disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Demande d'information - " + tvTitle.getText().toString());
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Aucune application email disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkFavorite() {
        if (TextUtils.isEmpty(clientId)) return;

        favoriService.checkIfFavorite(clientId, annonceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isFavorite = response.body().has("isFavorite") && response.body().get("isFavorite").getAsBoolean();
                    updateFavoriteButton();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                // Ignorer les erreurs
            }
        });
    }

    private void toggleFavorite() {
        if (TextUtils.isEmpty(clientId)) {
            Toast.makeText(this, "Vous devez être connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFavorite) {
            removeFavorite();
        } else {
            addFavorite();
        }
    }

    private void addFavorite() {
        JsonObject body = new JsonObject();
        body.addProperty("clientId", clientId);
        body.addProperty("annonceId", annonceId);

        favoriService.addToFavorites(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    isFavorite = true;
                    updateFavoriteButton();
                    Toast.makeText(AnnonceDetailActivity.this, "Ajouté aux favoris", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AnnonceDetailActivity.this, "Erreur: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(AnnonceDetailActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFavorite() {
        favoriService.removeFromFavorites(clientId, annonceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    isFavorite = false;
                    updateFavoriteButton();
                    Toast.makeText(AnnonceDetailActivity.this, "Retiré des favoris", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AnnonceDetailActivity.this, "Erreur: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(AnnonceDetailActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFavoriteButton() {
        if (btnFavorite instanceof android.widget.ImageButton) {
            ((android.widget.ImageButton) btnFavorite).setImageResource(
                isFavorite ? android.R.drawable.star_big_on : android.R.drawable.star_big_off
            );
        }
    }
}

