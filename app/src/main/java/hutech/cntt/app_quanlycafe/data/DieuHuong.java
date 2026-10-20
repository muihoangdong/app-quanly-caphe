package hutech.cntt.app_quanlycafe.data;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import hutech.cntt.app_quanlycafe.GiaoDienBanActivity;
import hutech.cntt.app_quanlycafe.GiaoDienBaoCaoActivity;
import hutech.cntt.app_quanlycafe.GiaoDienHoaDonActivity;
import hutech.cntt.app_quanlycafe.GiaoDienThucDonActivity;
import hutech.cntt.app_quanlycafe.GiaoDienTrangChuActivity;
import hutech.cntt.app_quanlycafe.R;

/**
 * Xử lý chung cho các màn hình: tràn viền (edge-to-edge), thanh tiêu đề và thanh điều hướng dưới.
 */
public class DieuHuong {

    /** Đệm view gốc (id = main) theo thanh hệ thống và bàn phím. */
    public static void apDungInsets(Activity activity) {
        View main = activity.findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Gắn thanh điều hướng dưới, itemHienTai là mục của màn hình đang mở. */
    public static void ganThanhDieuHuong(Activity activity, int itemHienTai) {
        View btnCaiDat = activity.findViewById(R.id.imgCaiDat);
        if (btnCaiDat != null) btnCaiDat.setOnClickListener(v -> hienThiCaiDatMayChu(activity, null));

        BottomNavigationView nav = activity.findViewById(R.id.bottomNav);
        if (nav == null) return;
        nav.setSelectedItemId(itemHienTai);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == itemHienTai) return true;
            Class<?> manHinh;
            if (id == R.id.nav_trang_chu) {
                manHinh = GiaoDienTrangChuActivity.class;
            } else if (id == R.id.nav_ban) {
                manHinh = GiaoDienBanActivity.class;
            } else if (id == R.id.nav_thuc_don) {
                manHinh = GiaoDienThucDonActivity.class;
            } else if (id == R.id.nav_hoa_don) {
                manHinh = GiaoDienHoaDonActivity.class;
            } else {
                manHinh = GiaoDienBaoCaoActivity.class;
            }
            chuyenManHinh(activity, manHinh);
            return false;
        });
    }

    public static void chuyenManHinh(Activity activity, Class<?> manHinh) {
        Intent in = new Intent(activity, manHinh);
        in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(in);
        activity.overridePendingTransition(0, 0);
    }

    /**
     * Hộp thoại nhập địa chỉ MySQL dạng host:cổng (VD: 127.0.0.1:3307 cho máy ảo đã adb reverse,
     * 192.168.1.5:3307 cho điện thoại thật cùng Wi-Fi). sauKhiLuu chạy khi kết nối thành công.
     */
    public static void hienThiCaiDatMayChu(Activity activity, Runnable sauKhiLuu) {
        CafeDatabase db = CafeDatabase.getInstance(activity);
        EditText edt = new EditText(activity);
        edt.setText(db.getKetNoi().layDiaChi());
        edt.setSelection(edt.length());
        edt.setSingleLine(true);
        int dem = (int) (20 * activity.getResources().getDisplayMetrics().density);
        FrameLayout khung = new FrameLayout(activity);
        khung.setPadding(dem, dem / 2, dem, 0);
        khung.addView(edt);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Địa chỉ MySQL (host:cổng)")
                .setMessage("Mặc định: " + KetNoiMySQL.DIA_CHI_MAC_DINH
                        + " (máy ảo / điện thoại cắm USB, Android Studio tự chạy adb reverse khi bấm Run)"
                        + "\nĐiện thoại cùng Wi-Fi: <IP máy tính>:3307")
                .setView(khung)
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Mặc định", null)
                .setPositiveButton("Lưu & kiểm tra", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            edt.setText(KetNoiMySQL.DIA_CHI_MAC_DINH);
            edt.setSelection(edt.length());
        });
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (edt.getText().toString().trim().isEmpty()) {
                edt.setError("Nhập địa chỉ MySQL");
                return;
            }
            db.getKetNoi().luuDiaChi(edt.getText().toString());
            ChayNen.chay(activity, () -> {
                db.kiemTraKetNoi();
                return true;
            }, ok -> {
                Toast.makeText(activity, "Kết nối MySQL thành công", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (sauKhiLuu != null) sauKhiLuu.run();
            }, loi -> edt.setError(loi));
        });
    }
}
