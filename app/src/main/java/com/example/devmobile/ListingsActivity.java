package com.example.devmobile;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.devmobile.api.AnnonceService;
import com.example.devmobile.api.RetrofitClient;
import com.example.devmobile.models.Annonce;
import com.example.devmobile.models.AnnonceListResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private AnnoncesAdapter adapter;

    // Filtres
    private com.google.android.material.textfield.TextInputEditText etLocalisation;
    private com.google.android.material.textfield.TextInputEditText etPrixMin;
    private com.google.android.material.textfield.TextInputEditText etPrixMax;
    private com.google.android.material.button.MaterialButton btnFilter;
    private com.google.android.material.button.MaterialButton btnClearFilters;

    // Valeurs des filtres actuels
    private String currentLocalisation = null;
    private String currentPrixMin = null;
    private String currentPrixMax = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ListingsActivity", "onCreate appelé");
        try {
            setContentView(R.layout.activity_listings);
            android.util.Log.d("ListingsActivity", "setContentView réussi");
        } catch (Exception e) {
            android.util.Log.e("ListingsActivity", "Erreur dans setContentView", e);
            throw e;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_listings);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Annonces");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.rv_annonces);
        progressBar = findViewById(R.id.progress);
        tvEmpty = findViewById(R.id.tv_empty);

        // Initialiser les vues de filtrage
        etLocalisation = findViewById(R.id.et_localisation);
        etPrixMin = findViewById(R.id.et_prix_min);
        etPrixMax = findViewById(R.id.et_prix_max);
        btnFilter = findViewById(R.id.btn_filter);
        btnClearFilters = findViewById(R.id.btn_clear_filters);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnnoncesAdapter();
        recyclerView.setAdapter(adapter);

        // Configurer les boutons de filtrage
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> applyFilters());
        }
        if (btnClearFilters != null) {
            btnClearFilters.setOnClickListener(v -> clearFilters());
        }

        // TODO: Ajouter la fonctionnalité de recherche avec la touche Entrée si souhaité

        loadAnnonces();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les annonces quand on revient sur cette activité
        loadAnnonces();
    }

    private void loadAnnonces() {
        loadAnnoncesWithFilters(currentLocalisation, currentPrixMin, currentPrixMax);
    }

    private void loadAnnoncesWithFilters(String localisation, String prixMin, String prixMax) {
        android.util.Log.d("ListingsActivity", "loadAnnoncesWithFilters appelé");
        // Initialiser le contexte pour Retrofit
        RetrofitClient.setContext(this);
        android.util.Log.d("ListingsActivity", "RetrofitClient.setContext réussi");

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        RetrofitClient client = RetrofitClient.getInstance();
        android.util.Log.d("ListingsActivity", "RetrofitClient.getInstance réussi");
        AnnonceService service = client.getAnnonceService();
        android.util.Log.d("ListingsActivity", "AnnonceService créé");
        Log.d("ListingsActivity", "Chargement des annonces avec filtres - localisation: " + localisation + ", prixMin: " + prixMin + ", prixMax: " + prixMax);
        service.getAnnonces(localisation, prixMin, prixMax, null, null, null).enqueue(new Callback<AnnonceListResponse>() {
            @Override
            public void onResponse(@NonNull Call<AnnonceListResponse> call, @NonNull Response<AnnonceListResponse> response) {
                progressBar.setVisibility(View.GONE);
                Log.d("ListingsActivity", "Réponse reçue: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<Annonce> list = response.body().getAnnonces();
                    if (list == null) list = new ArrayList<>();
                    Log.d("ListingsActivity", "Nombre d'annonces: " + list.size());
                    adapter.setItems(list);
                    recyclerView.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    String errorMsg = "Impossible de charger les annonces (" + response.code() + ")";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("ListingsActivity", "Erreur body: " + errorBody);
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
                        Log.e("ListingsActivity", "Erreur parsing: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AnnonceListResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                Log.e("ListingsActivity", "Erreur réseau: " + t.getMessage(), t);
            }
        });
    }

    private static class AnnoncesAdapter extends RecyclerView.Adapter<AnnonceVH> {
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

    private static class AnnonceVH extends RecyclerView.ViewHolder {
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
                        try {
                            String base64Image = firstImage.substring(firstImage.indexOf(",") + 1);
                            byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            ivImage.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            android.util.Log.e("ListingsActivity", "Erreur chargement image", e);
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
            
            itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(itemView.getContext(), AnnonceDetailActivity.class);
                intent.putExtra("annonceId", a.getId());
                itemView.getContext().startActivity(intent);
            });
        }
    }

    private void applyFilters() {
        // Récupérer les valeurs des champs
        String localisation = etLocalisation != null ? etLocalisation.getText().toString().trim() : "";
        String prixMin = etPrixMin != null ? etPrixMin.getText().toString().trim() : "";
        String prixMax = etPrixMax != null ? etPrixMax.getText().toString().trim() : "";

        // Convertir les chaînes vides en null
        localisation = localisation.isEmpty() ? null : localisation;
        prixMin = prixMin.isEmpty() ? null : prixMin;
        prixMax = prixMax.isEmpty() ? null : prixMax;

        // Validation basique des prix
        if (prixMin != null && prixMax != null) {
            try {
                double min = Double.parseDouble(prixMin);
                double max = Double.parseDouble(prixMax);
                if (min > max) {
                    android.widget.Toast.makeText(this, "Le prix minimum ne peut pas être supérieur au prix maximum", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                android.widget.Toast.makeText(this, "Veuillez entrer des prix valides", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Mettre à jour les filtres actuels
        currentLocalisation = localisation;
        currentPrixMin = prixMin;
        currentPrixMax = prixMax;

        // Recharger les annonces avec les nouveaux filtres
        loadAnnonces();

        // Masquer le clavier
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void clearFilters() {
        // Vider tous les champs
        if (etLocalisation != null) etLocalisation.setText("");
        if (etPrixMin != null) etPrixMin.setText("");
        if (etPrixMax != null) etPrixMax.setText("");

        // Réinitialiser les filtres actuels
        currentLocalisation = null;
        currentPrixMin = null;
        currentPrixMax = null;

        // Recharger toutes les annonces
        loadAnnonces();

        // Masquer le clavier
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        android.widget.Toast.makeText(this, "Filtres effacés", android.widget.Toast.LENGTH_SHORT).show();
    }
}
