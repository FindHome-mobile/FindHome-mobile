package com.example.devmobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.devmobile.api.DemandeProprietaireService;
import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.models.DemandeProprietaire;
import com.example.devmobile.models.DemandeListResponse;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private DemandeProprietaireAdapter adapter;
    private DemandeProprietaireService demandeService;
    private String adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.toolbar_admin);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestion des Demandes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rv_demandes);
        progressBar = findViewById(R.id.progress);
        tvEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DemandeProprietaireAdapter();
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        adminId = prefs.getString("USER_ID", null);
        String userType = prefs.getString("USER_TYPE", "");

        if (!"admin".equals(userType)) {
            Toast.makeText(this, "Accès réservé aux administrateurs", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        demandeService = RetrofitClient.getInstance().getDemandeProprietaireService();
        loadDemandes();
    }

    private void loadDemandes() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        android.util.Log.d("AdminActivity", "Chargement des demandes...");
        android.util.Log.d("AdminActivity", "URL: " + RetrofitClient.getInstance().getBaseUrl() + "demandes-proprietaire?statut=en_attente");
        
        demandeService.getAllDemandes("en_attente").enqueue(new Callback<DemandeListResponse>() {
            @Override
            public void onResponse(@NonNull Call<DemandeListResponse> call, @NonNull Response<DemandeListResponse> response) {
                progressBar.setVisibility(View.GONE);
                android.util.Log.d("AdminActivity", "Réponse: " + response.code());
                android.util.Log.d("AdminActivity", "Headers: " + response.headers());
                
                if (response.isSuccessful() && response.body() != null) {
                    List<DemandeProprietaire> list = response.body().getDemandes();
                    if (list == null) list = new ArrayList<>();
                    android.util.Log.d("AdminActivity", "Nombre de demandes: " + list.size());
                    adapter.setItems(list);
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    if (list.isEmpty()) {
                        tvEmpty.setText("Aucune demande en attente");
                    }
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    String errorMsg = "Impossible de charger les demandes (" + response.code() + ")";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("AdminActivity", "Erreur body complet: " + errorBody);
                            
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
                            
                            if (response.code() == 404) {
                                errorMsg = "Endpoint non trouvé (404).\nVérifiez que le backend est démarré sur le port 5000.";
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AdminActivity", "Erreur parsing: " + e.getMessage(), e);
                        if (response.code() == 404) {
                            errorMsg = "Endpoint non trouvé (404). Vérifiez que le backend est démarré.";
                        }
                    }
                    tvEmpty.setText(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<DemandeListResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                android.util.Log.e("AdminActivity", "Erreur réseau complète", t);
                
                String errorMsg = "Erreur réseau";
                if (t.getMessage() != null) {
                    android.util.Log.e("AdminActivity", "Message d'erreur: " + t.getMessage());
                    if (t.getMessage().contains("404") || t.getMessage().contains("Not Found")) {
                        errorMsg = "Endpoint non trouvé (404).\nVérifiez que:\n1. Le backend est démarré\n2. L'URL est correcte: " + RetrofitClient.getInstance().getBaseUrl();
                    } else if (t.getMessage().contains("Failed to connect") || t.getMessage().contains("Unable to resolve host")) {
                        errorMsg = "Impossible de se connecter au serveur.\nVérifiez l'URL: " + RetrofitClient.getInstance().getBaseUrl();
                    } else {
                        errorMsg = "Erreur: " + t.getMessage();
                    }
                }
                tvEmpty.setText(errorMsg);
            }
        });
    }

    private void approveDemande(String demandeId) {
        JsonObject body = new JsonObject();
        body.addProperty("adminId", adminId);

        demandeService.approveDemande(demandeId, body).enqueue(new Callback<DemandeProprietaire>() {
            @Override
            public void onResponse(@NonNull Call<DemandeProprietaire> call, @NonNull Response<DemandeProprietaire> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminActivity.this, "Demande approuvée", Toast.LENGTH_SHORT).show();
                    loadDemandes();
                } else {
                    Toast.makeText(AdminActivity.this, "Erreur: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DemandeProprietaire> call, @NonNull Throwable t) {
                Toast.makeText(AdminActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectDemande(String demandeId) {
        JsonObject body = new JsonObject();
        body.addProperty("adminId", adminId);

        demandeService.rejectDemande(demandeId, body).enqueue(new Callback<DemandeProprietaire>() {
            @Override
            public void onResponse(@NonNull Call<DemandeProprietaire> call, @NonNull Response<DemandeProprietaire> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminActivity.this, "Demande rejetée", Toast.LENGTH_SHORT).show();
                    loadDemandes();
                } else {
                    Toast.makeText(AdminActivity.this, "Erreur: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DemandeProprietaire> call, @NonNull Throwable t) {
                Toast.makeText(AdminActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class DemandeProprietaireAdapter extends RecyclerView.Adapter<DemandeVH> {
        private final List<DemandeProprietaire> items = new ArrayList<>();

        void setItems(List<DemandeProprietaire> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DemandeVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_demande, parent, false);
            return new DemandeVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DemandeVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() { return items.size(); }
    }

    private class DemandeVH extends RecyclerView.ViewHolder {
        private final TextView tvName, tvEmail;
        private final android.widget.Button btnApprove, btnReject;

        DemandeVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }

        void bind(DemandeProprietaire demande) {
            if (demande.getUtilisateur() != null) {
                String name = demande.getUtilisateur().getNom() + " " + demande.getUtilisateur().getPrenom();
                tvName.setText(name);
                tvEmail.setText(demande.getUtilisateur().getEmail());
            }

            btnApprove.setOnClickListener(v -> approveDemande(demande.getId()));
            btnReject.setOnClickListener(v -> rejectDemande(demande.getId()));
        }
    }
}

