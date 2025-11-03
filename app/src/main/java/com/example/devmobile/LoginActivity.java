package com.example.devmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.devmobile.api.AuthService;
import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.models.User;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialisation de Retrofit
        authService = RetrofitClient.getInstance().getAuthService();

        // Initialisation des vues (sans ViewBinding car vous avez demandé Java/XML classique)
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register_prompt);

        // Gestionnaires d'événements
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rediriger vers l'activité d'enregistrement
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Créer l'objet JSON à envoyer au backend
        JsonObject loginBody = new JsonObject();
        loginBody.addProperty("email", email);
        loginBody.addProperty("motDePasse", password);

        // Appel de l'API de connexion
        Call<User> call = authService.loginUser(loginBody);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    // Connexion réussie, enregistrer le token et naviguer
                    // Note: Utiliser SharedPreferences pour stocker le token de manière sécurisée

                    // Sauvegarder l'utilisateur pour le profil
                    try {
                        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor ed = prefs.edit();
                        ed.putString("USER_ID", user.getId());
                        ed.putString("USER_NOM", user.getNom());
                        ed.putString("USER_PRENOM", user.getPrenom());
                        ed.putString("USER_EMAIL", user.getEmail());
                        ed.putString("USER_TYPE", user.getType());
                        ed.putString("USER_PHOTO_URL", user.getPhotoUrl());
                        ed.putString("USER_PHOTO_BASE64", user.getPhotoDeProfile());
                        ed.putString("USER_LOCATION", user.getLocation());
                        ed.apply();
                    } catch (Exception e) {
                        Log.e("LOGIN_PREF", "Failed to persist user: " + e.getMessage());
                    }

                    Toast.makeText(LoginActivity.this, "Connexion réussie! Bienvenue " + user.getPrenom(), Toast.LENGTH_LONG).show();
                    startDashboardActivity();

                } else {
                    // Gérer les erreurs (ex: 401 Unauthorized, 404 Not Found)
                    Log.e("API_ERROR", "Login failed: " + response.code());
                    Toast.makeText(LoginActivity.this, "Échec de la connexion. Vérifiez vos identifiants.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Erreur de réseau ou de serveur
                Log.e("NETWORK_ERROR", "Login failed: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Erreur réseau. Impossible de se connecter au serveur.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startDashboardActivity() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        // Empêche de revenir à l'écran de connexion avec le bouton Retour
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
