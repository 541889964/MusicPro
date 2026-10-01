package com.music.app.adapter;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.music.app.R;
import com.music.app.model.Song;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.VH> {
    public interface OnItemClick { void onClick(Song s, int pos); }
    public interface OnItemLongClick { void onLongClick(Song s, int pos); }
    private final List<Song> data = new ArrayList<Song>();
    private final OnItemClick listener;
    private final OnItemLongClick longListener;
    private int lastAnimatedPos = -1;
    public SongAdapter(OnItemClick l) { this(l, null); }
    public SongAdapter(OnItemClick l, OnItemLongClick ll) { this.listener = l; this.longListener = ll; }
    public void setData(List<Song> list) {
        data.clear();
        if (list != null) data.addAll(list);
        lastAnimatedPos = -1;
        notifyDataSetChanged();
    }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull final VH h, int pos) {
        final Song s = data.get(pos);
        h.tvTitle.setText(s.title);
        h.tvSub.setText(s.artist + " · " + s.album);
        h.tvDur.setText(s.getDurationText());
        String name = WallpaperHelper.forSong(h.ivCover.getContext(), s.id);
        Bitmap bm = WallpaperHelper.load(h.ivCover.getContext(), name);
        if (bm != null) {
            h.ivCover.setImageBitmap(bm);
        } else if (s.cover != null && !s.cover.isEmpty()) {
            Glide.with(h.ivCover.getContext()).load(s.cover)
                .placeholder(R.drawable.cover_placeholder)
                .error(R.drawable.cover_placeholder).into(h.ivCover);
        } else {
            h.ivCover.setImageResource(R.drawable.cover_placeholder);
        }
        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (listener != null) listener.onClick(s, h.getAdapterPosition()); }
        });
        h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (longListener != null) { longListener.onLongClick(s, h.getAdapterPosition()); return true; }
                return false;
            }
        });
        if (pos > lastAnimatedPos) {
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationY(80f);
            h.itemView.animate().alpha(1f).translationY(0f).setDuration(420)
                .setInterpolator(new DecelerateInterpolator()).start();
            lastAnimatedPos = pos;
        }
    }
    @Override public int getItemCount() { return data.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub, tvDur;
        ImageView ivCover;
        VH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvSub = v.findViewById(R.id.tvSub);
            tvDur = v.findViewById(R.id.tvDur);
            ivCover = v.findViewById(R.id.ivCover);
        }
    }
}
