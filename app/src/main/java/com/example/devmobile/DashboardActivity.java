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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
            
            // Afficher/masquer les options selon le type d'utilisateur
            String userType = prefs.getString("USER_TYPE", "client");
            String userName = prefs.getString("USER_PRENOM", "");
            if (!userName.isEmpty()) {
                tvWelcome.setText("Bonjour, " + userName + "!");
            } else {
                tvWelcome.setText("Bonjour!");
            }

            MenuItem createAnnonceItem = navigationView.getMenu().findItem(R.id.nav_create_annonce);
            MenuItem adminItem = navigationView.getMenu().findItem(R.id.nav_admin);

            if ("proprietaire".equals(userType)) {
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
                    } else {
                        tvEmptyAnnonces.setVisibility(View.GONE);
                        rvAnnonces.setVisibility(View.VISIBLE);
                        annoncesAdapter.setItems(list);
                        
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
            startActivity(new Intent(DashboardActivity.this, ListingsActivity.class));
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
        // 1. Supprimer toutes les données d'authentification stockées localement
        try {
            SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
        } catch (Exception e) {
            Log.e("DashboardActivity", "Erreur lors du clear AuthPrefs", e);
        }

        // 2. Naviguer vers l'écran de connexion et nettoyer la back stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();
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
