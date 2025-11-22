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
import com.example.devmobile.api.FavoriService;
import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.models.Annonce;
import com.example.devmobile.models.AnnonceListResponse;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.navigation.NavigationView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView tvWelcome, tvStatsAnnonces, tvStatsFavoris;
    private LinearLayout tvEmptyAnnonces; // LinearLayout dans le layout
    private RecyclerView rvAnnonces;
    private ProgressBar progressAnnonces;
    private AnnoncesAdapter annoncesAdapter;
    private com.google.android.material.chip.Chip chipFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialiser le contexte pour Retrofit
        RetrofitClient.setContext(this);

        // Vérifier si l'utilisateur est connecté
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", null);
        if (userId == null || userId.isEmpty()) {
            // L'utilisateur n'est pas connecté, rediriger vers LoginActivity
            android.util.Log.d("DashboardActivity", "Utilisateur non connecté, redirection vers LoginActivity");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        try {
            setContentView(R.layout.activity_dashboard);

            // 1. Configurer la barre d'outils (Toolbar)
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            // Retirer le titre par défaut pour laisser de la place au texte de bienvenue
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }

            // 2. Configurer le menu de navigation (Drawer)
            drawerLayout = findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            // Configuration du bouton de bascule (Hamburger icon)
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            // 3. Initialiser les vues du tableau de bord avec vérification null
            tvWelcome = findViewById(R.id.tv_welcome_message);
            rvAnnonces = findViewById(R.id.rv_annonces_dashboard);
            progressAnnonces = findViewById(R.id.progress_annonces);
            tvEmptyAnnonces = findViewById(R.id.tv_empty_annonces);
            tvStatsAnnonces = findViewById(R.id.tv_stats_annonces);
            tvStatsFavoris = findViewById(R.id.tv_stats_favoris);
            chipFilter = findViewById(R.id.chip_filter);
            
            if (tvWelcome == null || rvAnnonces == null) {
                android.util.Log.e("DashboardActivity", "Vues critiques non trouvées");
                Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // Configurer le RecyclerView pour les annonces avec espacement
            rvAnnonces.setLayoutManager(new LinearLayoutManager(this));
            annoncesAdapter = new AnnoncesAdapter();
            rvAnnonces.setAdapter(annoncesAdapter);
            
            // Ajouter un décorateur d'espacement pour les items
            try {
                androidx.recyclerview.widget.DividerItemDecoration dividerItemDecoration = 
                    new androidx.recyclerview.widget.DividerItemDecoration(rvAnnonces.getContext(), 
                        LinearLayoutManager.VERTICAL);
                dividerItemDecoration.setDrawable(getResources().getDrawable(android.R.color.transparent));
                rvAnnonces.addItemDecoration(dividerItemDecoration);
            } catch (Exception e) {
                android.util.Log.w("DashboardActivity", "Erreur ajout décorateur", e);
            }

            // Configurer le chip de filtre
            if (chipFilter != null) {
                android.util.Log.d("DashboardActivity", "Chip de filtre trouvé et configuré");
                chipFilter.setOnClickListener(v -> {
                    android.util.Log.d("DashboardActivity", "Chip de filtre cliqué");
                    showFilterOptionsDialog();
                });
            } else {
                android.util.Log.e("DashboardActivity", "Chip de filtre non trouvé dans le layout");
            }

            // Afficher/masquer les options selon le type d'utilisateur
            String userType = prefs.getString("USER_TYPE", "client");
            String userName = prefs.getString("USER_PRENOM", "");
            if (!userName.isEmpty()) {
                tvWelcome.setText("Bonjour, " + userName + "!");
            } else {
                tvWelcome.setText("Bonjour!");
            }

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

            // 4. Définir l'élément Accueil comme sélectionné par défaut
            if (savedInstanceState == null) {
                navigationView.setCheckedItem(R.id.nav_home);
            }
            
            // Charger les annonces directement dans le Dashboard
            loadAnnoncesInDashboard();
        } catch (Exception e) {
            android.util.Log.e("DashboardActivity", "Erreur critique dans onCreate", e);
            Toast.makeText(this, "Erreur au démarrage: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void loadAnnoncesInDashboard() {
        progressAnnonces.setVisibility(View.VISIBLE);
        tvEmptyAnnonces.setVisibility(View.GONE);
        rvAnnonces.setVisibility(View.GONE);
        
        AnnonceService service = RetrofitClient.getInstance().getAnnonceService();
        Log.d("DashboardActivity", "Chargement des annonces...");
        service.getAnnonces(null, null, null, null, null, null).enqueue(new Callback<AnnonceListResponse>() {
            @Override
            public void onResponse(@NonNull Call<AnnonceListResponse> call, @NonNull Response<AnnonceListResponse> response) {
                progressAnnonces.setVisibility(View.GONE);
                Log.d("DashboardActivity", "Réponse annonces: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<Annonce> list = response.body().getAnnonces();
                    if (list == null) list = new ArrayList<>();
                    Log.d("DashboardActivity", "Nombre d'annonces: " + list.size());
                    if (list.isEmpty()) {
                        tvEmptyAnnonces.setVisibility(View.VISIBLE);
                        rvAnnonces.setVisibility(View.GONE);
                        android.util.Log.d("DashboardActivity", "Aucune annonce trouvée, affichage du message vide");
                    } else {
                        tvEmptyAnnonces.setVisibility(View.GONE);
                        rvAnnonces.setVisibility(View.VISIBLE);
                        annoncesAdapter.setItems(list);
                        android.util.Log.d("DashboardActivity", "Annonces chargées: " + list.size() + ", RecyclerView visible");

                        // Mettre à jour les statistiques
                        if (tvStatsAnnonces != null) {
                            tvStatsAnnonces.setText(String.valueOf(list.size()));
                        }
                    }
                } else {
                    tvEmptyAnnonces.setVisibility(View.VISIBLE);
                    rvAnnonces.setVisibility(View.GONE);
                    // tvEmptyAnnonces est un LinearLayout, pas un TextView
                    // Le message d'erreur est déjà dans le layout
                }
            }

            @Override
            public void onFailure(@NonNull Call<AnnonceListResponse> call, @NonNull Throwable t) {
                progressAnnonces.setVisibility(View.GONE);
                tvEmptyAnnonces.setVisibility(View.VISIBLE);
                rvAnnonces.setVisibility(View.GONE);
                // tvEmptyAnnonces est un LinearLayout, le message d'erreur est déjà dans le layout
                Log.e("DashboardActivity", "Erreur réseau: " + t.getMessage(), t);
            }
        });
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
        private final com.google.android.material.chip.Chip chipType, tvTypeBien;

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
                        // Image en base64
                        try {
                            String base64Image = firstImage.substring(firstImage.indexOf(",") + 1);
                            byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            ivImage.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            android.util.Log.e("DashboardActivity", "Erreur chargement image base64", e);
                            ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
                        }
                    } else {
                        // URL d'image
                        // Utiliser une bibliothèque comme Glide ou Picasso pour charger l'image
                        ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } else {
                    ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else if (ivImage != null) {
                ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, AnnonceDetailActivity.class);
                intent.putExtra("annonceId", a.getId());
                startActivity(intent);
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // On est déjà sur le dashboard, rien à faire ou rafraîchir
            Toast.makeText(DashboardActivity.this, "🏠 Vous êtes déjà sur l'accueil", Toast.LENGTH_SHORT).show();
            // Optionnellement rafraîchir les annonces
            // loadAnnoncesInDashboard();
        } else if (id == R.id.nav_favorites) {
            startActivity(new Intent(DashboardActivity.this, FavoritesActivity.class));
        } else if (id == R.id.nav_my_annonces) {
            startActivity(new Intent(DashboardActivity.this, MyAnnoncesActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
        } else if (id == R.id.nav_create_annonce) {
            startActivity(new Intent(DashboardActivity.this, CreateAnnonceActivity.class));
        } else if (id == R.id.nav_admin) {
            startActivity(new Intent(DashboardActivity.this, AdminActivity.class));
        } else if (id == R.id.nav_logout) {
            performLogout();
        } else {
            Toast.makeText(this, "Action pour: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }

        // Fermer le tiroir après la sélection
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Gère la déconnexion de l'utilisateur.
     */
    private void performLogout() {
        // 1. Supprimer les données des SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // 2. Naviguer vers l'écran de connexion
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();
    }

    private void showFilterOptionsDialog() {
        android.util.Log.d("DashboardActivity", "Affichage du dialogue d'options de filtrage");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("🔍 Filtrer les annonces");
        builder.setMessage("Choisissez votre méthode de filtrage préférée");

        // Layout moderne avec Material Design
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(24, 8, 24, 24);

        // Bouton pour filtrer par localisation
        com.google.android.material.button.MaterialButton btnLocation = new com.google.android.material.button.MaterialButton(this);
        btnLocation.setText("📍 Par localisation");
        btnLocation.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_bright)));
        btnLocation.setTextColor(getResources().getColor(android.R.color.white));
        btnLocation.setCornerRadius(12);
        btnLocation.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mylocation));
        btnLocation.setIconTint(getResources().getColorStateList(android.R.color.white));
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 12);
        btnLocation.setLayoutParams(params);
        btnLocation.setOnClickListener(v -> {
            try {
                android.app.AlertDialog dialog = builder.create();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                // Ouvrir le dialogue de filtrage par localisation
                showLocationFilterDialog();
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Erreur lors de l'ouverture du filtre localisation", e);
            }
        });
        layout.addView(btnLocation);

        // Bouton pour filtrer par prix
        com.google.android.material.button.MaterialButton btnPrice = new com.google.android.material.button.MaterialButton(this);
        btnPrice.setText("💰 Par prix");
        btnPrice.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_light)));
        btnPrice.setTextColor(getResources().getColor(android.R.color.white));
        btnPrice.setCornerRadius(12);
        btnPrice.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_today));
        btnPrice.setIconTint(getResources().getColorStateList(android.R.color.white));
        btnPrice.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        btnPrice.setOnClickListener(v -> {
            try {
                android.app.AlertDialog dialog = builder.create();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                // Ouvrir le dialogue de filtrage par prix
                showPriceFilterDialog();
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Erreur lors de l'ouverture du filtre prix", e);
            }
        });
        layout.addView(btnPrice);

        builder.setView(layout);
        builder.setNegativeButton("Annuler", null);

        try {
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
            android.util.Log.d("DashboardActivity", "Dialogue d'options de filtrage affiché");
        } catch (Exception e) {
            android.util.Log.e("DashboardActivity", "Erreur lors de l'affichage du dialogue d'options", e);
            Toast.makeText(DashboardActivity.this, "Erreur d'affichage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLocationFilterDialog() {
        android.util.Log.d("DashboardActivity", "Affichage du dialogue de filtrage par localisation");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("📍 Filtrer par localisation");
        builder.setMessage("Trouvez des annonces dans votre zone préférée");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(24, 8, 24, 16);

        // TextInputLayout pour un style moderne
        com.google.android.material.textfield.TextInputLayout textInputLayout = new com.google.android.material.textfield.TextInputLayout(this);
        textInputLayout.setHint("Localisation");
        textInputLayout.setHelperText("Ville, quartier, région...");
        textInputLayout.setStartIconDrawable(android.R.drawable.ic_menu_mylocation);
        textInputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

        final com.google.android.material.textfield.TextInputEditText etLocation = new com.google.android.material.textfield.TextInputEditText(this);
        etLocation.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        textInputLayout.addView(etLocation);

        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 8, 0, 0);
        textInputLayout.setLayoutParams(layoutParams);
        layout.addView(textInputLayout);

        builder.setView(layout);

        builder.setPositiveButton("🔍 Filtrer", (dialog, which) -> {
            try {
                String localisation = etLocation.getText().toString().trim();
                if (!localisation.isEmpty()) {
                    android.util.Log.d("DashboardActivity", "Filtrage par localisation: " + localisation);
                    Toast.makeText(DashboardActivity.this, "🔍 Recherche à " + localisation + "...", Toast.LENGTH_SHORT).show();
                    loadAnnoncesWithFilters(localisation, null, null);
                } else {
                    Toast.makeText(DashboardActivity.this, "⚠️ Veuillez entrer une localisation", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Erreur lors du filtrage par localisation", e);
                Toast.makeText(DashboardActivity.this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("❌ Annuler", null);

        builder.setNeutralButton("🗑️ Effacer les filtres", (dialog, which) -> {
            try {
                android.util.Log.d("DashboardActivity", "Effacement des filtres");
                Toast.makeText(DashboardActivity.this, "🔄 Filtres effacés", Toast.LENGTH_SHORT).show();
                loadAnnoncesInDashboard();
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Erreur lors de l'effacement", e);
                Toast.makeText(DashboardActivity.this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        try {
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            android.util.Log.e("DashboardActivity", "Erreur lors de l'affichage du dialogue localisation", e);
            Toast.makeText(DashboardActivity.this, "Erreur d'affichage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPriceFilterDialog() {
        android.util.Log.d("DashboardActivity", "Affichage du dialogue de filtrage par prix");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("💰 Filtrer par prix");
        builder.setMessage("Définissez votre fourchette de prix");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(24, 8, 24, 16);

        // Prix minimum avec TextInputLayout
        com.google.android.material.textfield.TextInputLayout tilPrixMin = new com.google.android.material.textfield.TextInputLayout(this);
        tilPrixMin.setHint("Prix minimum");
        tilPrixMin.setHelperText("Prix minimum en DT");
        tilPrixMin.setStartIconDrawable(android.R.drawable.ic_menu_today);
        tilPrixMin.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

        final com.google.android.material.textfield.TextInputEditText etPrixMin = new com.google.android.material.textfield.TextInputEditText(this);
        etPrixMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        tilPrixMin.addView(etPrixMin);

        android.widget.LinearLayout.LayoutParams layoutParamsMin = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParamsMin.setMargins(0, 8, 0, 8);
        tilPrixMin.setLayoutParams(layoutParamsMin);
        layout.addView(tilPrixMin);

        // Prix maximum avec TextInputLayout
        com.google.android.material.textfield.TextInputLayout tilPrixMax = new com.google.android.material.textfield.TextInputLayout(this);
        tilPrixMax.setHint("Prix maximum");
        tilPrixMax.setHelperText("Prix maximum en DT (optionnel)");
        tilPrixMax.setStartIconDrawable(android.R.drawable.ic_menu_today);
        tilPrixMax.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

        final com.google.android.material.textfield.TextInputEditText etPrixMax = new com.google.android.material.textfield.TextInputEditText(this);
        etPrixMax.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        tilPrixMax.addView(etPrixMax);

        android.widget.LinearLayout.LayoutParams layoutParamsMax = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParamsMax.setMargins(0, 8, 0, 0);
        tilPrixMax.setLayoutParams(layoutParamsMax);
        layout.addView(tilPrixMax);

        builder.setView(layout);

        builder.setPositiveButton("🔍 Filtrer", (dialog, which) -> {
            try {
                String prixMin = etPrixMin.getText().toString().trim();
                String prixMax = etPrixMax.getText().toString().trim();

                // Validation basique
                if (!prixMin.isEmpty() || !prixMax.isEmpty()) {
                    // Convertir vides en null
                    prixMin = prixMin.isEmpty() ? null : prixMin;
                    prixMax = prixMax.isEmpty() ? null : prixMax;

                    // Validation des valeurs si les deux sont remplis
                    if (prixMin != null && prixMax != null) {
                        try {
                            double min = Double.parseDouble(prixMin);
                            double max = Double.parseDouble(prixMax);
                            if (min > max) {
                                Toast.makeText(DashboardActivity.this, "⚠️ Le prix minimum ne peut pas être supérieur au maximum", Toast.LENGTH_LONG).show();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(DashboardActivity.this, "⚠️ Veuillez entrer des prix valides", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    android.util.Log.d("DashboardActivity", "Filtrage par prix: min=" + prixMin + ", max=" + prixMax);
                    String priceRange = (prixMin != null ? prixMin : "0") + " - " + (prixMax != null ? prixMax : "∞") + " DT";
                    Toast.makeText(DashboardActivity.this, "🔍 Recherche: " + priceRange, Toast.LENGTH_SHORT).show();
                    loadAnnoncesWithFilters(null, prixMin, prixMax);
                } else {
                    Toast.makeText(DashboardActivity.this, "⚠️ Veuillez entrer au moins un prix", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Erreur lors du filtrage par prix", e);
                Toast.makeText(DashboardActivity.this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("❌ Annuler", null);

        builder.setNeutralButton("🗑️ Effacer les filtres", (dialog, which) -> {
            try {
                android.util.Log.d("DashboardActivity", "Effacement des filtres");
                Toast.makeText(DashboardActivity.this, "🔄 Filtres effacés", Toast.LENGTH_SHORT).show();
                loadAnnoncesInDashboard();
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Erreur lors de l'effacement", e);
                Toast.makeText(DashboardActivity.this, "❌ Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        try {
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            android.util.Log.e("DashboardActivity", "Erreur lors de l'affichage du dialogue prix", e);
            Toast.makeText(DashboardActivity.this, "Erreur d'affichage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAnnoncesWithFilters(String localisation, String prixMin, String prixMax) {
        try {
            android.util.Log.d("DashboardActivity", "Rechargement avec filtres: loc=" + localisation + ", min=" + prixMin + ", max=" + prixMax);

            progressAnnonces.setVisibility(View.VISIBLE);
            tvEmptyAnnonces.setVisibility(View.GONE);
            rvAnnonces.setVisibility(View.GONE);

            AnnonceService service = RetrofitClient.getInstance().getAnnonceService();
            android.util.Log.d("DashboardActivity", "Service obtenu, appel API...");
            service.getAnnonces(localisation, prixMin, prixMax, null, null, null).enqueue(new Callback<AnnonceListResponse>() {
                @Override
                public void onResponse(@NonNull Call<AnnonceListResponse> call, @NonNull Response<AnnonceListResponse> response) {
                    try {
                        progressAnnonces.setVisibility(View.GONE);
                        android.util.Log.d("DashboardActivity", "Réponse filtrée: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            List<Annonce> list = response.body().getAnnonces();
                            if (list == null) list = new ArrayList<>();
                            android.util.Log.d("DashboardActivity", "Annonces filtrées: " + list.size());

                            if (list.isEmpty()) {
                                tvEmptyAnnonces.setVisibility(View.VISIBLE);
                                rvAnnonces.setVisibility(View.GONE);
                                android.util.Log.d("DashboardActivity", "Aucune annonce trouvée avec ces filtres");
                            } else {
                                tvEmptyAnnonces.setVisibility(View.GONE);
                                rvAnnonces.setVisibility(View.VISIBLE);
                                annoncesAdapter.setItems(list);
                                android.util.Log.d("DashboardActivity", "Annonces affichées: " + list.size());

                                // Mettre à jour les statistiques
                                if (tvStatsAnnonces != null) {
                                    tvStatsAnnonces.setText(String.valueOf(list.size()));
                                }
                            }
                        } else {
                            tvEmptyAnnonces.setVisibility(View.VISIBLE);
                            rvAnnonces.setVisibility(View.GONE);
                            android.util.Log.w("DashboardActivity", "Réponse non réussie: " + response.code());
                        }
                    } catch (Exception e) {
                        android.util.Log.e("DashboardActivity", "Erreur dans onResponse", e);
                        progressAnnonces.setVisibility(View.GONE);
                        tvEmptyAnnonces.setVisibility(View.VISIBLE);
                        rvAnnonces.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AnnonceListResponse> call, @NonNull Throwable t) {
                    android.util.Log.e("DashboardActivity", "Erreur réseau filtrage: " + t.getMessage(), t);
                    progressAnnonces.setVisibility(View.GONE);
                    tvEmptyAnnonces.setVisibility(View.VISIBLE);
                    rvAnnonces.setVisibility(View.GONE);
                    Toast.makeText(DashboardActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("DashboardActivity", "Erreur dans loadAnnoncesWithFilters", e);
            progressAnnonces.setVisibility(View.GONE);
            tvEmptyAnnonces.setVisibility(View.VISIBLE);
            rvAnnonces.setVisibility(View.GONE);
            Toast.makeText(DashboardActivity.this, "Erreur de filtrage: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Ferme le tiroir au lieu de quitter l'application si le tiroir est ouvert
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
