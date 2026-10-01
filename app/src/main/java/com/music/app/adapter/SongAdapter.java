package com.music.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.music.app.R;
import com.music.app.model.Song;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.VH> {

    public interface OnClick { void click(Song s, int pos); }

    private final List<Song> data = new ArrayList<>();
    private final OnClick l;
    private int last = -1;

    public SongAdapter(OnClick l) { this.l = l; }

    public void setData(List<Song> list) {
        data.clear();
        if (list != null) data.addAll(list);
        last = -1;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext())
            .inflate(R.layout.item_song, p, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Song s = data.get(pos);
        h.title.setText(s.title);
        h.sub.setText(s.artist + " · " + s.album);
        h.dur.setText(s.getDur());

        h.itemView.setOnClickListener(v -> {
            int p = h.getAdapterPosition();
            if (p != RecyclerView.NO_POSITION && l != null) l.click(s, p);
        });

        h.itemView.setAlpha(1f);
        h.itemView.setTranslationY(0f);
        if (pos > last) {
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationY(40f);
            h.itemView.animate().alpha(1f).translationY(0f)
                .setDuration(350).start();
            last = pos;
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, sub, dur;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            sub = v.findViewById(R.id.tvSub);
            dur = v.findViewById(R.id.tvDur);
        }
    }
}
