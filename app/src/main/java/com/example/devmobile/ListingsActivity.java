package com.example.devmobile;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listings);

        recyclerView = findViewById(R.id.rv_annonces);
        progressBar = findViewById(R.id.progress);
        tvEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnnoncesAdapter();
        recyclerView.setAdapter(adapter);

        loadAnnonces();
    }

    private void loadAnnonces() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        RetrofitClient client = RetrofitClient.getInstance();
        AnnonceService service = client.getAnnonceService();
        service.getAnnonces(null, null, null, null, null, null).enqueue(new Callback<AnnonceListResponse>() {
            @Override
            public void onResponse(@NonNull Call<AnnonceListResponse> call, @NonNull Response<AnnonceListResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Annonce> list = response.body().getAnnonces();
                    if (list == null) list = new ArrayList<>();
                    adapter.setItems(list);
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Impossible de charger les annonces (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AnnonceListResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Erreur réseau: " + t.getMessage());
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
        private final TextView tvTitle;
        private final TextView tvSubtitle;

        AnnonceVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
        }

        void bind(Annonce a) {
            tvTitle.setText(a.getTitre());
            String sub = (a.getLocalisation() != null ? a.getLocalisation() : "") + " • " + a.getPrix() + " DT";
            tvSubtitle.setText(sub);
        }
    }
}
