package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.adapter.DongAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.CafeDatabase.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.model.NhanVien;


public class GiaoDienTaiKhoanActivity extends AppCompatActivity {

    ListView lvTaiKhoan;
    CafeDatabase db;
    ArrayList<NhanVien> listNhanVien = new ArrayList<>();
    DongAdapter<NhanVien> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!DieuHuong.chiQuanLy(this)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_tai_khoan);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhTieuDe(this, "Tài khoản nhân viên", false);
        DieuHuong.ganNutTieuDe(this, 1, R.drawable.ic_add, "Thêm nhân viên", v -> themNhanVien());

        db = CafeDatabase.getInstance(this);
        lvTaiKhoan = findViewById(R.id.lvTaiKhoan);
        adapter = new DongAdapter<>(this, listNhanVien, R.layout.item_the_quan_ly, (dong, nv) -> {
            ImageView img = dong.findViewById(R.id.imgTheQuanLy);
            img.setImageResource(nv.isHoatDong() ? R.drawable.ic_person : R.drawable.ic_lock);
            img.setAlpha(nv.isHoatDong() ? 1f : 0.5f);
            String ten = nv.getHoTen() + (nv.getId() == db.idNguoiDangNhap() ? " (bạn)" : "");
            ((TextView) dong.findViewById(R.id.txtTenTheQuanLy)).setText(ten);
            ((TextView) dong.findViewById(R.id.txtMoTaTheQuanLy)).setText(nv.getTenDangNhap() + " • "
                    + nv.getVaiTro() + (nv.isHoatDong() ? "" : " • Đã khóa"));
        });
        lvTaiKhoan.setAdapter(adapter);
        lvTaiKhoan.setOnItemClickListener((parent, view, position, id) ->
                hienThiTuyChon(listNhanVien.get(position)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) taiDuLieu();
    }

    private void taiDuLieu() {
        ChayNen.chay(this, db::layTatCaNhanVien, ds -> {
            listNhanVien.clear();
            listNhanVien.addAll(ds);
            adapter.notifyDataSetChanged();
        });
    }

    private void ghi(ChayNen.Viec<?> viec, String thongBao) {
        ChayNen.chay(this, () -> {
            viec.lam();
            return true;
        }, ok -> {
            Toast.makeText(this, thongBao, Toast.LENGTH_SHORT).show();
            taiDuLieu();
        }, loi -> new AlertDialog.Builder(this)
                .setTitle("Không thực hiện được")
                .setMessage(loi)
                .setPositiveButton("Đã hiểu", null)
                .show());
    }

    private void hienThiTuyChon(NhanVien nv) {
        boolean laMinh = nv.getId() == db.idNguoiDangNhap();
        ArrayList<String> tuyChon = new ArrayList<>();
        if (laMinh) {
            tuyChon.add("Đổi mật khẩu của tôi");
        } else {
            tuyChon.add("Đặt lại mật khẩu về " + CafeDatabase.MAT_KHAU_MAC_DINH);
            tuyChon.add(nv.isHoatDong() ? "Khóa tài khoản" : "Mở khóa tài khoản");
        }
        new AlertDialog.Builder(this)
                .setTitle(nv.getHoTen() + " • " + nv.getTenDangNhap())
                .setItems(tuyChon.toArray(new String[0]), (d, i) -> {
                    String chon = tuyChon.get(i);
                    if (laMinh) {
                        DieuHuong.hienThiDoiMatKhau(this, false, null);
                    } else if (chon.startsWith("Đặt lại")) {
                        xacNhanDatLaiMatKhau(nv);
                    } else {
                        xacNhanKhoa(nv, nv.isHoatDong());
                    }
                })
                .show();
    }

    private void xacNhanDatLaiMatKhau(NhanVien nv) {
        new AlertDialog.Builder(this)
                .setTitle("Đặt lại mật khẩu?")
                .setMessage("Mật khẩu của " + nv.getHoTen() + " sẽ về " + CafeDatabase.MAT_KHAU_MAC_DINH
                        + ". Lần đăng nhập tới họ phải đặt mật khẩu mới.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đặt lại", (d, w) -> ghi(() -> {
                    db.datLaiMatKhau(nv.getId());
                    return null;
                }, "Đã đặt lại mật khẩu cho " + nv.getHoTen()))
                .show();
    }

    private void xacNhanKhoa(NhanVien nv, boolean khoa) {
        new AlertDialog.Builder(this)
                .setTitle(khoa ? "Khóa tài khoản?" : "Mở khóa tài khoản?")
                .setMessage(khoa
                        ? nv.getHoTen() + " sẽ không đăng nhập được nữa. Hóa đơn cũ của họ vẫn được giữ."
                        : nv.getHoTen() + " sẽ đăng nhập lại được.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton(khoa ? "Khóa" : "Mở khóa", (d, w) -> ghi(() -> {
                    db.khoaTaiKhoan(nv.getId(), khoa);
                    return null;
                }, (khoa ? "Đã khóa " : "Đã mở khóa ") + nv.getHoTen()))
                .show();
    }

    private void themNhanVien() {
        EditText edtHoTen = new EditText(this);
        edtHoTen.setHint("Họ tên");
        edtHoTen.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText edtTenDangNhap = new EditText(this);
        edtTenDangNhap.setHint("Tên đăng nhập");
        edtTenDangNhap.setInputType(InputType.TYPE_CLASS_TEXT);
        EditText edtSdt = new EditText(this);
        edtSdt.setHint("Số điện thoại (không bắt buộc)");
        edtSdt.setInputType(InputType.TYPE_CLASS_PHONE);
        CheckBox chkQuanLy = new CheckBox(this);
        chkQuanLy.setText("Quản lý");

        LinearLayout khung = new LinearLayout(this);
        khung.setOrientation(LinearLayout.VERTICAL);
        int dem = Math.round(20 * getResources().getDisplayMetrics().density);
        khung.setPadding(dem, dem / 2, dem, 0);
        khung.addView(edtHoTen);
        khung.addView(edtTenDangNhap);
        khung.addView(edtSdt);
        khung.addView(chkQuanLy);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thêm nhân viên")
                .setMessage("Mật khẩu ban đầu: " + CafeDatabase.MAT_KHAU_MAC_DINH
                        + ". Nhân viên phải đổi ở lần đăng nhập đầu tiên.")
                .setView(khung)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String hoTen = edtHoTen.getText().toString().trim();
            String tenDangNhap = edtTenDangNhap.getText().toString().trim();
            if (hoTen.isEmpty()) {
                edtHoTen.setError("Nhập họ tên");
                return;
            }
            if (!tenDangNhap.matches("[A-Za-z0-9_.]{3,50}")) {
                edtTenDangNhap.setError("3-50 ký tự: chữ không dấu, số, _ .");
                return;
            }
            ChayNen.chay(this, () -> db.themNhanVien(tenDangNhap, hoTen, chkQuanLy.isChecked(),
                    edtSdt.getText().toString()), id -> {
                Toast.makeText(this, "Đã thêm " + hoTen, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                taiDuLieu();
            }, edtTenDangNhap::setError);
        });
    }
}
