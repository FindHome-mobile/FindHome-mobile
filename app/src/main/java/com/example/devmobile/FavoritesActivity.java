package com.example.devmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.devmobile.api.FavoriService;
import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.models.Annonce;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private RecyclerView rvFavorites;
    private ProgressBar progressFavorites;
    private LinearLayout tvEmptyFavorites;
    private FavoritesAdapter favoritesAdapter;
    private FavoriService favoriService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialiser le contexte pour Retrofit
        RetrofitClient.setContext(this);

        // Vérifier si l'utilisateur est connecté
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", null);
        if (userId == null || userId.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        try {
            setContentView(R.layout.activity_favorites);

            // Configurer la barre d'outils
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }

            // Configurer le menu de navigation
            drawerLayout = findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            // Initialiser les vues
            rvFavorites = findViewById(R.id.rv_favorites);
            progressFavorites = findViewById(R.id.progress_favorites);
            tvEmptyFavorites = findViewById(R.id.tv_empty_favorites);

            if (rvFavorites == null) {
                Log.e("FavoritesActivity", "RecyclerView non trouvé");
                Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Configurer le RecyclerView
            rvFavorites.setLayoutManager(new LinearLayoutManager(this));
            favoritesAdapter = new FavoritesAdapter();
            rvFavorites.setAdapter(favoritesAdapter);

            // Ajouter un décorateur d'espacement
            try {
                androidx.recyclerview.widget.DividerItemDecoration dividerItemDecoration =
                    new androidx.recyclerview.widget.DividerItemDecoration(rvFavorites.getContext(),
                        LinearLayoutManager.VERTICAL);
                dividerItemDecoration.setDrawable(getResources().getDrawable(android.R.color.transparent));
                rvFavorites.addItemDecoration(dividerItemDecoration);
            } catch (Exception e) {
                Log.w("FavoritesActivity", "Erreur ajout décorateur", e);
            }

            // Initialiser le service
            favoriService = RetrofitClient.getInstance().getFavoriService();

            // Afficher/masquer les options selon le type d'utilisateur
            String userType = prefs.getString("USER_TYPE", "client");
            MenuItem myAnnoncesItem = navigationView.getMenu().findItem(R.id.nav_my_annonces);
            MenuItem createAnnonceItem = navigationView.getMenu().findItem(R.id.nav_create_annonce);
            MenuItem adminItem = navigationView.getMenu().findItem(R.id.nav_admin);

            if ("proprietaire".equals(userType)) {
                myAnnoncesItem.setVisible(true);
                createAnnonceItem.setVisible(true);
            }
            if ("admin".equals(userType)) {
                adminItem.setVisible(true);
            }

            // Définir l'élément Favoris comme sélectionné par défaut
            if (savedInstanceState == null) {
                navigationView.setCheckedItem(R.id.nav_favorites);
            }

            // Charger les favoris
            loadFavorites();

        } catch (Exception e) {
            Log.e("FavoritesActivity", "Erreur critique dans onCreate", e);
            Toast.makeText(this, "Erreur au démarrage: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir les favoris quand on revient sur cette activité
        loadFavorites();
    }

    private void loadFavorites() {
        progressFavorites.setVisibility(View.VISIBLE);
        tvEmptyFavorites.setVisibility(View.GONE);
        rvFavorites.setVisibility(View.GONE);

        Log.d("FavoritesActivity", "Chargement des favoris pour userId: " + userId);

        if (userId == null || userId.isEmpty()) {
            Log.e("FavoritesActivity", "userId est null ou vide !");
            tvEmptyFavorites.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
            progressFavorites.setVisibility(View.GONE);
            Toast.makeText(this, "Erreur: Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            return;
        }

        favoriService.getClientFavorites(userId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressFavorites.setVisibility(View.GONE);
                Log.d("FavoritesActivity", "Réponse favoris: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Debug: Afficher la réponse complète
                        String jsonResponse = response.body().toString();
                        Log.d("FavoritesActivity", "Réponse JSON complète: " + jsonResponse);

                        // Parser la réponse JSON pour extraire les annonces
                        List<Annonce> favorites = new ArrayList<>();

                        // Vérifier le type de la réponse
                        Log.d("FavoritesActivity", "Type de réponse: " + response.body().getClass().getSimpleName());

                        // Essayer différentes structures de réponse possibles
                        com.google.gson.JsonElement dataElement = null;
                        String structureType = "unknown";

                        // Structure 1: { "annonces": [...] }
                        if (response.body().has("annonces")) {
                            Log.d("FavoritesActivity", "Structure détectée: annonces");
                            if (response.body().get("annonces").isJsonArray()) {
                                dataElement = response.body().get("annonces");
                                structureType = "annonces";
                            } else {
                                Log.w("FavoritesActivity", "annonces n'est pas un tableau");
                            }
                        }
                        // Structure 2: { "data": [...] }
                        else if (response.body().has("data")) {
                            Log.d("FavoritesActivity", "Structure détectée: data");
                            if (response.body().get("data").isJsonArray()) {
                                dataElement = response.body().get("data");
                                structureType = "data";
                            } else {
                                Log.w("FavoritesActivity", "data n'est pas un tableau");
                            }
                        }
                        // Structure 3: { "favoris": [...] }
                        else if (response.body().has("favoris")) {
                            Log.d("FavoritesActivity", "Structure détectée: favoris");
                            if (response.body().get("favoris").isJsonArray()) {
                                dataElement = response.body().get("favoris");
                                structureType = "favoris";
                            } else {
                                Log.w("FavoritesActivity", "favoris n'est pas un tableau");
                            }
                        }
                        // Structure 4: La réponse est directement un tableau
                        else if (response.body().isJsonArray()) {
                            Log.d("FavoritesActivity", "Structure détectée: tableau direct");
                            dataElement = response.body();
                            structureType = "array";
                        }
                        // Structure 5: Vérifier toutes les propriétés disponibles
                        else {
                            Log.d("FavoritesActivity", "Structure inconnue, propriétés disponibles:");
                            for (String key : response.body().keySet()) {
                                Log.d("FavoritesActivity", "Propriété: " + key + " = " + response.body().get(key));
                            }

                            // Essayer de trouver un tableau dans n'importe quelle propriété
                            for (String key : response.body().keySet()) {
                                com.google.gson.JsonElement element = response.body().get(key);
                                if (element.isJsonArray() && element.getAsJsonArray().size() > 0) {
                                    Log.d("FavoritesActivity", "Utilisation de la propriété: " + key);
                                    dataElement = element;
                                    structureType = "property:" + key;
                                    break;
                                }
                            }
                        }

                        Log.d("FavoritesActivity", "Structure finale utilisée: " + structureType);

                        if (dataElement != null && dataElement.isJsonArray()) {
                            com.google.gson.JsonArray annoncesArray = dataElement.getAsJsonArray();
                            Log.d("FavoritesActivity", "Nombre d'éléments dans le tableau: " + annoncesArray.size());

                            for (int i = 0; i < annoncesArray.size(); i++) {
                                com.google.gson.JsonElement element = annoncesArray.get(i);
                                Log.d("FavoritesActivity", "Élément " + i + " type: " + element.getClass().getSimpleName());

                                // Vérifier si c'est un objet annonce direct ou un objet avec propriété annonce
                                com.google.gson.JsonObject annonceObj = null;

                                if (element.isJsonObject()) {
                                    annonceObj = element.getAsJsonObject();
                                    Log.d("FavoritesActivity", "Objet annonce trouvé, propriétés: " + annonceObj.keySet());

                                    // Si l'élément a une propriété "annonce", l'utiliser
                                    if (annonceObj.has("annonce") && annonceObj.get("annonce").isJsonObject()) {
                                        Log.d("FavoritesActivity", "Utilisation de la propriété annonce imbriquée");
                                        annonceObj = annonceObj.get("annonce").getAsJsonObject();
                                    }
                                } else if (element.isJsonPrimitive()) {
                                    Log.w("FavoritesActivity", "Élément " + i + " est une primitive: " + element.getAsString());
                                    continue;
                                }

                                if (annonceObj != null) {
                                    Log.d("FavoritesActivity", "Tentative de parsing de l'annonce " + i);
                                    Annonce annonce = parseAnnonceFromJson(annonceObj);
                                    if (annonce != null && annonce.getId() != null) {
                                        favorites.add(annonce);
                                        Log.d("FavoritesActivity", "Annonce ajoutée avec succès: " + annonce.getTitre() + " (ID: " + annonce.getId() + ")");
                                    } else {
                                        Log.w("FavoritesActivity", "Impossible de parser l'annonce à l'index " + i + " - annonce null ou ID null");
                                    }
                                } else {
                                    Log.w("FavoritesActivity", "annonceObj est null pour l'index " + i);
                                }
                            }
                        } else {
                            Log.w("FavoritesActivity", "Aucun tableau trouvé dans la réponse");
                        }

                        Log.d("FavoritesActivity", "Nombre de favoris parsés avec succès: " + favorites.size());
                        if (favorites.isEmpty()) {
                            tvEmptyFavorites.setVisibility(View.VISIBLE);
                            rvFavorites.setVisibility(View.GONE);
                            Log.d("FavoritesActivity", "Aucun favori trouvé - affichage du message vide");

                            // Afficher un toast pour informer l'utilisateur
                            runOnUiThread(() -> {
                                Toast.makeText(FavoritesActivity.this, "Aucun favori trouvé. Réponse: " + jsonResponse.substring(0, Math.min(100, jsonResponse.length())), Toast.LENGTH_LONG).show();
                            });
                        } else {
                            tvEmptyFavorites.setVisibility(View.GONE);
                            rvFavorites.setVisibility(View.VISIBLE);
                            favoritesAdapter.setItems(favorites);
                            Log.d("FavoritesActivity", "Favoris affichés: " + favorites.size());

                            runOnUiThread(() -> {
                                Toast.makeText(FavoritesActivity.this, favorites.size() + " favori(s) chargé(s)", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e("FavoritesActivity", "Erreur parsing favoris", e);
                        Log.e("FavoritesActivity", "Exception détaillée", e);
                        tvEmptyFavorites.setVisibility(View.VISIBLE);
                        rvFavorites.setVisibility(View.GONE);
                        Toast.makeText(FavoritesActivity.this, "Erreur de traitement des données: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("FavoritesActivity", "Réponse non réussie: " + response.code() + " - " + response.message());
                    tvEmptyFavorites.setVisibility(View.VISIBLE);
                    rvFavorites.setVisibility(View.GONE);

                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("FavoritesActivity", "Corps de l'erreur: " + errorBody);
                        } catch (Exception e) {
                            Log.e("FavoritesActivity", "Impossible de lire le corps de l'erreur", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressFavorites.setVisibility(View.GONE);
                tvEmptyFavorites.setVisibility(View.VISIBLE);
                rvFavorites.setVisibility(View.GONE);
                Log.e("FavoritesActivity", "Erreur réseau: " + t.getMessage(), t);
                Toast.makeText(FavoritesActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Annonce parseAnnonceFromJson(com.google.gson.JsonObject jsonObject) {
        try {
            // Utiliser Gson pour convertir le JsonObject en Annonce
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(jsonObject, Annonce.class);
        } catch (Exception e) {
            Log.e("FavoritesActivity", "Erreur parsing annonce", e);
            return null;
        }
    }

    private class FavoritesAdapter extends RecyclerView.Adapter<FavoriteVH> {
        private final List<Annonce> items = new ArrayList<>();

        void setItems(List<Annonce> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FavoriteVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_annonce, parent, false);
            return new FavoriteVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FavoriteVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() { return items.size(); }
    }

    private class FavoriteVH extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvSubtitle, tvPrix;
        private final com.google.android.material.imageview.ShapeableImageView ivImage;
        private final com.google.android.material.chip.Chip chipType;
        private final TextView tvTypeBien;

        FavoriteVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvTypeBien = itemView.findViewById(R.id.tv_type_bien);
            tvPrix = itemView.findViewById(R.id.tv_prix);
            ivImage = itemView.findViewById(R.id.iv_annonce_image);
            chipType = itemView.findViewById(R.id.chip_type);
        }

        void bind(Annonce a) {
            tvTitle.setText(a.getTitre());
            String sub = (a.getLocalisation() != null ? a.getLocalisation() : "");
            tvSubtitle.setText(sub);

            String typeBien = a.getTypeBien() != null ? a.getTypeBien() : "";

            if (tvTypeBien != null) {
                tvTypeBien.setText(typeBien);
            }

            if (chipType != null) {
                chipType.setText(typeBien);
            }

            if (tvPrix != null) {
                tvPrix.setText(String.format("%.0f DT", a.getPrix()));
            }

            // Charger l'image si disponible
            if (ivImage != null && a.getImages() != null && !a.getImages().isEmpty()) {
                String firstImage = a.getImages().get(0);
                if (firstImage != null && !firstImage.isEmpty()) {
                    if (firstImage.startsWith("data:image")) {
                        try {
                            String base64Image = firstImage.substring(firstImage.indexOf(",") + 1);
                            byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            ivImage.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            Log.e("FavoritesActivity", "Erreur chargement image base64", e);
                            ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
                        }
                    } else {
                        ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } else {
                    ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else if (ivImage != null) {
                ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesActivity.this, AnnonceDetailActivity.class);
                intent.putExtra("annonceId", a.getId());
                startActivity(intent);
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(new Intent(FavoritesActivity.this, ListingsActivity.class));
        } else if (id == R.id.nav_favorites) {
            // Déjà sur cette page, rien à faire
        } else if (id == R.id.nav_my_annonces) {
            startActivity(new Intent(FavoritesActivity.this, MyAnnoncesActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(FavoritesActivity.this, ProfileActivity.class));
        } else if (id == R.id.nav_create_annonce) {
            startActivity(new Intent(FavoritesActivity.this, CreateAnnonceActivity.class));
        } else if (id == R.id.nav_admin) {
            startActivity(new Intent(FavoritesActivity.this, AdminActivity.class));
        } else if (id == R.id.nav_logout) {
            performLogout();
        } else {
            Toast.makeText(this, "Action pour: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void performLogout() {
        // 1. Supprimer le token JWT des SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // 2. Naviguer vers l'écran de connexion
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
