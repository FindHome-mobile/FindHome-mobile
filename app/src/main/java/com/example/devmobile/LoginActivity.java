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
        
        try {
            setContentView(R.layout.activity_login);

            // Vérifier si l'utilisateur est déjà connecté
            SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
            String userId = prefs.getString("USER_ID", null);
            if (userId != null && !userId.isEmpty()) {
                // L'utilisateur est déjà connecté, rediriger vers le Dashboard
                Log.d("LoginActivity", "Utilisateur déjà connecté, redirection vers Dashboard");
                startDashboardActivity();
                return;
            }

            // Initialisation du contexte pour Retrofit
            RetrofitClient.setContext(this);

            // Initialisation de Retrofit
            try {
                authService = RetrofitClient.getInstance().getAuthService();
            } catch (Exception e) {
                Log.e("LoginActivity", "Erreur initialisation Retrofit", e);
                Toast.makeText(this, "Erreur d'initialisation. Veuillez réessayer.", Toast.LENGTH_LONG).show();
                return;
            }

            // Initialisation des vues avec vérification null
            try {
                etEmail = findViewById(R.id.et_email);
                etPassword = findViewById(R.id.et_password);
                btnLogin = findViewById(R.id.btn_login);
                tvRegister = findViewById(R.id.tv_register_prompt);

                if (etEmail == null || etPassword == null || btnLogin == null || tvRegister == null) {
                    Log.e("LoginActivity", "Une ou plusieurs vues sont null");
                    Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_LONG).show();
                    return;
                }

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
            } catch (Exception e) {
                Log.e("LoginActivity", "Erreur initialisation vues", e);
                Toast.makeText(this, "Erreur d'initialisation: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("LoginActivity", "Erreur critique dans onCreate", e);
            Toast.makeText(this, "Erreur au démarrage: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void performLogin() {
        try {
            if (etEmail == null || etPassword == null) {
                Toast.makeText(this, "Erreur: champs non initialisés", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (authService == null) {
                Toast.makeText(this, "Erreur: service non disponible", Toast.LENGTH_SHORT).show();
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
                    try {
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
                                ed.putString("USER_PHONE", user.getNumTel());
                                ed.putString("USER_FACEBOOK", user.getFacebook());
                                ed.apply();
                            } catch (Exception e) {
                                Log.e("LOGIN_PREF", "Failed to persist user: " + e.getMessage(), e);
                            }

                            Toast.makeText(LoginActivity.this, "Connexion réussie! Bienvenue " + (user.getPrenom() != null ? user.getPrenom() : ""), Toast.LENGTH_LONG).show();
                            startDashboardActivity();

                        } else {
                            // Gérer les erreurs (ex: 401 Unauthorized, 404 Not Found)
                            Log.e("API_ERROR", "Login failed: " + response.code());
                            String errorMsg = "Échec de la connexion. Vérifiez vos identifiants.";
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e("LoginActivity", "Erreur body: " + errorBody);
                                    if (errorBody.contains("\"message\"")) {
                                        int start = errorBody.indexOf("\"message\"");
                                        if (start != -1) {
                                            int msgStart = errorBody.indexOf("\"", start + 10) + 1;
                                            int msgEnd = errorBody.indexOf("\"", msgStart);
                                            if (msgEnd > msgStart) {
                                                errorMsg = errorBody.substring(msgStart, msgEnd);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("LoginActivity", "Erreur parsing error body", e);
                            }
                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Erreur dans onResponse", e);
                        Toast.makeText(LoginActivity.this, "Erreur lors de la connexion: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    // Erreur de réseau ou de serveur
                    Log.e("NETWORK_ERROR", "Login failed: " + t.getMessage(), t);
                    String errorMsg = "Erreur réseau. Impossible de se connecter au serveur.";
                    if (t.getMessage() != null) {
                        if (t.getMessage().contains("Failed to connect")) {
                            errorMsg = "Impossible de se connecter au serveur. Vérifiez votre connexion internet.";
                        } else {
                            errorMsg = "Erreur: " + t.getMessage();
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e("LoginActivity", "Erreur dans performLogin", e);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startDashboardActivity() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        // Empêche de revenir à l'écran de connexion avec le bouton Retour
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
