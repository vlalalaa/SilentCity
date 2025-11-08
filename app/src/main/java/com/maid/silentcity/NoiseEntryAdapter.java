package com.maid.silentcity;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoiseEntryAdapter extends RecyclerView.Adapter<NoiseEntryAdapter.ViewHolder> {

    private final List<NoiseEntry> dataList;
    private final Context context;

    public NoiseEntryAdapter(Context context, List<NoiseEntry> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_noise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoiseEntry entry = dataList.get(position);

        // Форматування часу
        String formattedDate = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(new Date(entry.getTimestamp()));

        // Встановлення даних
        holder.tvDate.setText(formattedDate);
        holder.tvAvgNoise.setText(String.format("%s дБ", entry.getAvgNoise()));

        // Обробник натискання елемента
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);

            // Передача всіх необхідних даних на сторінку деталей
            intent.putExtra("TIMESTAMP", entry.getTimestamp());
            intent.putExtra("AUTHOR_EMAIL", entry.getAuthorEmail());
            intent.putExtra("CAUSE", entry.getCause());
            intent.putExtra("AVG_NOISE", entry.getAvgNoise());
            intent.putExtra("MAX_NOISE", entry.getMaxNoise());
            intent.putExtra("MIN_NOISE", entry.getMinNoise());
            intent.putExtra("LATITUDE", entry.getLatitude());
            intent.putExtra("LONGITUDE", entry.getLongitude());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDate;
        final TextView tvAvgNoise;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_list_date);
            tvAvgNoise = itemView.findViewById(R.id.tv_list_avg_noise);
        }
    }
}