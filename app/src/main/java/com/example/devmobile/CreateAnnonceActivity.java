package com.example.devmobile;

import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.devmobile.api.AnnonceService;
import com.example.devmobile.api.RetrofitClient;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateAnnonceActivity extends AppCompatActivity {

    private EditText etTitre, etDescription, etLocalisation, etPrix, etNbPieces, etSurface, etTypeBien;
    private android.widget.CheckBox cbMeublee, cbAscenseur, cbParking, cbClimatisation, cbChauffage, cbBalcon, cbJardin, cbPiscine;
    private EditText etEtage, etTelephone;
    private View btnCreate;
    private ProgressBar progressBar;
    private AnnonceService annonceService;
    private String proprietaireId;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private List<Uri> selectedImages = new ArrayList<>();
    private RecyclerView rvSelectedImages;
    private SelectedImagesAdapter imagesAdapter;
    private TextView tvImageCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_annonce);

        Toolbar toolbar = findViewById(R.id.toolbar_create);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Créer une annonce");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        proprietaireId = prefs.getString("USER_ID", null);
        String userType = prefs.getString("USER_TYPE", "");
        String userPhone = prefs.getString("USER_NUMTEL", "");

        if (!"proprietaire".equals(userType)) {
            Toast.makeText(this, "Seuls les propriétaires peuvent créer des annonces", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        
        // Pré-remplir le téléphone si disponible
        if (etTelephone != null) {
            if (!TextUtils.isEmpty(userPhone)) {
                etTelephone.setText(userPhone);
                android.util.Log.d("CreateAnnonce", "Téléphone pré-rempli: " + userPhone);
            } else {
                android.util.Log.w("CreateAnnonce", "Aucun téléphone dans SharedPreferences");
            }
        } else {
            android.util.Log.e("CreateAnnonce", "etTelephone est null après initViews");
        }
        
        annonceService = RetrofitClient.getInstance().getAnnonceService();
        setupImagePicker();
    }

    private void initViews() {
        etTitre = findViewById(R.id.et_titre);
        etDescription = findViewById(R.id.et_description);
        etLocalisation = findViewById(R.id.et_localisation);
        etPrix = findViewById(R.id.et_prix);
        etNbPieces = findViewById(R.id.et_nb_pieces);
        etSurface = findViewById(R.id.et_surface);
        etTypeBien = findViewById(R.id.et_type_bien);
        etEtage = findViewById(R.id.et_etage);
        etTelephone = findViewById(R.id.et_telephone);
        cbMeublee = findViewById(R.id.cb_meublee);
        cbAscenseur = findViewById(R.id.cb_ascenseur);
        cbParking = findViewById(R.id.cb_parking);
        cbClimatisation = findViewById(R.id.cb_climatisation);
        cbChauffage = findViewById(R.id.cb_chauffage);
        cbBalcon = findViewById(R.id.cb_balcon);
        cbJardin = findViewById(R.id.cb_jardin);
        cbPiscine = findViewById(R.id.cb_piscine);
        btnCreate = findViewById(R.id.btn_create);
        progressBar = findViewById(R.id.progress);
        rvSelectedImages = findViewById(R.id.rv_selected_images);
        tvImageCount = findViewById(R.id.tv_image_count);

        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imagesAdapter = new SelectedImagesAdapter();
        rvSelectedImages.setAdapter(imagesAdapter);
        
        updateImageCount();

        btnCreate.setOnClickListener(v -> createAnnonce());
        findViewById(R.id.btn_add_images).setOnClickListener(v -> {
            if (selectedImages.size() >= 10) {
                Toast.makeText(this, "Maximum 10 images autorisées", Toast.LENGTH_SHORT).show();
            } else {
                imagePickerLauncher.launch("image/*");
            }
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && selectedImages.size() < 10) {
                selectedImages.add(uri);
                imagesAdapter.notifyItemInserted(selectedImages.size() - 1);
                updateImageCount();
                Toast.makeText(this, "Image ajoutée (" + selectedImages.size() + "/10)", Toast.LENGTH_SHORT).show();
            } else if (selectedImages.size() >= 10) {
                Toast.makeText(this, "Maximum 10 images autorisées", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateImageCount() {
        if (tvImageCount != null) {
            tvImageCount.setText(selectedImages.size() + "/10");
        }
    }

    private void createAnnonce() {
        // Vérifier que le propriétaire est connecté
        if (TextUtils.isEmpty(proprietaireId)) {
            Toast.makeText(this, "Erreur: Utilisateur non connecté", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String titre = etTitre.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String localisation = etLocalisation.getText().toString().trim();
        String prix = etPrix.getText().toString().trim();
        String nbPieces = etNbPieces.getText().toString().trim();
        String surface = etSurface.getText().toString().trim();
        String typeBien = etTypeBien.getText().toString().trim();
        
        // Vérifier que le champ téléphone est initialisé et lire sa valeur
        String telephone = "";
        if (etTelephone == null) {
            android.util.Log.e("CreateAnnonce", "etTelephone est null - réinitialisation");
            etTelephone = findViewById(R.id.et_telephone);
        }
        
        if (etTelephone != null) {
            CharSequence telText = etTelephone.getText();
            if (telText != null) {
                telephone = telText.toString().trim();
                // Nettoyer le téléphone (enlever seulement les espaces, garder le + et les chiffres)
                telephone = telephone.replaceAll("\\s+", "");
                android.util.Log.d("CreateAnnonce", "Téléphone lu depuis le champ: '" + telephone + "' (longueur: " + telephone.length() + ")");
                
                // Vérifier que le téléphone n'est pas vide après nettoyage
                if (telephone.isEmpty()) {
                    android.util.Log.w("CreateAnnonce", "Téléphone vide après nettoyage");
                }
            } else {
                android.util.Log.w("CreateAnnonce", "getText() retourne null");
                telephone = "";
            }
        } else {
            android.util.Log.e("CreateAnnonce", "Impossible de trouver le champ téléphone");
            Toast.makeText(this, "Erreur: Champ téléphone non trouvé", Toast.LENGTH_LONG).show();
            return;
        }

        // Validation des champs obligatoires
        boolean hasError = false;
        if (TextUtils.isEmpty(titre)) {
            etTitre.setError("Le titre est requis");
            hasError = true;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("La description est requise");
            hasError = true;
        }
        if (TextUtils.isEmpty(localisation)) {
            etLocalisation.setError("La localisation est requise");
            hasError = true;
        }
        if (TextUtils.isEmpty(prix)) {
            etPrix.setError("Le prix est requis");
            hasError = true;
        }
        if (TextUtils.isEmpty(nbPieces)) {
            etNbPieces.setError("Le nombre de pièces est requis");
            hasError = true;
        }
        if (TextUtils.isEmpty(surface)) {
            etSurface.setError("La surface est requise");
            hasError = true;
        }
        if (TextUtils.isEmpty(typeBien)) {
            etTypeBien.setError("Le type de bien est requis");
            hasError = true;
        }
        if (TextUtils.isEmpty(telephone)) {
            if (etTelephone != null) {
                etTelephone.setError("Le numéro de téléphone est requis");
            }
            Toast.makeText(this, "Veuillez remplir le numéro de téléphone", Toast.LENGTH_SHORT).show();
            android.util.Log.w("CreateAnnonce", "Téléphone vide détecté");
            hasError = true;
        }
        
        if (hasError) {
            android.util.Log.d("CreateAnnonce", "Validation échouée - champs manquants");
            return;
        }
        
        android.util.Log.d("CreateAnnonce", "Validation réussie - Téléphone: " + telephone);

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Veuillez ajouter au moins une image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnCreate.setEnabled(false);

        android.util.Log.d("CreateAnnonce", "Création annonce - URL: " + RetrofitClient.getInstance().getBaseUrl() + "annonces");

        try {
            // Préparer les images avec gestion d'erreur
            List<MultipartBody.Part> imageParts = new ArrayList<>();
            for (int i = 0; i < selectedImages.size(); i++) {
                Uri uri = selectedImages.get(i);
                if (uri == null) {
                    android.util.Log.w("CreateAnnonce", "URI null à l'index " + i);
                    continue;
                }
                
                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    if (inputStream == null) {
                        android.util.Log.e("CreateAnnonce", "Impossible d'ouvrir l'input stream pour l'image " + i);
                        continue;
                    }
                    
                    byte[] bytes = readAllBytes(inputStream);
                    if (bytes == null || bytes.length == 0) {
                        android.util.Log.e("CreateAnnonce", "Image vide à l'index " + i);
                        continue;
                    }
                    
                    String mime = getContentResolver().getType(uri);
                    if (mime == null || !mime.startsWith("image/")) {
                        mime = "image/jpeg";
                    }
                    
                    String fileName = "image_" + i + ".jpg";
                    RequestBody imageRB = RequestBody.create(MediaType.parse(mime), bytes);
                    MultipartBody.Part imagePart = MultipartBody.Part.createFormData("images", fileName, imageRB);
                    imageParts.add(imagePart);
                    android.util.Log.d("CreateAnnonce", "Image " + i + " préparée: " + bytes.length + " bytes");
                } catch (Exception e) {
                    android.util.Log.e("CreateAnnonce", "Erreur lors du traitement de l'image " + i, e);
                    Toast.makeText(this, "Erreur lors du traitement de l'image " + (i + 1), Toast.LENGTH_SHORT).show();
                }
            }
            
            if (imageParts.isEmpty()) {
                progressBar.setVisibility(View.GONE);
                btnCreate.setEnabled(true);
                Toast.makeText(this, "Impossible de traiter les images. Veuillez réessayer.", Toast.LENGTH_LONG).show();
                return;
            }

            // Créer les RequestBody pour les champs texte
            RequestBody rbTitre = RequestBody.create(MediaType.parse("text/plain"), titre);
            RequestBody rbDescription = RequestBody.create(MediaType.parse("text/plain"), description);
            RequestBody rbLocalisation = RequestBody.create(MediaType.parse("text/plain"), localisation);
            RequestBody rbPrix = RequestBody.create(MediaType.parse("text/plain"), prix);
            RequestBody rbNbPieces = RequestBody.create(MediaType.parse("text/plain"), nbPieces);
            RequestBody rbSurface = RequestBody.create(MediaType.parse("text/plain"), surface);
            RequestBody rbTypeBien = RequestBody.create(MediaType.parse("text/plain"), typeBien);
            RequestBody rbProprietaire = RequestBody.create(MediaType.parse("text/plain"), proprietaireId);
            RequestBody rbMeublee = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(cbMeublee.isChecked()));
            RequestBody rbAscenseur = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(cbAscenseur.isChecked()));
            RequestBody rbParking = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(cbParking.isChecked()));
            RequestBody rbClimatisation = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(cbClimatisation.isChecked()));
            RequestBody rbChauffage = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(cbChauffage.isChecked()));
            RequestBody rbBalcon = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(cbBalcon.isChecked()));
            RequestBody rbJardin = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(cbJardin.isChecked()));
            RequestBody rbPiscine = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(cbPiscine.isChecked()));
            
            RequestBody rbEtage = null;
            if (!TextUtils.isEmpty(etEtage.getText())) {
                rbEtage = RequestBody.create(MediaType.parse("text/plain"), etEtage.getText().toString());
            } else {
                rbEtage = RequestBody.create(MediaType.parse("text/plain"), "");
            }
            
            // S'assurer que le téléphone n'est pas vide
            if (TextUtils.isEmpty(telephone)) {
                progressBar.setVisibility(View.GONE);
                btnCreate.setEnabled(true);
                etTelephone.setError("Le numéro de téléphone est requis");
                Toast.makeText(this, "Veuillez remplir le numéro de téléphone", Toast.LENGTH_LONG).show();
                android.util.Log.e("CreateAnnonce", "Téléphone vide avant envoi");
                return;
            }
            
            // Vérifier une dernière fois que le téléphone n'est pas vide avant de créer le RequestBody
            if (TextUtils.isEmpty(telephone)) {
                progressBar.setVisibility(View.GONE);
                btnCreate.setEnabled(true);
                if (etTelephone != null) {
                    etTelephone.setError("Le numéro de téléphone est requis");
                    etTelephone.requestFocus();
                }
                Toast.makeText(this, "Veuillez remplir le numéro de téléphone", Toast.LENGTH_LONG).show();
                android.util.Log.e("CreateAnnonce", "Téléphone vide détecté juste avant l'envoi");
                return;
            }
            
            RequestBody rbTelephone = RequestBody.create(MediaType.parse("text/plain"), telephone);

            android.util.Log.d("CreateAnnonce", "=== Données à envoyer ===");
            android.util.Log.d("CreateAnnonce", "Images: " + imageParts.size());
            android.util.Log.d("CreateAnnonce", "Titre: " + titre);
            android.util.Log.d("CreateAnnonce", "Proprietaire ID: " + proprietaireId);
            android.util.Log.d("CreateAnnonce", "Prix: " + prix);
            android.util.Log.d("CreateAnnonce", "Téléphone: '" + telephone + "' (longueur: " + telephone.length() + ")");
            android.util.Log.d("CreateAnnonce", "Meublée: " + cbMeublee.isChecked());

            // Utiliser varargs au lieu d'un tableau
            annonceService.createAnnonce(
                rbTitre, rbDescription, rbLocalisation, rbPrix, rbNbPieces, rbSurface, rbTypeBien,
                rbProprietaire, rbMeublee, rbAscenseur, rbParking, rbClimatisation, rbChauffage,
                rbBalcon, rbJardin, rbPiscine, rbEtage, rbTelephone, imageParts.toArray(new MultipartBody.Part[0])
            ).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    progressBar.setVisibility(View.GONE);
                    btnCreate.setEnabled(true);
                    android.util.Log.d("CreateAnnonce", "Réponse: " + response.code());
                    
                    if (response.isSuccessful()) {
                        Toast.makeText(CreateAnnonceActivity.this, "Annonce créée avec succès", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String errorMsg = "Erreur " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                android.util.Log.e("CreateAnnonce", "Erreur body: " + errorBody);
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
                                
                                // Si l'erreur concerne le téléphone, mettre le focus sur le champ
                                if (errorMsg.toLowerCase().contains("téléphone") || errorMsg.toLowerCase().contains("telephone")) {
                                    android.util.Log.e("CreateAnnonce", "Erreur téléphone détectée: " + errorMsg);
                                    if (etTelephone != null) {
                                        etTelephone.setError("Le numéro de téléphone est requis");
                                        etTelephone.requestFocus();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("CreateAnnonce", "Erreur parsing: " + e.getMessage(), e);
                        }
                        if (response.code() == 404) {
                            errorMsg = "Endpoint non trouvé (404). Vérifiez que le backend est démarré.";
                        }
                        Toast.makeText(CreateAnnonceActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnCreate.setEnabled(true);
                    android.util.Log.e("CreateAnnonce", "Erreur réseau", t);
                    String errorMsg = "Erreur réseau";
                    if (t.getMessage() != null) {
                        if (t.getMessage().contains("404") || t.getMessage().contains("Not Found")) {
                            errorMsg = "Endpoint non trouvé (404). Vérifiez que le backend est démarré sur le port 5000.";
                        } else if (t.getMessage().contains("Failed to connect")) {
                            errorMsg = "Impossible de se connecter au serveur. Vérifiez l'URL: " + RetrofitClient.getInstance().getBaseUrl();
                        } else {
                            errorMsg = "Erreur: " + t.getMessage();
                        }
                    }
                    Toast.makeText(CreateAnnonceActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnCreate.setEnabled(true);
            android.util.Log.e("CreateAnnonce", "Erreur inattendue", e);
            String errorMsg = "Erreur: " + (e.getMessage() != null ? e.getMessage() : "Erreur inconnue");
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        if (is == null) {
            throw new IOException("InputStream est null");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
        } catch (IOException e) {
            try {
                buffer.close();
            } catch (IOException ignored) {}
            throw e;
        }
        return buffer.toByteArray();
    }

    private class SelectedImagesAdapter extends RecyclerView.Adapter<ImageVH> {
        @NonNull
        @Override
        public ImageVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_image, parent, false);
            return new ImageVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageVH holder, int position) {
            holder.bind(selectedImages.get(position), position);
        }

        @Override
        public int getItemCount() {
            return selectedImages.size();
        }
    }

    private class ImageVH extends RecyclerView.ViewHolder {
        private final android.widget.ImageView ivImage;
        private final com.google.android.material.button.MaterialButton btnRemove;

        ImageVH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        void bind(Uri uri, int position) {
            if (uri == null) {
                android.util.Log.w("CreateAnnonce", "URI null à la position " + position);
                return;
            }
            
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    android.graphics.Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        ivImage.setImageBitmap(bitmap);
                    } else {
                        android.util.Log.e("CreateAnnonce", "Bitmap null pour l'image " + position);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("CreateAnnonce", "Erreur chargement image", e);
            }

            btnRemove.setOnClickListener(v -> {
                try {
                    if (position >= 0 && position < selectedImages.size()) {
                        selectedImages.remove(position);
                        imagesAdapter.notifyItemRemoved(position);
                        imagesAdapter.notifyItemRangeChanged(position, selectedImages.size());
                        updateImageCount();
                        Toast.makeText(CreateAnnonceActivity.this, "Image supprimée", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("CreateAnnonce", "Erreur suppression image", e);
                }
            });
        }
    }
}

