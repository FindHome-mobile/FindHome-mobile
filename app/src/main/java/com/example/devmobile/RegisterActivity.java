package com.example.devmobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.devmobile.api.AuthService;
import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.models.User;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private AutoCompleteTextView etType;
    private EditText etPhone, etFacebook;
    private AutoCompleteTextView etLocation;
    private Button btnRegister;
    private TextView tvLoginPrompt;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialiser le contexte pour Retrofit
        RetrofitClient.setContext(this);

        authService = RetrofitClient.getInstance().getAuthService();

        // Initialisation des vues
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etType = findViewById(R.id.et_type);
        etPhone = findViewById(R.id.et_phone);
        etFacebook = findViewById(R.id.et_facebook);
        etLocation = findViewById(R.id.et_location);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginPrompt = findViewById(R.id.tv_login_prompt);

        // Valeur par défaut du type
        if (etType != null) {
            etType.setText("client", false);
        }

        // Suggestions pour Gouvernorat / Ville (AutoComplete)
        if (etLocation != null) {
            String[] GOVS = new String[]{
                    "Ariana","Béja","Ben Arous","Bizerte","Gabès","Gafsa","Jendouba",
                    "Kairouan","Kasserine","Kébili","Le Kef","Mahdia","La Manouba",
                    "Médenine","Monastir","Nabeul","Sfax","Sidi Bouzid","Siliana",
                    "Sousse","Tataouine","Tozeur","Tunis","Zaghouan"
            };
            ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_list_item_1, GOVS
            );
            etLocation.setAdapter(locationAdapter);
            etLocation.setThreshold(1);
        }

        // Gestionnaires d'événements
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        tvLoginPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retourner à l'activité de connexion
                finish();
            }
        });
    }

    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String type = etType != null && etType.getText() != null ? etType.getText().toString().trim() : "client";
        String numTel = etPhone != null ? etPhone.getText().toString().trim() : "";
        String facebook = etFacebook != null ? etFacebook.getText().toString().trim() : "";
        String location = etLocation != null ? etLocation.getText().toString().trim() : "";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Adapter les champs au backend: nom, prenom, email, motDePasse, type (+ proprietaire fields)
        // Si l'utilisateur entre un seul nom (ex: "hazem"), on le met dans nom et prenom
        // Si l'utilisateur entre deux mots (ex: "hazem ben", on divise en nom et prenom
        String[] parts = name.trim().split("\\s+", 2);
        String nom;
        String prenom;
        
        if (parts.length == 1) {
            // Un seul mot: on le met dans nom et prenom (le backend exige les deux)
            nom = parts[0];
            prenom = parts[0]; // Même valeur pour les deux
        } else if (parts.length == 2) {
            // Deux mots: premier = nom, deuxième = prenom
            nom = parts[0];
            prenom = parts[1];
        } else {
            // Par défaut (ne devrait pas arriver)
            nom = name;
            prenom = name;
        }

        // Construire le corps JSON exactement comme l'exemple backend
        JsonObject body = new JsonObject();
        body.addProperty("nom", nom);
        body.addProperty("prenom", prenom);
        body.addProperty("email", email);
        body.addProperty("motDePasse", password);
        body.addProperty("type", type.isEmpty() ? "client" : type);
        if (type.equalsIgnoreCase("proprietaire")) {
            body.addProperty("numTel", numTel);
            body.addProperty("facebook", facebook);
            body.addProperty("location", location);
        }

        // Désactiver le bouton pour éviter les doubles clics
        btnRegister.setEnabled(false);
        
        // Appel de l'API d'enregistrement (JSON)
        Log.d("RegisterActivity", "Envoi de l'inscription: " + body.toString());
        Call<User> call = authService.registerUser(body);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                btnRegister.setEnabled(true);
                Log.d("RegisterActivity", "Réponse inscription: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    Log.d("RegisterActivity", "Inscription réussie pour: " + user.getEmail());
                    Toast.makeText(RegisterActivity.this, "Compte créé avec succès! Veuillez vous connecter.", Toast.LENGTH_LONG).show();
                    // Retour à l'écran de connexion
                    finish();
                } else {
                    Log.e("API_ERROR", "Registration failed: " + response.code());
                    String errorMsg = "Échec de l'enregistrement";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("RegisterActivity", "Erreur body: " + errorBody);
                            
                            // Essayer d'extraire le message d'erreur du JSON
                            if (errorBody.contains("\"message\"")) {
                                int start = errorBody.indexOf("\"message\"");
                                if (start != -1) {
                                    int msgStart = errorBody.indexOf("\"", start + 10) + 1;
                                    int msgEnd = errorBody.indexOf("\"", msgStart);
                                    if (msgEnd > msgStart) {
                                        errorMsg = errorBody.substring(msgStart, msgEnd);
                                    }
                                }
                            } else if (errorBody.contains("message")) {
                                // Format alternatif
                                int start = errorBody.indexOf("message");
                                if (start != -1) {
                                    int msgStart = errorBody.indexOf(":", start) + 1;
                                    int msgEnd = errorBody.indexOf(",", msgStart);
                                    if (msgEnd == -1) msgEnd = errorBody.indexOf("}", msgStart);
                                    if (msgEnd > msgStart) {
                                        String msg = errorBody.substring(msgStart, msgEnd).trim();
                                        if (msg.startsWith("\"")) msg = msg.substring(1);
                                        if (msg.endsWith("\"")) msg = msg.substring(0, msg.length() - 1);
                                        errorMsg = msg;
                                    }
                                }
                            }
                            
                            // Messages spécifiques selon le code
                            if (response.code() == 400) {
                                if (errorMsg.contains("déjà") || errorMsg.contains("existe")) {
                                    errorMsg = "Cet email est déjà utilisé. Veuillez utiliser un autre email.";
                                }
                            } else if (response.code() == 500) {
                                errorMsg = "Erreur serveur. Veuillez réessayer plus tard.";
                            }
                        }
                    } catch (Exception e) {
                        Log.e("RegisterActivity", "Erreur parsing: " + e.getMessage());
                        if (response.code() == 400) {
                            errorMsg = "Données invalides. Vérifiez vos informations.";
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                btnRegister.setEnabled(true);
                Log.e("NETWORK_ERROR", "Registration failed: " + t.getMessage(), t);
                String errorMsg = "Erreur réseau";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("Failed to connect") || t.getMessage().contains("Unable to resolve host")) {
                        errorMsg = "Impossible de se connecter au serveur. Vérifiez votre connexion internet.";
                    } else {
                        errorMsg = "Erreur réseau: " + t.getMessage();
                    }
                }
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
