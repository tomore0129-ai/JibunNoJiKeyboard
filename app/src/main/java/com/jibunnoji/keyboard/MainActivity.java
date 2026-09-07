package com.jibunnoji.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private DrawingView drawing;
    private boolean practice;
    private final int cream = Color.rgb(255,248,235);
    private final int ink = Color.rgb(75,65,60);

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null && state.getBoolean("practice")) {
            showPractice();
            Serializable saved = state.getSerializable("strokes");
            if (saved instanceof ArrayList) {
                for (Object item : (ArrayList<?>) saved) {
                    if (item instanceof float[]) drawing.strokes.add((float[]) item);
                }
            }
        } else showHome();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("practice", practice);
        if (drawing != null) state.putSerializable("strokes", drawing.strokes);
    }

    private LinearLayout screen() {
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setGravity(Gravity.CENTER_HORIZONTAL);
        s.setBackgroundColor(cream);
        s.setPadding(dp(16),dp(16),dp(16),dp(12));
        // Keep controls clear of Android status/navigation bars, including edge-to-edge.
        s.setOnApplyWindowInsetsListener((v,insets) -> {
            v.setPadding(dp(16)+insets.getSystemWindowInsetLeft(),
                dp(16)+insets.getSystemWindowInsetTop(),
                dp(16)+insets.getSystemWindowInsetRight(),
                dp(12)+insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(s);
        s.requestApplyInsets();
        return s;
    }

    private TextView text(String title, int size) {
        TextView t = new TextView(this);
        t.setText(title); t.setTextSize(size); t.setTextColor(ink);
        t.setGravity(Gravity.CENTER); return t;
    }

    private Button button(String title, Runnable action) {
        Button b = new Button(this);
        b.setText(title); b.setTextSize(16); b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void showHome() {
        practice = false; drawing = null;
        LinearLayout s = screen();
        s.addView(text("自分の字キーボード",28));
        s.addView(text("\nじぶんの字を、キーボードにしよう！\n",18));
        s.addView(button("もじをかいてみよう",this::showPractice));
        s.addView(button("ほぞんした字を見る",this::showGallery));
    }

    private void showPractice() {
        practice = true;
        LinearLayout s = screen();
        s.addView(text("もじをかいてみよう",24));
        s.addView(text("白いところに ゆびで かいてね",16));
        drawing = new DrawingView();
        s.addView(drawing,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout row = new LinearLayout(this);
        row.addView(button("ひとつ消す",() -> drawing.undo()),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(button("ぜんぶ消す",() -> {
            if (!drawing.strokes.isEmpty()) new AlertDialog.Builder(this)
                .setMessage("かいた線を ぜんぶ消しますか？")
                .setNegativeButton("やめる",null)
                .setPositiveButton("消す",(d,w) -> {drawing.strokes.clear(); drawing.invalidate();}).show();
        }),new LinearLayout.LayoutParams(0,-2,1));
        s.addView(row);
        s.addView(button("ほぞんする",this::saveDrawing),new LinearLayout.LayoutParams(-1,-2));
        s.addView(button("ほぞんした字を見る",this::showGallery),new LinearLayout.LayoutParams(-1,-2));
    }

    private File drawingsDir() { return new File(getFilesDir(),"drawings"); }

    private void saveDrawing() {
        if (drawing.strokes.isEmpty()) {
            Toast.makeText(this,"字をかいてから ほぞんしてね",Toast.LENGTH_SHORT).show(); return;
        }
        File file = new File(drawingsDir(),UUID.randomUUID().toString()+".png");
        Bitmap bitmap = null;
        try {
            if (!drawingsDir().isDirectory() && !drawingsDir().mkdirs()) throw new IOException();
            bitmap = Bitmap.createBitmap(1024,1024,Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            drawing.paintStrokes(canvas,1024,1024); // Never export guide lines.
            try (FileOutputStream out = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG,100,out)) throw new IOException();
            }
            new AlertDialog.Builder(this).setTitle("ほぞんできたよ！")
                .setMessage("アプリの「ほぞんした字を見る」で確認できます。\nアプリを削除すると保存した字も消えます。")
                .setPositiveButton("つづけてかく",null)
                .setNeutralButton("保存した字を見る",(d,w) -> showGallery()).show();
        } catch (IOException e) {
            file.delete(); // Remove only this incomplete newly-created image.
            Toast.makeText(this,"ほぞんできませんでした。空き容量を確認してください",Toast.LENGTH_LONG).show();
        } finally { if (bitmap != null) bitmap.recycle(); }
    }

    private void showGallery() {
        File[] found = drawingsDir().listFiles((dir,name) -> name.endsWith(".png"));
        if (found == null || found.length == 0) {
            Toast.makeText(this,"まだ保存した字はありません",Toast.LENGTH_SHORT).show(); return;
        }
        Arrays.sort(found,(a,b) -> Long.compare(b.lastModified(),a.lastModified()));
        String[] labels = new String[found.length];
        java.text.DateFormat format = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss",Locale.JAPAN);
        for (int i=0;i<found.length;i++) labels[i] = (i+1)+". "+format.format(new Date(found[i].lastModified()));
        new AlertDialog.Builder(this).setTitle("ほぞんした字")
            .setItems(labels,(dialog,index) -> {
                Bitmap bitmap = BitmapFactory.decodeFile(found[index].getAbsolutePath());
                if (bitmap == null) {
                    Toast.makeText(this,"画像を読み込めませんでした",Toast.LENGTH_SHORT).show(); return;
                }
                ImageView preview = new ImageView(this);
                preview.setImageBitmap(bitmap); preview.setAdjustViewBounds(true);
                preview.setBackgroundColor(Color.WHITE);
                new AlertDialog.Builder(this).setTitle(labels[index]).setView(preview)
                    .setPositiveButton("とじる",null).show();
            }).setNegativeButton("もどる",null).show();
    }

    @Override public void onBackPressed() {
        if (practice) {
            new AlertDialog.Builder(this).setMessage("ホームに戻りますか？ 保存していない線は消えます。")
                .setNegativeButton("かきつづける",null)
                .setPositiveButton("もどる",(d,w) -> showHome()).show();
        } else super.onBackPressed();
    }

    private class DrawingView extends View {
        final ArrayList<float[]> strokes = new ArrayList<>();
        private final ArrayList<Float> points = new ArrayList<>();
        private int pointer = -1;
        private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);

        DrawingView() {
            super(MainActivity.this);
            setBackgroundColor(Color.WHITE); setContentDescription("指で文字を書くキャンバス");
            pen.setColor(ink); pen.setStyle(Paint.Style.STROKE);
            pen.setStrokeCap(Paint.Cap.ROUND); pen.setStrokeJoin(Paint.Join.ROUND);
            guide.setColor(Color.rgb(240,205,218)); guide.setStrokeWidth(dp(1));
        }

        private void drawStroke(Canvas c,float[] data,float width,float height) {
            if (data.length<2) return;
            pen.setStrokeWidth(Math.min(width,height)*0.014f);
            if(data.length==2) { c.drawPoint(data[0]*width,data[1]*height,pen); return; }
            Path path = new Path(); path.moveTo(data[0]*width,data[1]*height);
            for(int i=2;i+1<data.length;i+=2) path.lineTo(data[i]*width,data[i+1]*height);
            c.drawPath(path,pen);
        }

        void paintStrokes(Canvas c,float width,float height) {
            for(float[] stroke:strokes) drawStroke(c,stroke,width,height);
        }

        private float[] active() {
            float[] result = new float[points.size()];
            for(int i=0;i<result.length;i++) result[i]=points.get(i);
            return result;
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawLine(getWidth()/2f,0,getWidth()/2f,getHeight(),guide);
            c.drawLine(0,getHeight()/2f,getWidth(),getHeight()/2f,guide);
            paintStrokes(c,getWidth(),getHeight());
            if(!points.isEmpty()) drawStroke(c,active(),getWidth(),getHeight());
        }

        private void add(float x,float y) {
            points.add(Math.max(0,Math.min(1,x/Math.max(1,getWidth()))));
            points.add(Math.max(0,Math.min(1,y/Math.max(1,getHeight()))));
        }

        private void finish(boolean keep) {
            if(keep && !points.isEmpty()) strokes.add(active());
            points.clear(); pointer=-1;
            getParent().requestDisallowInterceptTouchEvent(false); invalidate();
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN) {
                points.clear(); pointer=e.getPointerId(0);
                add(e.getX(),e.getY());
                getParent().requestDisallowInterceptTouchEvent(true);
            } else if(action==MotionEvent.ACTION_CANCEL) {
                finish(false);
            } else if(action==MotionEvent.ACTION_MOVE) {
                int index=e.findPointerIndex(pointer);
                if(index>=0) {
                    for(int h=0;h<e.getHistorySize();h++)
                        add(e.getHistoricalX(index,h),e.getHistoricalY(index,h));
                    add(e.getX(index),e.getY(index));
                }
            } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
                int index=e.getActionIndex();
                if(e.getPointerId(index)==pointer) {
                    add(e.getX(index),e.getY(index)); finish(true); performClick();
                }
            }
            invalidate(); return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        void undo() {
            if(!strokes.isEmpty()) strokes.remove(strokes.size()-1);
            invalidate();
        }
    }
}
