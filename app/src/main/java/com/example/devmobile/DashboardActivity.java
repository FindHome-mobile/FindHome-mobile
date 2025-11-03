package com.example.devmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Configurer la barre d'outils (Toolbar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Retirer le titre par défaut pour laisser de la place au texte de bienvenue
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        // 2. Configurer le menu de navigation (Drawer)
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Configuration du bouton de bascule (Hamburger icon)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 3. Initialiser les vues du tableau de bord
        tvWelcome = findViewById(R.id.tv_welcome_message);
        // Simulation: Remplacer par la récupération du nom de l'utilisateur stocké localement
        tvWelcome.setText("Bonjour, Hazem!");

        // 4. Définir l'élément Accueil comme sélectionné par défaut
        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.nav_home);
            // Ici, vous pourriez charger un Fragment d'accueil si vous utilisiez des Fragments
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(new Intent(DashboardActivity.this, ListingsActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
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
        // 1. Supprimer le token JWT des SharedPreferences
        // getSharedPreferences("AuthPrefs", MODE_PRIVATE).edit().clear().apply();

        // 2. Naviguer vers l'écran de connexion
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
