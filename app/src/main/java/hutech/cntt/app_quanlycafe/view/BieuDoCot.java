package hutech.cntt.app_quanlycafe.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import hutech.cntt.app_quanlycafe.R;

/**
 * Biểu đồ cột đơn giản: doanh số theo khung giờ. Cột cao nhất tô màu caramel.
 */
public class BieuDoCot extends View {

    private long[] giaTri = new long[0];
    private String[] nhan = new String[0];

    private final Paint butCot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint butNhan = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint butGiaTri = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint butNen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF hinhChuNhat = new RectF();
    private final float dp;

    public BieuDoCot(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        butNhan.setTextSize(10 * dp);
        butNhan.setColor(ContextCompat.getColor(context, R.color.text_hint));
        butNhan.setTextAlign(Paint.Align.CENTER);
        butGiaTri.setTextSize(10 * dp);
        butGiaTri.setFakeBoldText(true);
        butGiaTri.setColor(ContextCompat.getColor(context, R.color.caramel_dark));
        butGiaTri.setTextAlign(Paint.Align.CENTER);
        butNen.setColor(ContextCompat.getColor(context, R.color.latte));
    }

    public void setDuLieu(long[] giaTri, String[] nhan) {
        this.giaTri = giaTri;
        this.nhan = nhan;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = giaTri.length;
        if (n == 0) return;

        long lonNhat = 0;
        int viTriLonNhat = -1;
        for (int i = 0; i < n; i++) {
            if (giaTri[i] > lonNhat) {
                lonNhat = giaTri[i];
                viTriLonNhat = i;
            }
        }

        float caoNhan = 16 * dp;
        float caoGiaTri = 16 * dp;
        float vungCao = getHeight() - getPaddingTop() - getPaddingBottom() - caoNhan - caoGiaTri;
        float rongO = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) n;
        float rongCot = Math.min(rongO * 0.6f, 28 * dp);
        float day = getHeight() - getPaddingBottom() - caoNhan;
        float bo = 6 * dp;

        for (int i = 0; i < n; i++) {
            float tam = getPaddingLeft() + rongO * i + rongO / 2;
            float trai = tam - rongCot / 2;
            float phai = tam + rongCot / 2;

            // nền cột
            hinhChuNhat.set(trai, day - vungCao, phai, day);
            canvas.drawRoundRect(hinhChuNhat, bo, bo, butNen);

            if (giaTri[i] > 0 && lonNhat > 0) {
                float cao = Math.max(4 * dp, vungCao * giaTri[i] / (float) lonNhat);
                butCot.setColor(ContextCompat.getColor(getContext(),
                        i == viTriLonNhat ? R.color.caramel : R.color.mocha_light));
                hinhChuNhat.set(trai, day - cao, phai, day);
                canvas.drawRoundRect(hinhChuNhat, bo, bo, butCot);
                if (i == viTriLonNhat) {
                    canvas.drawText(rutGon(giaTri[i]), tam, day - cao - 4 * dp, butGiaTri);
                }
            }
            if (i < nhan.length) {
                canvas.drawText(nhan[i], tam, getHeight() - getPaddingBottom() - 3 * dp, butNhan);
            }
        }
    }

    /** 1450000 -> "1.45M", 240000 -> "240k" */
    private String rutGon(long soTien) {
        if (soTien >= 1_000_000) {
            return String.format(java.util.Locale.US, "%.2fM", soTien / 1_000_000.0);
        }
        return (soTien / 1000) + "k";
    }
}
