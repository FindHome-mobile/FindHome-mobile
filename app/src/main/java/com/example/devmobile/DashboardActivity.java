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
import com.google.android.material.switchmaterial.SwitchMaterial;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView tvWelcome, tvStatsAnnonces, tvStatsFavoris;
    private LinearLayout tvEmptyAnnonces; // LinearLayout dans le layout
    private RecyclerView rvAnnonces;
    private ProgressBar progressAnnonces;
    private AnnoncesAdapter annoncesAdapter;
    private com.google.android.material.chip.Chip chipFilter;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DashboardActivity", "onCreate appelé - savedInstanceState: " + (savedInstanceState != null));

        // Initialiser le contexte pour Retrofit
        RetrofitClient.setContext(this);
        Log.d("DashboardActivity", "RetrofitClient.setContext appelé");

        // Vérifier si l'utilisateur est connecté
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", null);
        Log.d("DashboardActivity", "Vérification connexion - userId: " + userId);

        if (userId == null || userId.isEmpty()) {
            // L'utilisateur n'est pas connecté, rediriger vers LoginActivity
            Log.d("DashboardActivity", "Utilisateur non connecté, redirection vers LoginActivity");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        try {
            Log.d("DashboardActivity", "setContentView appelé");
            setContentView(R.layout.activity_dashboard);

            // 1. Configurer la barre d'outils (Toolbar)
            Toolbar toolbar = findViewById(R.id.toolbar);
            Log.d("DashboardActivity", "Toolbar trouvée: " + (toolbar != null));
            setSupportActionBar(toolbar);
            // Retirer le titre par défaut pour laisser de la place au texte de bienvenue
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }

            // 2. Configurer le menu de navigation (Drawer)
            drawerLayout = findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            Log.d("DashboardActivity", "DrawerLayout trouvé: " + (drawerLayout != null) + ", NavigationView trouvé: "
                    + (navigationView != null));
            navigationView.setNavigationItemSelectedListener(this);

            // Configuration du bouton de bascule (Hamburger icon)
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);

            // Désactiver l'indicateur par défaut (qui fait l'animation flèche/burger)
            toggle.setDrawerIndicatorEnabled(false);

            // Définir notre icône statique "3 tirets" blanche
            toolbar.setNavigationIcon(R.drawable.ic_menu_burger);

            // Gérer le clic sur l'icône pour ouvrir le drawer
            toolbar.setNavigationOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });

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
            swipeRefreshLayout = findViewById(R.id.swipe_refresh);

            Log.d("DashboardActivity", "Vues initialisées - tvWelcome: " + (tvWelcome != null) +
                    ", rvAnnonces: " + (rvAnnonces != null) +
                    ", progressAnnonces: " + (progressAnnonces != null) +
                    ", tvEmptyAnnonces: " + (tvEmptyAnnonces != null));

            if (tvWelcome == null || rvAnnonces == null) {
                Log.e("DashboardActivity", "Vues critiques non trouvées");
                Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Configurer le SwipeRefreshLayout
            if (swipeRefreshLayout != null) {
                // Couleurs modernes pour le refresh
                swipeRefreshLayout.setColorSchemeResources(R.color.md_primary, R.color.md_secondary,
                        R.color.md_tertiary);
                swipeRefreshLayout.setOnRefreshListener(this::loadAnnoncesInDashboard);
            }

            // Configurer le RecyclerView pour les annonces avec espacement et animations
            rvAnnonces.setLayoutManager(new LinearLayoutManager(this));
            rvAnnonces.setHasFixedSize(false); // Important: permet au RecyclerView de mesurer correctement les items
            rvAnnonces.setNestedScrollingEnabled(true); // Active le scroll fluide

            annoncesAdapter = new AnnoncesAdapter();
            rvAnnonces.setAdapter(annoncesAdapter);

            // Configurer les animations modernes
            rvAnnonces.setItemAnimator(new ModernItemAnimator());

            // Ajouter un décorateur d'espacement pour les items avec padding moderne
            try {
                androidx.recyclerview.widget.DividerItemDecoration dividerItemDecoration = new androidx.recyclerview.widget.DividerItemDecoration(
                        rvAnnonces.getContext(),
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
            Log.d("DashboardActivity", "Type utilisateur: " + userType + ", nom: " + userName);
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

            // 5. Configurer le switch du mode sombre
            MenuItem darkModeItem = navigationView.getMenu().findItem(R.id.nav_dark_mode);
            if (darkModeItem != null) {

                com.google.android.material.switchmaterial.SwitchMaterial switchDarkMode = (com.google.android.material.switchmaterial.SwitchMaterial) darkModeItem
                        .getActionView().findViewById(R.id.switch_dark_mode);

                if (switchDarkMode != null) {
                    // Initialiser l'état du switch
                    int currentNightMode = getResources().getConfiguration().uiMode
                            & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                    switchDarkMode.setChecked(currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);

                    // Gérer le changement d'état
                    switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            androidx.appcompat.app.AppCompatDelegate
                                    .setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                        } else {
                            androidx.appcompat.app.AppCompatDelegate
                                    .setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                        }
                        // Recréer l'activité pour appliquer le nouveau thème immédiatement
                        recreate();
                    });

                    // Rendre l'item cliquable pour basculer le switch aussi
                    darkModeItem.setOnMenuItemClickListener(item -> {
                        switchDarkMode.toggle();
                        return true;
                    });
                }
            }

            // Charger les annonces directement dans le Dashboard
            loadAnnoncesInDashboard();
        } catch (Exception e) {
            Log.e("DashboardActivity", "Erreur critique dans onCreate", e);
            Toast.makeText(this, "Erreur au démarrage: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private void loadAnnoncesInDashboard() {
        Log.d("DashboardActivity", "loadAnnoncesInDashboard appelé");

        if (progressAnnonces == null || tvEmptyAnnonces == null || rvAnnonces == null) {
            Log.e("DashboardActivity", "Vues non initialisées dans loadAnnoncesInDashboard");
            if (swipeRefreshLayout != null)
                swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Si on rafraîchit via le swipe, on ne montre pas la progressBar centrale
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            progressAnnonces.setVisibility(View.GONE);
        } else {
            progressAnnonces.setVisibility(View.VISIBLE);
        }

        tvEmptyAnnonces.setVisibility(View.GONE);
        // On garde le RecyclerView visible s'il a déjà des données pour éviter le
        // clignotement
        if (annoncesAdapter.getItemCount() == 0) {
            rvAnnonces.setVisibility(View.GONE);
        }

        AnnonceService service = RetrofitClient.getInstance().getAnnonceService();
        Log.d("DashboardActivity", "Chargement des annonces...");
        service.getAnnonces(null, null, null, null, null, null).enqueue(new Callback<AnnonceListResponse>() {
            @Override
            public void onResponse(@NonNull Call<AnnonceListResponse> call,
                    @NonNull Response<AnnonceListResponse> response) {
                if (!isFinishing() && !isDestroyed()) {
                    progressAnnonces.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null)
                        swipeRefreshLayout.setRefreshing(false);
                }
                Log.d("DashboardActivity", "Réponse annonces: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<Annonce> list = response.body().getAnnonces();
                    if (list == null)
                        list = new ArrayList<>();
                    Log.d("DashboardActivity", "Nombre d'annonces: " + list.size());
                    if (list.isEmpty()) {
                        tvEmptyAnnonces.setVisibility(View.VISIBLE);
                        rvAnnonces.setVisibility(View.GONE);
                        android.util.Log.d("DashboardActivity", "Aucune annonce trouvée, affichage du message vide");
                    } else {
                        tvEmptyAnnonces.setVisibility(View.GONE);
                        rvAnnonces.setVisibility(View.VISIBLE);
                        annoncesAdapter.setItems(list);
                        android.util.Log.d("DashboardActivity",
                                "Annonces chargées: " + list.size() + ", RecyclerView visible");

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
                if (!isFinishing() && !isDestroyed()) {
                    progressAnnonces.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null)
                        swipeRefreshLayout.setRefreshing(false);
                    tvEmptyAnnonces.setVisibility(View.VISIBLE);
                    rvAnnonces.setVisibility(View.GONE);
                    // tvEmptyAnnonces est un LinearLayout, le message d'erreur est déjà dans le
                    // layout
                }
                Log.e("DashboardActivity", "Erreur réseau: " + t.getMessage(), t);
            }
        });
    }

    private class AnnoncesAdapter extends RecyclerView.Adapter<AnnonceVH> {
        private final List<Annonce> items = new ArrayList<>();

        void setItems(List<Annonce> newItems) {
            Log.d("DashboardAdapter", "setItems appelé avec " + newItems.size() + " annonces");
            items.clear();
            items.addAll(newItems);
            Log.d("DashboardAdapter", "items.size() après ajout: " + items.size());
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AnnonceVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            Log.d("DashboardAdapter", "onCreateViewHolder appelé");
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_annonce, parent, false);
            return new AnnonceVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AnnonceVH holder, int position) {
            Log.d("DashboardAdapter", "onBindViewHolder appelé pour position: " + position);
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            int count = items.size();
            Log.d("DashboardAdapter", "getItemCount retourne: " + count);
            return count;
        }
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
                        // Image en base64
                        try {
                            String base64Image = firstImage.substring(firstImage.indexOf(",") + 1);
                            byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory
                                    .decodeByteArray(decodedBytes, 0, decodedBytes.length);
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

                // Démarrage simple de l'activité pour compatibilité API 24
                startActivity(intent);
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Log.d("DashboardActivity", "Navigation item sélectionné: " + id);

        if (id == R.id.nav_home) {
            // On est déjà sur le dashboard, rien à faire ou rafraîchir
            Toast.makeText(DashboardActivity.this, "🏠 Vous êtes déjà sur l'accueil", Toast.LENGTH_SHORT).show();
            // Optionnellement rafraîchir les annonces
            // loadAnnoncesInDashboard();
        } else if (id == R.id.nav_favorites) {
            startActivityWithAnimation(new Intent(DashboardActivity.this, FavoritesActivity.class));
        } else if (id == R.id.nav_my_annonces) {
            startActivityWithAnimation(new Intent(DashboardActivity.this, MyAnnoncesActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivityWithAnimation(new Intent(DashboardActivity.this, ProfileActivity.class));
        } else if (id == R.id.nav_create_annonce) {
            startActivityWithAnimation(new Intent(DashboardActivity.this, CreateAnnonceActivity.class));
        } else if (id == R.id.nav_admin) {
            startActivityWithAnimation(new Intent(DashboardActivity.this, AdminActivity.class));
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
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();
    }

    private void showFilterOptionsDialog() {
        Log.d("DashboardActivity", "Affichage du bottom sheet de filtrage moderne");

        if (isFinishing() || isDestroyed()) {
            Log.w("DashboardActivity", "Activité en cours de destruction");
            return;
        }

        // Utiliser un Bottom Sheet moderne - tous les filtres en un seul endroit
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        android.view.View view = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);

        // Récupérer les vues
        final com.google.android.material.textfield.TextInputEditText etLocation = view
                .findViewById(R.id.et_location_filter);
        final com.google.android.material.textfield.TextInputEditText etPrixMin = view.findViewById(R.id.et_prix_min);
        final com.google.android.material.textfield.TextInputEditText etPrixMax = view.findViewById(R.id.et_prix_max);
        final com.google.android.material.chip.ChipGroup chipGroupType = view.findViewById(R.id.chip_group_type);

        // Bouton fermer
        android.widget.ImageView btnClose = view.findViewById(R.id.btn_close_filter);
        btnClose.setOnClickListener(v -> bottomSheet.dismiss());

        // Bouton réinitialiser
        com.google.android.material.button.MaterialButton btnReset = view.findViewById(R.id.btn_reset_filter);
        btnReset.setOnClickListener(v -> {
            etLocation.setText("");
            etPrixMin.setText("");
            etPrixMax.setText("");
            chipGroupType.clearCheck();
            Toast.makeText(this, "Filtres réinitialisés", Toast.LENGTH_SHORT).show();
        });

        // Bouton appliquer
        com.google.android.material.button.MaterialButton btnApply = view.findViewById(R.id.btn_apply_filter);
        btnApply.setOnClickListener(v -> {
            try {
                String location = etLocation.getText().toString().trim();
                String prixMin = etPrixMin.getText().toString().trim();
                String prixMax = etPrixMax.getText().toString().trim();

                // Récupérer le type sélectionné
                int selectedChipId = chipGroupType.getCheckedChipId();
                String typeBien = null;
                if (selectedChipId != android.view.View.NO_ID) {
                    com.google.android.material.chip.Chip selectedChip = view.findViewById(selectedChipId);
                    if (selectedChip != null) {
                        typeBien = selectedChip.getText().toString();
                    }
                }

                // Validation
                if (location.isEmpty() && prixMin.isEmpty() && prixMax.isEmpty() && typeBien == null) {
                    Toast.makeText(this, "Veuillez sélectionner au moins un filtre", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validation des prix
                if (!prixMin.isEmpty() && !prixMax.isEmpty()) {
                    try {
                        double min = Double.parseDouble(prixMin);
                        double max = Double.parseDouble(prixMax);
                        if (min > max) {
                            Toast.makeText(this, "Le prix minimum ne peut pas être supérieur au maximum",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Veuillez entrer des prix valides", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Appliquer les filtres
                String message = "Filtres appliqués";
                if (!location.isEmpty())
                    message += " • " + location;
                if (!prixMin.isEmpty() || !prixMax.isEmpty()) {
                    message += " • " + (prixMin.isEmpty() ? "0" : prixMin) + "-" + (prixMax.isEmpty() ? "∞" : prixMax)
                            + " DT";
                }
                if (typeBien != null)
                    message += " • " + typeBien;

                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                loadAnnoncesWithFilters(location.isEmpty() ? null : location,
                        prixMin.isEmpty() ? null : prixMin,
                        prixMax.isEmpty() ? null : prixMax);
                bottomSheet.dismiss();
            } catch (Exception e) {
                android.util.Log.e("DashboardActivity", "Erreur lors de l'application des filtres", e);
                Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheet.setContentView(view);
        bottomSheet.show();
    }

    private void loadAnnoncesWithFilters(String localisation, String prixMin, String prixMax) {
        try {
            Log.d("DashboardActivity",
                    "Rechargement avec filtres: loc=" + localisation + ", min=" + prixMin + ", max=" + prixMax);

            if (progressAnnonces == null || tvEmptyAnnonces == null || rvAnnonces == null) {
                Log.e("DashboardActivity", "Vues non initialisées dans loadAnnoncesWithFilters");
                return;
            }

            progressAnnonces.setVisibility(View.VISIBLE);
            tvEmptyAnnonces.setVisibility(View.GONE);
            rvAnnonces.setVisibility(View.GONE);

            AnnonceService service = RetrofitClient.getInstance().getAnnonceService();
            android.util.Log.d("DashboardActivity", "Service obtenu, appel API...");
            service.getAnnonces(localisation, prixMin, prixMax, null, null, null)
                    .enqueue(new Callback<AnnonceListResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<AnnonceListResponse> call,
                                @NonNull Response<AnnonceListResponse> response) {
                            try {
                                if (!isFinishing() && !isDestroyed()) {
                                    progressAnnonces.setVisibility(View.GONE);
                                }
                                android.util.Log.d("DashboardActivity", "Réponse filtrée: " + response.code());
                                if (response.isSuccessful() && response.body() != null) {
                                    List<Annonce> list = response.body().getAnnonces();
                                    if (list == null)
                                        list = new ArrayList<>();
                                    android.util.Log.d("DashboardActivity", "Annonces filtrées: " + list.size());

                                    if (list.isEmpty()) {
                                        tvEmptyAnnonces.setVisibility(View.VISIBLE);
                                        rvAnnonces.setVisibility(View.GONE);
                                        android.util.Log.d("DashboardActivity",
                                                "Aucune annonce trouvée avec ces filtres");
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
                            Log.e("DashboardActivity", "Erreur réseau filtrage: " + t.getMessage(), t);
                            if (!isFinishing() && !isDestroyed()) {
                                progressAnnonces.setVisibility(View.GONE);
                                tvEmptyAnnonces.setVisibility(View.VISIBLE);
                                rvAnnonces.setVisibility(View.GONE);
                                Toast.makeText(DashboardActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT)
                                        .show();
                            }
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
    protected void onResume() {
        super.onResume();
        Log.d("DashboardActivity", "onResume appelé");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("DashboardActivity", "onPause appelé");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("DashboardActivity", "onDestroy appelé");
    }

    @Override
    public void onBackPressed() {
        Log.d("DashboardActivity", "onBackPressed appelé");
        // Ferme le tiroir au lieu de quitter l'application si le tiroir est ouvert
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Méthode helper pour les transitions d'activité fluides
    private void startActivityWithAnimation(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // Animations modernes pour les items du RecyclerView
    private static class ModernItemAnimator extends androidx.recyclerview.widget.DefaultItemAnimator {

        @Override
        public boolean animateAdd(androidx.recyclerview.widget.RecyclerView.ViewHolder holder) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(100f);
            holder.itemView.setScaleX(0.95f);
            holder.itemView.setScaleY(0.95f);

            holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .setListener(new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            dispatchAddFinished(holder);
                        }
                    })
                    .start();

            return false;
        }

        @Override
        public boolean animateRemove(androidx.recyclerview.widget.RecyclerView.ViewHolder holder) {
            holder.itemView.animate()
                    .alpha(0f)
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .setListener(new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            dispatchRemoveFinished(holder);
                        }
                    })
                    .start();

            return false;
        }
    }
}
