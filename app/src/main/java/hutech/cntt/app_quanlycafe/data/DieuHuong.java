package hutech.cntt.app_quanlycafe.data;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import hutech.cntt.app_quanlycafe.GiaoDienBanActivity;
import hutech.cntt.app_quanlycafe.GiaoDienCaiDatActivity;
import hutech.cntt.app_quanlycafe.GiaoDienDangNhapActivity;
import hutech.cntt.app_quanlycafe.GiaoDienOrderActivity;
import hutech.cntt.app_quanlycafe.GiaoDienThucDonActivity;
import hutech.cntt.app_quanlycafe.GiaoDienTrangChuActivity;
import hutech.cntt.app_quanlycafe.R;

public class DieuHuong {

    public static void apDungInsets(Activity activity) {
        View main = activity.findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static void ganThanhDieuHuong(Activity activity, int itemHienTai) {
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
            } else if (id == R.id.nav_order) {
                manHinh = GiaoDienOrderActivity.class;
            } else {
                manHinh = GiaoDienCaiDatActivity.class;
            }
            chuyenManHinh(activity, manHinh);
            return false;
        });
    }

    public static void ganThanhTieuDe(Activity activity, String tieuDe, boolean veTrangChu) {
        ((TextView) activity.findViewById(R.id.txtTieuDeManHinh)).setText(tieuDe);
        activity.findViewById(R.id.btnQuayLai).setOnClickListener(v -> {
            if (veTrangChu) chuyenManHinh(activity, GiaoDienTrangChuActivity.class);
            else activity.finish();
        });
    }

    public static ImageView ganNutTieuDe(Activity activity, int viTri, int icon, String moTa, View.OnClickListener xuLy) {
        ImageView nut = activity.findViewById(viTri == 1 ? R.id.imgHanhDong : R.id.imgHanhDong2);
        nut.setImageResource(icon);
        nut.setContentDescription(moTa);
        nut.setVisibility(View.VISIBLE);
        nut.setOnClickListener(xuLy);
        return nut;
    }

    //TÀI KHOẢN

    public static boolean chiQuanLy(Activity activity) {
        if (CafeDatabase.getInstance(activity).laQuanLy()) return true;
        Toast.makeText(activity, "Chỉ quản lý mới dùng được chức năng này", Toast.LENGTH_SHORT).show();
        activity.finish();
        return false;
    }

    public static void dangXuat(Activity activity) {
        CafeDatabase.getInstance(activity).dangXuat();
        Intent in = new Intent(activity, GiaoDienDangNhapActivity.class);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(in);
        activity.overridePendingTransition(R.anim.tab_vao, R.anim.tab_ra);
        activity.finish();
    }

    public static void xacNhanDangXuat(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Đăng xuất?")
                .setMessage("Người trực ca sau đăng nhập bằng tài khoản của mình.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (d, w) -> dangXuat(activity))
                .show();
    }

    public static void hienThiDoiMatKhau(Activity activity, boolean batBuoc, Runnable sauKhiDoi) {
        CafeDatabase db = CafeDatabase.getInstance(activity);
        EditText edtCu = oMatKhau(activity, "Mật khẩu hiện tại");
        EditText edtMoi = oMatKhau(activity, "Mật khẩu mới (ít nhất 6 ký tự)");
        EditText edtNhapLai = oMatKhau(activity, "Nhập lại mật khẩu mới");
        if (batBuoc) {
            edtCu.setText(CafeDatabase.MAT_KHAU_MAC_DINH);
            edtCu.setVisibility(View.GONE);
        }
        LinearLayout khung = new LinearLayout(activity);
        khung.setOrientation(LinearLayout.VERTICAL);
        int dem = (int) (20 * activity.getResources().getDisplayMetrics().density);
        khung.setPadding(dem, dem / 2, dem, 0);
        khung.addView(edtCu);
        khung.addView(edtMoi);
        khung.addView(edtNhapLai);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("Đổi mật khẩu")
                .setView(khung)
                .setPositiveButton("Đổi mật khẩu", null);
        if (batBuoc) {
            builder.setMessage("Bạn đang dùng mật khẩu mặc định. Hãy đặt mật khẩu mới để tiếp tục.")
                    .setCancelable(false)
                    .setNegativeButton("Đăng xuất", (d, w) -> dangXuat(activity));
        } else {
            builder.setNegativeButton("Hủy", null);
        }
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cu = edtCu.getText().toString();
            String moi = edtMoi.getText().toString();
            if (cu.isEmpty()) {
                edtCu.setError("Nhập mật khẩu hiện tại");
                return;
            }
            if (moi.length() < 6) {
                edtMoi.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
                return;
            }
            if (moi.equals(CafeDatabase.MAT_KHAU_MAC_DINH) || moi.equals(cu)) {
                edtMoi.setError("Hãy chọn mật khẩu khác mật khẩu cũ");
                return;
            }
            if (!moi.equals(edtNhapLai.getText().toString())) {
                edtNhapLai.setError("Mật khẩu nhập lại không khớp");
                return;
            }
            ChayNen.chay(activity, () -> {
                db.doiMatKhau(cu, moi);
                return true;
            }, ok -> {
                Toast.makeText(activity, "Đã đổi mật khẩu", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (sauKhiDoi != null) sauKhiDoi.run();
            }, loi -> (batBuoc ? edtMoi : edtCu).setError(loi));
        });
    }

    private static EditText oMatKhau(Activity activity, String goiY) {
        EditText edt = new EditText(activity);
        edt.setHint(goiY);
        edt.setSingleLine(true);
        edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edt.setTypeface(Typeface.DEFAULT); // ô mật khẩu mặc định dùng font monospace
        return edt;
    }

    public static void chuyenManHinh(Activity activity, Class<?> manHinh) {
        Intent in = new Intent(activity, manHinh);
        in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(in);
        // Đổi tab dưới: hiện dần thay vì trượt như mở màn hình con
        activity.overridePendingTransition(R.anim.tab_vao, R.anim.tab_ra);
    }

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
