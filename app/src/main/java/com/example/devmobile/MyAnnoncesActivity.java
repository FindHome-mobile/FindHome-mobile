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

import com.example.devmobile.api.AnnonceService;
import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.models.Annonce;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyAnnoncesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private RecyclerView rvAnnonces;
    private ProgressBar progressAnnonces;
    private LinearLayout tvEmptyAnnonces;
    private FloatingActionButton fabAddAnnonce;
    private AnnoncesAdapter annoncesAdapter;
    private String userId;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérifier si l'utilisateur est connecté
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", null);
        userType = prefs.getString("USER_TYPE", "client");

        if (userId == null || userId.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Vérifier si c'est un propriétaire
        if (!"proprietaire".equals(userType)) {
            Toast.makeText(this, "Cette fonctionnalité est réservée aux propriétaires", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            setContentView(R.layout.activity_my_annonces);

            // Initialiser le contexte pour Retrofit
            RetrofitClient.setContext(this);

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
            rvAnnonces = findViewById(R.id.rv_my_annonces);
            progressAnnonces = findViewById(R.id.progress_my_annonces);
            tvEmptyAnnonces = findViewById(R.id.tv_empty_my_annonces);
            fabAddAnnonce = findViewById(R.id.fab_add_annonce);

            if (rvAnnonces == null) {
                Log.e("MyAnnoncesActivity", "RecyclerView non trouvé");
                Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Configurer le RecyclerView
            rvAnnonces.setLayoutManager(new LinearLayoutManager(this));
            annoncesAdapter = new AnnoncesAdapter();
            rvAnnonces.setAdapter(annoncesAdapter);

            // Configurer le FAB pour ajouter une annonce
            fabAddAnnonce.setOnClickListener(v -> {
                Intent intent = new Intent(MyAnnoncesActivity.this, CreateAnnonceActivity.class);
                startActivity(intent);
            });

            // Afficher/masquer les options selon le type d'utilisateur
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

            // Définir l'élément Mes annonces comme sélectionné par défaut
            if (savedInstanceState == null) {
                navigationView.setCheckedItem(R.id.nav_my_annonces);
            }

            // Charger les annonces du propriétaire
            loadMyAnnonces();

        } catch (Exception e) {
            Log.e("MyAnnoncesActivity", "Erreur critique dans onCreate", e);
            Toast.makeText(this, "Erreur au démarrage: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadMyAnnonces() {
        progressAnnonces.setVisibility(View.VISIBLE);
        tvEmptyAnnonces.setVisibility(View.GONE);
        rvAnnonces.setVisibility(View.GONE);

        Log.d("MyAnnoncesActivity", "Chargement des annonces pour propriétaire: " + userId);

        AnnonceService service = RetrofitClient.getInstance().getAnnonceService();
        service.getAnnoncesByProprietaireWithAuth(userId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressAnnonces.setVisibility(View.GONE);
                Log.d("MyAnnoncesActivity", "Réponse propriétaire - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Log.d("MyAnnoncesActivity", "Réponse JSON: " + response.body().toString());

                        List<Annonce> annonces = new ArrayList<>();

                        // Essayer différentes structures de réponse
                        if (response.body().has("annonces") && response.body().get("annonces").isJsonArray()) {
                            com.google.gson.JsonArray annoncesArray = response.body().getAsJsonArray("annonces");
                            Log.d("MyAnnoncesActivity", "Annonces trouvées: " + annoncesArray.size());

                            for (int i = 0; i < annoncesArray.size(); i++) {
                                com.google.gson.JsonElement element = annoncesArray.get(i);
                                if (element.isJsonObject()) {
                                    Annonce annonce = parseAnnonceFromJson(element.getAsJsonObject());
                                    if (annonce != null) {
                                        annonces.add(annonce);
                                        Log.d("MyAnnoncesActivity", "Annonce ajoutée: " + annonce.getTitre());
                                    }
                                }
                            }
                        }

                        Log.d("MyAnnoncesActivity", "Total annonces parsées: " + annonces.size());

                        if (annonces.isEmpty()) {
                            tvEmptyAnnonces.setVisibility(View.VISIBLE);
                            rvAnnonces.setVisibility(View.GONE);
                            Log.d("MyAnnoncesActivity", "Aucune annonce trouvée");
                            Toast.makeText(MyAnnoncesActivity.this, "Aucune annonce trouvée. Créez votre première annonce !", Toast.LENGTH_SHORT).show();
                        } else {
                            tvEmptyAnnonces.setVisibility(View.GONE);
                            rvAnnonces.setVisibility(View.VISIBLE);
                            annoncesAdapter.setItems(annonces);
                            Log.d("MyAnnoncesActivity", "Annonces affichées: " + annonces.size());
                        }
                    } catch (Exception e) {
                        Log.e("MyAnnoncesActivity", "Erreur parsing annonces", e);
                        tvEmptyAnnonces.setVisibility(View.VISIBLE);
                        rvAnnonces.setVisibility(View.GONE);
                        Toast.makeText(MyAnnoncesActivity.this, "Erreur de traitement des données", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("MyAnnoncesActivity", "Réponse non réussie: " + response.code() + " - URL: " + call.request().url());
                    tvEmptyAnnonces.setVisibility(View.VISIBLE);
                    rvAnnonces.setVisibility(View.GONE);

                    String errorMsg = "Erreur lors du chargement des annonces";
                    if (response.code() == 401) {
                        errorMsg = "Session expirée. Veuillez vous reconnecter.";
                        // Rediriger vers la page de connexion
                        Intent intent = new Intent(MyAnnoncesActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (response.code() == 403) {
                        errorMsg = "Accès non autorisé";
                    }

                    Toast.makeText(MyAnnoncesActivity.this, errorMsg, Toast.LENGTH_SHORT).show();

                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("MyAnnoncesActivity", "Erreur: " + errorBody);
                        } catch (Exception e) {
                            Log.e("MyAnnoncesActivity", "Impossible de lire l'erreur", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressAnnonces.setVisibility(View.GONE);
                tvEmptyAnnonces.setVisibility(View.VISIBLE);
                rvAnnonces.setVisibility(View.GONE);
                Log.e("MyAnnoncesActivity", "Erreur réseau: " + t.getMessage(), t);
                Toast.makeText(MyAnnoncesActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Annonce parseAnnonceFromJson(com.google.gson.JsonObject jsonObject) {
        try {
            // Utiliser Gson pour convertir le JsonObject en Annonce
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(jsonObject, Annonce.class);
        } catch (Exception e) {
            Log.e("MyAnnoncesActivity", "Erreur parsing annonce", e);
            return null;
        }
    }

    private class AnnoncesAdapter extends RecyclerView.Adapter<AnnonceVH> {
        private final List<Annonce> items = new ArrayList<>();

        void setItems(List<Annonce> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AnnonceVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_annonce, parent, false);
            return new AnnonceVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AnnonceVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() { return items.size(); }
    }

    private class AnnonceVH extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvSubtitle, tvPrix;
        private final com.google.android.material.imageview.ShapeableImageView ivImage;
        private final com.google.android.material.chip.Chip chipType;
        private final TextView tvTypeBien;

        AnnonceVH(@NonNull View itemView) {
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
                            Log.e("MyAnnoncesActivity", "Erreur chargement image base64", e);
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

            // Afficher les boutons d'action pour cette activité
            android.view.View layoutActions = itemView.findViewById(R.id.layout_actions);
            if (layoutActions != null) {
                layoutActions.setVisibility(android.view.View.VISIBLE);

                // Bouton modifier
                android.widget.Button btnEdit = itemView.findViewById(R.id.btn_edit);
                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> editAnnonce(a));
                }

                // Bouton supprimer
                android.widget.Button btnDelete = itemView.findViewById(R.id.btn_delete);
                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> deleteAnnonce(a));
                }
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MyAnnoncesActivity.this, AnnonceDetailActivity.class);
                intent.putExtra("annonceId", a.getId());
                startActivity(intent);
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(new Intent(MyAnnoncesActivity.this, DashboardActivity.class));
        } else if (id == R.id.nav_favorites) {
            startActivity(new Intent(MyAnnoncesActivity.this, FavoritesActivity.class));
        } else if (id == R.id.nav_my_annonces) {
            // Déjà sur cette page, rien à faire
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(MyAnnoncesActivity.this, ProfileActivity.class));
        } else if (id == R.id.nav_create_annonce) {
            startActivity(new Intent(MyAnnoncesActivity.this, CreateAnnonceActivity.class));
        } else if (id == R.id.nav_admin) {
            startActivity(new Intent(MyAnnoncesActivity.this, AdminActivity.class));
        } else if (id == R.id.nav_logout) {
            performLogout();
        } else {
            Toast.makeText(this, "Action pour: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir les annonces quand on revient sur cette activité
        loadMyAnnonces();
    }

    private void editAnnonce(Annonce annonce) {
        Log.d("MyAnnoncesActivity", "Modification de l'annonce: " + annonce.getId());

        Intent intent = new Intent(MyAnnoncesActivity.this, CreateAnnonceActivity.class);
        intent.putExtra("annonceId", annonce.getId());
        intent.putExtra("isEditing", true);
        startActivity(intent);
    }

    private void deleteAnnonce(Annonce annonce) {
        Log.d("MyAnnoncesActivity", "Demande de suppression de l'annonce: " + annonce.getId());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Supprimer l'annonce")
                .setMessage("Êtes-vous sûr de vouloir supprimer l'annonce \"" + annonce.getTitre() + "\" ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> performDeleteAnnonce(annonce))
                .setNegativeButton("Annuler", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performDeleteAnnonce(Annonce annonce) {
        Log.d("MyAnnoncesActivity", "Suppression de l'annonce: " + annonce.getId());

        progressAnnonces.setVisibility(View.VISIBLE);

        AnnonceService service = RetrofitClient.getInstance().getAnnonceService();
        service.deleteAnnonceByProprietaire(annonce.getId()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressAnnonces.setVisibility(View.GONE);
                Log.d("MyAnnoncesActivity", "Réponse suppression - Code: " + response.code());

                if (response.isSuccessful()) {
                    Log.d("MyAnnoncesActivity", "Annonce supprimée avec succès");
                    Toast.makeText(MyAnnoncesActivity.this, "Annonce supprimée avec succès", Toast.LENGTH_SHORT).show();

                    // Rafraîchir la liste
                    loadMyAnnonces();
                } else {
                    Log.e("MyAnnoncesActivity", "Erreur suppression - Code: " + response.code() + " - URL: " + call.request().url());

                    String errorMsg = "Erreur lors de la suppression";
                    if (response.code() == 401) {
                        errorMsg = "Session expirée. Veuillez vous reconnecter.";
                        // Rediriger vers la page de connexion
                        Intent intent = new Intent(MyAnnoncesActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    } else if (response.code() == 403) {
                        errorMsg = "Vous n'avez pas l'autorisation de supprimer cette annonce";
                    } else if (response.code() == 404) {
                        errorMsg = "Annonce introuvable";
                    }

                    Toast.makeText(MyAnnoncesActivity.this, errorMsg, Toast.LENGTH_SHORT).show();

                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("MyAnnoncesActivity", "Erreur body: " + errorBody);
                        } catch (Exception e) {
                            Log.e("MyAnnoncesActivity", "Impossible de lire l'erreur", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressAnnonces.setVisibility(View.GONE);
                Log.e("MyAnnoncesActivity", "Erreur réseau suppression: " + t.getMessage(), t);
                Toast.makeText(MyAnnoncesActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
