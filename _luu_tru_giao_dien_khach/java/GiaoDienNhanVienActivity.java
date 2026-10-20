package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.adapter.DongAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.GiaoDienKhach;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;

/**
 * Quản trị: thêm, sửa, khóa / mở khóa, đặt lại mật khẩu, xóa tài khoản nhân viên.
 */
public class GiaoDienNhanVienActivity extends AppCompatActivity {

    static final String[] CHUC_VU = {"Quản lý cửa hàng", "Quản lý ca", "Thu ngân", "Pha chế", "Phục vụ"};

    ListView lvNhanVien;
    EditText edtTim;
    TextView txtTong;

    CafeDatabase db;
    TaiKhoan toi;
    final ArrayList<TaiKhoan> listNhanVien = new ArrayList<>();
    DongAdapter<TaiKhoan> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PhienDangNhap.kiemTraQuyen(this, false, false, true)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_nhan_vien);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_nhan_vien);

        db = CafeDatabase.getInstance(this);
        toi = PhienDangNhap.layTaiKhoan(this);
        lvNhanVien = findViewById(R.id.lvNhanVien);
        edtTim = findViewById(R.id.edtTimNhanVien);
        txtTong = findViewById(R.id.txtTongNhanVien);

        adapter = new DongAdapter<>(this, listNhanVien, R.layout.item_nhan_vien, this::ganDong);
        lvNhanVien.setAdapter(adapter);
        lvNhanVien.setOnItemClickListener((p, v, position, id) -> hienThiTuyChon(listNhanVien.get(position)));

        findViewById(R.id.btnThemNhanVien).setOnClickListener(v -> hienThiDialog(null));
        findViewById(R.id.imgThongBao).setOnClickListener(v ->
                Toast.makeText(this, "Không có thông báo mới", Toast.LENGTH_SHORT).show());
        edtTim.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                taiDuLieu();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    private void taiDuLieu() {
        String tuKhoa = edtTim.getText().toString().trim().toLowerCase(Locale.getDefault());
        ArrayList<TaiKhoan> tatCa = db.layDanhSachNhanVien();
        int dangLam = 0;
        listNhanVien.clear();
        for (TaiKhoan tk : tatCa) {
            if (tk.isDangHoatDong()) dangLam++;
            String chuoi = (tk.getHoTen() + " " + tk.getTenDangNhap() + " " + tk.getSoDienThoai())
                    .toLowerCase(Locale.getDefault());
            if (tuKhoa.isEmpty() || chuoi.contains(tuKhoa)) listNhanVien.add(tk);
        }
        adapter.notifyDataSetChanged();
        txtTong.setText(tatCa.size() + " tài khoản • " + dangLam + " đang làm");
    }

    private void ganDong(View dong, TaiKhoan tk) {
        ((TextView) dong.findViewById(R.id.txtAvatarNhanVien)).setText(GiaoDienKhach.chuCaiDau(tk.getHoTen()));
        ((TextView) dong.findViewById(R.id.txtTenNhanVien)).setText(tk.getHoTen()
                + (tk.getId() == toi.getId() ? " (bạn)" : ""));
        String chucVu = tk.getChucVu() == null || tk.getChucVu().isEmpty() ? "" : tk.getChucVu() + " • ";
        ((TextView) dong.findViewById(R.id.txtChucVuNhanVien)).setText(chucVu + "@" + tk.getTenDangNhap());
        ((TextView) dong.findViewById(R.id.txtSdtNhanVien)).setText(tk.getSoDienThoai());

        TextView txtVaiTro = dong.findViewById(R.id.txtVaiTroNhanVien);
        txtVaiTro.setText(tk.tenVaiTro());
        txtVaiTro.setBackgroundResource(tk.laQuanTri() ? R.drawable.bg_pill_caramel : R.drawable.bg_pill_latte);
        txtVaiTro.setTextColor(getColor(tk.laQuanTri() ? R.color.white : R.color.espresso));

        TextView txtTrangThai = dong.findViewById(R.id.txtTrangThaiNhanVien);
        txtTrangThai.setText(tk.isDangHoatDong() ? "Đang làm" : "Đã khóa");
        txtTrangThai.setBackgroundResource(tk.isDangHoatDong() ? R.drawable.bg_pill_green : R.drawable.bg_pill_red);
        txtTrangThai.setTextColor(getColor(tk.isDangHoatDong() ? R.color.status_green : R.color.status_red));
        dong.setAlpha(tk.isDangHoatDong() ? 1f : 0.6f);
    }

    private void hienThiTuyChon(TaiKhoan tk) {
        boolean laToi = tk.getId() == toi.getId();
        ArrayList<String> tuyChon = new ArrayList<>();
        tuyChon.add("Sửa thông tin");
        tuyChon.add(laToi ? "Đổi mật khẩu" : "Đặt lại mật khẩu");
        if (!laToi) {
            tuyChon.add(tk.isDangHoatDong() ? "Khóa tài khoản" : "Mở khóa tài khoản");
            tuyChon.add("Xóa tài khoản");
        }
        new AlertDialog.Builder(this)
                .setTitle(tk.getHoTen())
                .setItems(tuyChon.toArray(new String[0]), (d, which) -> {
                    String chon = tuyChon.get(which);
                    if (chon.startsWith("Sửa")) {
                        hienThiDialog(tk);
                    } else if (chon.startsWith("Đổi")) {
                        GiaoDienKhach.hienThiDoiMatKhau(this, tk.getId(), true);
                    } else if (chon.startsWith("Đặt lại")) {
                        GiaoDienKhach.hienThiDoiMatKhau(this, tk.getId(), false);
                    } else if (chon.startsWith("Xóa")) {
                        xoa(tk);
                    } else {
                        doiTrangThai(tk);
                    }
                })
                .show();
    }

    /** Không cho khóa / xóa / hạ quyền quản trị viên cuối cùng còn hoạt động. */
    private boolean laQuanTriCuoi(TaiKhoan tk) {
        return tk.laQuanTri() && tk.isDangHoatDong() && db.demQuanTriDangHoatDong() <= 1;
    }

    private void doiTrangThai(TaiKhoan tk) {
        if (laQuanTriCuoi(tk)) {
            Toast.makeText(this, "Phải còn ít nhất 1 quản trị viên hoạt động", Toast.LENGTH_SHORT).show();
            return;
        }
        tk.setDangHoatDong(!tk.isDangHoatDong());
        db.suaTaiKhoan(tk);
        Toast.makeText(this, (tk.isDangHoatDong() ? "Đã mở khóa " : "Đã khóa ") + tk.getHoTen(),
                Toast.LENGTH_SHORT).show();
        taiDuLieu();
    }

    private void xoa(TaiKhoan tk) {
        if (laQuanTriCuoi(tk)) {
            Toast.makeText(this, "Phải còn ít nhất 1 quản trị viên hoạt động", Toast.LENGTH_SHORT).show();
            return;
        }
        GiaoDienKhach.xacNhan(this, "Xóa tài khoản",
                "Xóa vĩnh viễn tài khoản " + tk.getHoTen() + " (@" + tk.getTenDangNhap() + ")?"
                        + "\nNếu chỉ tạm nghỉ, nên dùng Khóa tài khoản.", "Xóa", () -> {
                    db.xoaTaiKhoan(tk.getId());
                    Toast.makeText(this, "Đã xóa " + tk.getHoTen(), Toast.LENGTH_SHORT).show();
                    taiDuLieu();
                });
    }

    private void hienThiDialog(TaiKhoan tkSua) {
        View view = getLayoutInflater().inflate(R.layout.dialog_nhan_vien, null);
        EditText edtHoTen = view.findViewById(R.id.edtHoTenNV);
        EditText edtTenDN = view.findViewById(R.id.edtTenDangNhapNV);
        EditText edtSdt = view.findViewById(R.id.edtSdtNV);
        AutoCompleteTextView edtChucVu = view.findViewById(R.id.edtChucVuNV);
        RadioGroup rgVaiTro = view.findViewById(R.id.rgVaiTroNV);
        EditText edtMatKhau = view.findViewById(R.id.edtMatKhauNV);

        edtChucVu.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CHUC_VU));
        edtChucVu.setOnClickListener(v -> edtChucVu.showDropDown());

        if (tkSua != null) {
            edtHoTen.setText(tkSua.getHoTen());
            edtTenDN.setText(tkSua.getTenDangNhap());
            edtSdt.setText(tkSua.getSoDienThoai());
            edtChucVu.setText(tkSua.getChucVu());
            rgVaiTro.check(tkSua.laQuanTri() ? R.id.radVaiTroQuanTri : R.id.radVaiTroNhanVien);
            // Mật khẩu đổi riêng bằng "Đặt lại mật khẩu"
            edtMatKhau.setVisibility(View.GONE);
            view.findViewById(R.id.txtNhanMatKhauNV).setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(tkSua == null ? "Thêm nhân viên" : "Sửa " + tkSua.getHoTen())
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String hoTen = edtHoTen.getText().toString().trim();
            String tenDN = edtTenDN.getText().toString().trim();
            String sdt = edtSdt.getText().toString().trim();
            int vaiTro = rgVaiTro.getCheckedRadioButtonId() == R.id.radVaiTroQuanTri
                    ? TaiKhoan.QUAN_TRI : TaiKhoan.NHAN_VIEN;
            int boQua = tkSua == null ? 0 : tkSua.getId();

            if (!DangKyActivity.kiemTra(edtHoTen, !hoTen.isEmpty(), "Nhập họ tên")) return;
            if (!DangKyActivity.kiemTra(edtTenDN, tenDN.matches("[a-zA-Z0-9_.]{3,30}"),
                    "3-30 ký tự: chữ không dấu, số, dấu _ .")) return;
            if (!DangKyActivity.kiemTra(edtSdt, sdt.isEmpty() || DangKyActivity.laSoDienThoai(sdt),
                    "Số điện thoại không hợp lệ")) return;
            if (!DangKyActivity.kiemTra(edtTenDN, !db.daTonTai(tenDN, sdt.isEmpty() ? tenDN : sdt, boQua),
                    "Tên đăng nhập hoặc SĐT đã được dùng")) return;
            if (tkSua == null && !DangKyActivity.kiemTra(edtMatKhau,
                    edtMatKhau.getText().length() >= 6, "Mật khẩu ít nhất 6 ký tự")) return;
            if (tkSua != null && vaiTro != TaiKhoan.QUAN_TRI && laQuanTriCuoi(tkSua)) {
                Toast.makeText(this, "Phải còn ít nhất 1 quản trị viên hoạt động", Toast.LENGTH_SHORT).show();
                return;
            }

            TaiKhoan tk = tkSua == null ? new TaiKhoan() : tkSua;
            tk.setHoTen(hoTen);
            tk.setTenDangNhap(tenDN);
            tk.setSoDienThoai(sdt);
            tk.setChucVu(edtChucVu.getText().toString().trim());
            tk.setVaiTro(vaiTro);
            if (tkSua == null) {
                db.themTaiKhoan(tk, edtMatKhau.getText().toString());
                Toast.makeText(this, "Đã tạo tài khoản @" + tenDN, Toast.LENGTH_SHORT).show();
            } else {
                db.suaTaiKhoan(tk);
                if (tk.getId() == toi.getId()) {
                    PhienDangNhap.lamMoi(this);
                    toi = PhienDangNhap.layTaiKhoan(this);
                    if (!toi.laQuanTri()) {
                        // Tự hạ quyền: không còn được vào màn hình này
                        PhienDangNhap.moManHinhChinh(this, toi);
                        return;
                    }
                    DieuHuong.ganHeader(this);
                }
                Toast.makeText(this, "Đã cập nhật " + hoTen, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            taiDuLieu();
        });
    }
}
