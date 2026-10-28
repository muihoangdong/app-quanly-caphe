package hutech.cntt.app_quanlycafe.data;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChayNen {

    public interface Viec<T> {
        T lam() throws KetNoiMySQL.LoiCSDL;
    }

    public interface KetQua<T> {
        void nhan(T ketQua);
    }

    public interface Loi {
        void nhan(String thongBao);
    }

    private static final ExecutorService LUONG_NEN = Executors.newFixedThreadPool(2);
    private static final Handler LUONG_GIAO_DIEN = new Handler(Looper.getMainLooper());

    public static <T> void chay(Activity activity, Viec<T> viec, KetQua<T> ketQua) {
        chay(activity, viec, ketQua, thongBao -> Toast.makeText(activity, thongBao, Toast.LENGTH_LONG).show());
    }

    public static <T> void chay(Activity activity, Viec<T> viec, KetQua<T> ketQua, Loi loi) {
        LUONG_NEN.execute(() -> {
            T kq;
            try {
                kq = viec.lam();
            } catch (KetNoiMySQL.LoiCSDL e) {
                LUONG_GIAO_DIEN.post(() -> {
                    if (conSong(activity)) loi.nhan(e.getMessage());
                });
                return;
            }
            LUONG_GIAO_DIEN.post(() -> {
                if (conSong(activity) && ketQua != null) ketQua.nhan(kq);
            });
        });
    }

    private static boolean conSong(Activity activity) {
        return !activity.isFinishing() && !activity.isDestroyed();
    }
}
