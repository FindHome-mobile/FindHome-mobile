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
        String[] parts = name.split(" ", 2);
        String nom = parts.length > 0 ? parts[0] : name;
        String prenom = parts.length > 1 ? parts[1] : "";

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

        // Appel de l'API d'enregistrement (JSON)
        Call<User> call = authService.registerUser(body);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    Toast.makeText(RegisterActivity.this, "Compte créé! Veuillez vous connecter.", Toast.LENGTH_LONG).show();
                    // Optionnel: Connexion automatique ou retour à l'écran de connexion
                    finish();
                } else {
                    Log.e("API_ERROR", "Registration failed: " + response.code());
                    Toast.makeText(RegisterActivity.this, "Échec de l'enregistrement. Email peut-être déjà utilisé.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e("NETWORK_ERROR", "Registration failed: " + t.getMessage());
                Toast.makeText(RegisterActivity.this, "Erreur réseau. Impossible de se connecter au serveur.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
