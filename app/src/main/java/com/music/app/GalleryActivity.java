package com.music.app;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.music.app.util.NiceToast;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import java.util.List;
public class GalleryActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_gallery);
        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        List<String> all = WallpaperHelper.listSucai(this);
        TextView tvCnt = findViewById(R.id.tvGalleryCount);
        if (tvCnt != null) tvCnt.setText("共 " + all.size() + " 张素材");
        RecyclerView rv = findViewById(R.id.rvGallery);
        if (rv != null) {
            rv.setLayoutManager(new GridLayoutManager(this, 2));
            rv.setAdapter(new GalleryAdapter(this, all));
        }
        View header = findViewById(R.id.galleryHeader);
        if (header != null) {
            header.setTranslationY(-100f); header.setAlpha(0f);
            header.animate().translationY(0f).alpha(1f).setDuration(550)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
    }
    static class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.VH> {
        private final Context ctx;
        private final List<String> data;
        GalleryAdapter(Context c, List<String> d) { ctx = c; data = d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_gallery, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, final int pos) {
            final String name = data.get(pos);
            Bitmap bm = WallpaperHelper.load(ctx, name);
            if (bm != null) h.iv.setImageBitmap(bm);
            h.tv.setText("#" + (pos + 1));
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationY(50f);
            h.itemView.animate().alpha(1f).translationY(0f)
                .setStartDelay(pos * 40L).setDuration(400)
                .setInterpolator(new DecelerateInterpolator()).start();
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.setWallpaper(ctx, name);
                    NiceToast.love(ctx, "已设为壁纸 #" + (pos + 1));
                }
            });
        }
        @Override public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            ImageView iv; TextView tv;
            VH(View v) { super(v); iv = v.findViewById(R.id.ivGallery); tv = v.findViewById(R.id.tvGalleryIndex); }
        }
    }
}
