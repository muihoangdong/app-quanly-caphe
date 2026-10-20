package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.GioHangKhach;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.DatBan;
import hutech.cntt.app_quanlycafe.model.DonOnline;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;

/**
 * Khách: xem giỏ hàng, chọn dùng tại quán (gắn với bàn đã đặt) hoặc mang đi,
 * chọn phương thức thanh toán rồi đặt hàng.
 */
public class KhachGioHangActivity extends AppCompatActivity {

    RadioGroup rgHinhThuc, rgThanhToan;
    Spinner spDatBan;
    TextView txtChuaDatBan, txtGioRong, txtThanhTien, txtTong, txtHinhThuc;
    LinearLayout layoutDongGio, layoutChonDatBan;
    EditText edtGhiChu;

    CafeDatabase db;
    TaiKhoan khach;
    ArrayList<DatBan> dsDatBan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PhienDangNhap.kiemTraQuyen(this, true, false, false)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_khach_gio_hang);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);
        khach = PhienDangNhap.layTaiKhoan(this);
        ((TextView) findViewById(R.id.txtTieuDeManHinh)).setText("Xác nhận đơn hàng");
        findViewById(R.id.btnQuayLai).setOnClickListener(v -> finish());

        rgHinhThuc = findViewById(R.id.rgHinhThuc);
        rgThanhToan = findViewById(R.id.rgThanhToan);
        spDatBan = findViewById(R.id.spDatBan);
        txtChuaDatBan = findViewById(R.id.txtChuaDatBan);
        txtGioRong = findViewById(R.id.txtGioRong);
        txtThanhTien = findViewById(R.id.txtThanhTienGio);
        txtTong = findViewById(R.id.txtTongGioKhach);
        txtHinhThuc = findViewById(R.id.txtHinhThucGio);
        layoutDongGio = findViewById(R.id.layoutDongGio);
        layoutChonDatBan = findViewById(R.id.layoutChonDatBan);
        edtGhiChu = findViewById(R.id.edtGhiChuDon);

        rgHinhThuc.setOnCheckedChangeListener((g, id) -> capNhatHinhThuc());
        txtChuaDatBan.setOnClickListener(v -> {
            DieuHuong.chuyenManHinh(this, KhachDatBanActivity.class);
            finish();
        });
        findViewById(R.id.txtThemMonGio).setOnClickListener(v -> finish());
        findViewById(R.id.btnDatHang).setOnClickListener(v -> datHang());
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDatBan();
        hienThiGio();
    }

    private boolean taiQuan() {
        return rgHinhThuc.getCheckedRadioButtonId() == R.id.radTaiQuan;
    }

    private void taiDatBan() {
        dsDatBan = db.layDatBanSapToi(khach.getId());
        ArrayList<String> ten = new ArrayList<>();
        int viTriGoiY = 0;
        for (int i = 0; i < dsDatBan.size(); i++) {
            DatBan d = dsDatBan.get(i);
            ten.add(d.getTenBan() + " • " + TienIch.gio(d.getThoiGian()) + " " + TienIch.ngay(d.getThoiGian())
                    + " • #" + d.getMaDat());
            if (d.getId() == GioHangKhach.datBanIdGoiY) viTriGoiY = i;
        }
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ten);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDatBan.setAdapter(ad);
        if (!dsDatBan.isEmpty()) spDatBan.setSelection(viTriGoiY);
        // Chưa đặt bàn thì mặc định chọn Mang đi
        if (dsDatBan.isEmpty() && GioHangKhach.datBanIdGoiY == 0) {
            ((RadioButton) findViewById(R.id.radMangDi)).setChecked(true);
        }
        capNhatHinhThuc();
    }

    private void capNhatHinhThuc() {
        layoutChonDatBan.setVisibility(taiQuan() ? View.VISIBLE : View.GONE);
        spDatBan.setVisibility(dsDatBan.isEmpty() ? View.GONE : View.VISIBLE);
        txtChuaDatBan.setVisibility(dsDatBan.isEmpty() ? View.VISIBLE : View.GONE);
        capNhatTong();
    }

    private void hienThiGio() {
        layoutDongGio.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (ChiTietHoaDon ct : GioHangKhach.layDanhSach()) {
            View dong = inflater.inflate(R.layout.item_k_gio_hang, layoutDongGio, false);
            TienIch.hienThiAnhMon(dong.findViewById(R.id.imgDongGio), ct.getUriAnh(), ct.getDanhMuc());
            ((TextView) dong.findViewById(R.id.txtTenDongGio)).setText(ct.getTenMon());
            TextView txtGhiChu = dong.findViewById(R.id.txtGhiChuDongGio);
            txtGhiChu.setText(ct.getGhiChu());
            txtGhiChu.setVisibility(ct.getGhiChu().isEmpty() ? View.GONE : View.VISIBLE);
            ((TextView) dong.findViewById(R.id.txtGiaDongGio)).setText(TienIch.tien(ct.getThanhTien()));
            ((TextView) dong.findViewById(R.id.txtSoLuongDongGio)).setText(String.valueOf(ct.getSoLuong()));
            dong.findViewById(R.id.btnGiamDongGio).setOnClickListener(v -> {
                GioHangKhach.doiSoLuong(ct, ct.getSoLuong() - 1);
                hienThiGio();
            });
            dong.findViewById(R.id.btnTangDongGio).setOnClickListener(v -> {
                GioHangKhach.doiSoLuong(ct, ct.getSoLuong() + 1);
                hienThiGio();
            });
            layoutDongGio.addView(dong);
        }
        txtGioRong.setVisibility(GioHangKhach.rong() ? View.VISIBLE : View.GONE);
        capNhatTong();
    }

    private void capNhatTong() {
        long tong = GioHangKhach.tongTien();
        txtThanhTien.setText(TienIch.tien(tong));
        txtTong.setText(TienIch.tien(tong));
        txtHinhThuc.setText((taiQuan() ? DonOnline.TAI_QUAN : DonOnline.MANG_DI)
                + " • " + GioHangKhach.tongSoLuong() + " món");
    }

    private String phuongThuc() {
        int id = rgThanhToan.getCheckedRadioButtonId();
        if (id == R.id.radMomo) return "Ví MoMo";
        if (id == R.id.radZalo) return "ZaloPay";
        if (id == R.id.radThe) return "Thẻ ATM";
        return DonOnline.TRA_TAI_QUAY;
    }

    private void datHang() {
        if (GioHangKhach.rong()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            return;
        }
        DonOnline don = new DonOnline();
        don.setKhachId(khach.getId());
        if (taiQuan()) {
            if (dsDatBan.isEmpty()) {
                Toast.makeText(this, "Hãy đặt bàn trước hoặc chọn Mang đi", Toast.LENGTH_SHORT).show();
                return;
            }
            don.setDatBanId(dsDatBan.get(spDatBan.getSelectedItemPosition()).getId());
            don.setHinhThuc(DonOnline.TAI_QUAN);
        } else {
            don.setHinhThuc(DonOnline.MANG_DI);
        }
        don.setPhuongThuc(phuongThuc());
        don.setGhiChu(edtGhiChu.getText().toString().trim());

        if (don.getPhuongThuc().equals(DonOnline.TRA_TAI_QUAY)) {
            luuDon(don);
        } else {
            // Thanh toán điện tử: mô phỏng màn hình cổng thanh toán
            new AlertDialog.Builder(this)
                    .setTitle("Thanh toán qua " + don.getPhuongThuc())
                    .setMessage("Số tiền: " + TienIch.tien(GioHangKhach.tongTien())
                            + "\n\n(Bản demo: chưa kết nối cổng thanh toán thật. Bấm \"Đã thanh toán\" để giả lập"
                            + " giao dịch thành công.)")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Đã thanh toán", (d, w) -> {
                        don.setDaThanhToan(true);
                        luuDon(don);
                    })
                    .show();
        }
    }

    private void luuDon(DonOnline don) {
        String ma = db.taoDonOnline(don, GioHangKhach.layDanhSach());
        GioHangKhach.xoaHet();
        new AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công")
                .setMessage("Mã đơn: #" + ma + "\n\n"
                        + (don.getHinhThuc().equals(DonOnline.TAI_QUAN)
                        ? "Quán sẽ chuẩn bị món theo lịch đặt bàn của bạn."
                        : "Quán sẽ báo khi món sẵn sàng, bạn đến quầy đọc mã đơn để nhận.")
                        + (don.isDaThanhToan() ? "" : "\nVui lòng thanh toán tại quầy."))
                .setCancelable(false)
                .setPositiveButton("Xem đơn của tôi", (d, w) -> {
                    DieuHuong.chuyenManHinh(this, KhachDonCuaToiActivity.class);
                    finish();
                })
                .show();
    }
}
