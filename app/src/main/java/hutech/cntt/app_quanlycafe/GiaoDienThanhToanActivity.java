package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.HoaDon;


public class GiaoDienThanhToanActivity extends AppCompatActivity {

    TextView txtBanThanhToan, txtMaHoaDon, txtTongTienHang, txtGiamGia, txtTongThanhToan;
    LinearLayout layoutDanhSachMon;
    RadioGroup rgPhuongThuc;
    MaterialButton btnHoanTat;

    CafeDatabase db;
    int hoaDonId;
    HoaDon hoaDon;
    ArrayList<ChiTietHoaDon> listMon = new ArrayList<>();
    String taiKhoan = "";

    long tongTienHang, giamGia, tongThanhToan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_thanh_toan);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_order);
        // Đang ở trong một đơn: bấm lại "Đơn hàng" thì về danh sách đơn
        ((BottomNavigationView) findViewById(R.id.bottomNav)).setOnItemReselectedListener(item ->
                DieuHuong.chuyenManHinh(this, GiaoDienOrderActivity.class));
        DieuHuong.ganThanhTieuDe(this, "Thanh toán", false);
        DieuHuong.ganNutTieuDe(this, 1, R.drawable.ic_more_vert, "Thao tác khác", v -> hienThiThaoTacKhac());

        db = CafeDatabase.getInstance(this);
        hoaDonId = getIntent().getIntExtra("hoaDonId", 0);

        txtBanThanhToan = findViewById(R.id.txtBanThanhToan);
        txtMaHoaDon = findViewById(R.id.txtMaHoaDon);
        txtTongTienHang = findViewById(R.id.txtTongTienHang);
        txtGiamGia = findViewById(R.id.txtGiamGia);
        txtTongThanhToan = findViewById(R.id.txtTongThanhToan);
        layoutDanhSachMon = findViewById(R.id.layoutDanhSachMon);
        rgPhuongThuc = findViewById(R.id.rgPhuongThuc);
        btnHoanTat = findViewById(R.id.btnHoanTat);
        btnHoanTat.setEnabled(false);

        findViewById(R.id.dongGiamGia).setOnClickListener(v -> nhapGiamGia());
        btnHoanTat.setOnClickListener(v -> {
            if (rgPhuongThuc.getCheckedRadioButtonId() == R.id.radChuyenKhoan) xacNhanChuyenKhoan();
            else nhapTienKhachDua();
        });
        findViewById(R.id.btnXemChiTietHoaDon).setOnClickListener(v -> xemTruocHoaDon());
        findViewById(R.id.dongLichSuHoaDon).setOnClickListener(v ->
                startActivity(new Intent(this, GiaoDienLichSuHoaDonActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiHoaDon();
    }

    private void taiHoaDon() {
        ChayNen.chay(this, () -> new Object[]{
                db.layHoaDon(hoaDonId), db.layChiTietHoaDon(hoaDonId), db.layCauHinh()
        }, kq -> {
            HoaDon hd = (HoaDon) kq[0];
            if (hd.getTrangThai() != HoaDon.DANG_PHUC_VU) {
                Toast.makeText(this, "Đơn đã thanh toán hoặc đã hủy", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            hoaDon = hd;
            @SuppressWarnings("unchecked") ArrayList<ChiTietHoaDon> ds = (ArrayList<ChiTietHoaDon>) kq[1];
            @SuppressWarnings("unchecked") LinkedHashMap<String, String> cauHinh = (LinkedHashMap<String, String>) kq[2];
            listMon = ds;
            taiKhoan = cauHinh.get(CafeDatabase.CD_TAI_KHOAN);
            hienThi();
        }, loi -> {
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void hienThi() {
        txtBanThanhToan.setText(hoaDon.getTenBan());
        txtMaHoaDon.setText("#" + hoaDon.getMaHoaDon() + " • " + hoaDon.getSoKhach() + " khách • Vào "
                + TienIch.gio(hoaDon.getGioVao()));

        layoutDanhSachMon.removeAllViews();
        tongTienHang = 0;
        for (ChiTietHoaDon ct : listMon) {
            View dong = getLayoutInflater().inflate(R.layout.item_mon_thanh_toan, layoutDanhSachMon, false);
            ImageView img = dong.findViewById(R.id.imgMonTT);
            img.setClipToOutline(true);
            TienIch.hienThiAnhMon(img, ct.getUriAnh(), ct.getTenMon(), ct.getDanhMuc());
            ((TextView) dong.findViewById(R.id.txtTenMonTT)).setText(ct.getTenMon());
            String soLuong = ct.getSoLuong() + " x " + TienIch.tien(ct.getDonGia());
            if (ct.getGhiChu() != null && !ct.getGhiChu().isEmpty()) soLuong += " • " + ct.getGhiChu();
            ((TextView) dong.findViewById(R.id.txtSoLuongMonTT)).setText(soLuong);
            ((TextView) dong.findViewById(R.id.txtThanhTienMonTT)).setText(TienIch.tien(ct.getThanhTien()));
            layoutDanhSachMon.addView(dong);
            tongTienHang += ct.getThanhTien();
        }
        if (listMon.isEmpty()) {
            TextView txt = new TextView(this);
            txt.setText("Đơn chưa có món. Mở menu ⋮ → Gọi thêm món.");
            txt.setTextColor(getColor(R.color.text_hint));
            txt.setPadding(0, 12, 0, 12);
            layoutDanhSachMon.addView(txt);
        }
        tinhTien();
        btnHoanTat.setEnabled(!listMon.isEmpty());
    }

    private void tinhTien() {
        giamGia = Math.max(0, Math.min(giamGia, tongTienHang));
        tongThanhToan = TienIch.lamTron(tongTienHang - giamGia);

        txtTongTienHang.setText(TienIch.tien(tongTienHang));
        txtGiamGia.setText(giamGia > 0 ? "-" + TienIch.tien(giamGia) : "0đ");
        txtTongThanhToan.setText(TienIch.tien(tongThanhToan));
    }

    private EditText taoONhapTien(String goiY) {
        EditText edt = new EditText(this);
        edt.setInputType(InputType.TYPE_CLASS_TEXT);
        edt.setHint(goiY);
        edt.setSingleLine(true);
        return edt;
    }

    private View boc(View... con) {
        LinearLayout khung = new LinearLayout(this);
        khung.setOrientation(LinearLayout.VERTICAL);
        int dem = (int) (20 * getResources().getDisplayMetrics().density);
        khung.setPadding(dem, dem / 2, dem, 0);
        for (View v : con) khung.addView(v);
        return khung;
    }

    private void nhapGiamGia() {
        EditText edt = taoONhapTien("VD: 20000 hoặc 10%");
        if (giamGia > 0) edt.setText(String.valueOf(giamGia));
        new AlertDialog.Builder(this)
                .setTitle("Giảm giá")
                .setView(boc(edt))
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Bỏ giảm giá", (d, w) -> {
                    giamGia = 0;
                    tinhTien();
                })
                .setPositiveButton("Áp dụng", (d, w) -> {
                    String s = edt.getText().toString().trim();
                    long so = TienIch.docSo(s);
                    giamGia = s.endsWith("%") ? tongTienHang * Math.min(so, 100) / 100 : so;
                    tinhTien();
                })
                .show();
    }

    private void nhapTienKhachDua() {
        EditText edt = taoONhapTien("Số tiền khách đưa");
        edt.setInputType(InputType.TYPE_CLASS_NUMBER);
        edt.setText(String.valueOf(tongThanhToan));
        edt.setSelectAllOnFocus(true);
        TextView txtThua = new TextView(this);
        txtThua.setTextSize(15);
        txtThua.setPadding(0, 12, 0, 0);
        Runnable capNhat = () -> {
            long thua = TienIch.docSo(edt.getText().toString()) - tongThanhToan;
            txtThua.setText(thua < 0 ? "Còn thiếu " + TienIch.tien(-thua) : "Tiền thừa: " + TienIch.tien(thua));
            txtThua.setTextColor(getColor(thua < 0 ? R.color.status_red : R.color.status_green));
        };
        edt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                capNhat.run();
            }
        });
        capNhat.run();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tiền mặt • " + TienIch.tien(tongThanhToan))
                .setView(boc(edt, txtThua))
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xác nhận", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long khachDua = TienIch.docSo(edt.getText().toString());
            if (khachDua < tongThanhToan) {
                edt.setError("Khách đưa chưa đủ tiền");
                return;
            }
            dialog.dismiss();
            thanhToan(HoaDon.TIEN_MAT, khachDua);
        });
    }

    private void xacNhanChuyenKhoan() {
        String noiDung = "Số tiền: " + TienIch.tien(tongThanhToan)
                + "\nNội dung: " + hoaDon.getMaHoaDon() + " " + hoaDon.getTenBan()
                + (taiKhoan == null || taiKhoan.isEmpty() ? "" : "\nTài khoản: " + taiKhoan)
                + "\n\nKiểm tra đã nhận tiền rồi bấm xác nhận.";
        new AlertDialog.Builder(this)
                .setTitle("Chuyển khoản")
                .setMessage(noiDung)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đã nhận tiền", (d, w) -> thanhToan(HoaDon.CHUYEN_KHOAN, tongThanhToan))
                .show();
    }

    private void thanhToan(String phuongThuc, long khachDua) {
        btnHoanTat.setEnabled(false);
        ChayNen.chay(this, () -> db.thanhToan(hoaDonId, giamGia, phuongThuc, khachDua), this::hienThiKetQua, loi -> {
            btnHoanTat.setEnabled(true);
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
        });
    }

    private void hienThiKetQua(HoaDon hd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hóa đơn #").append(hd.getMaHoaDon()).append(" • ").append(hd.getTenBan()).append("\n\n");
        sb.append("Tổng thanh toán: ").append(TienIch.tien(hd.getTongTien())).append("\n");
        sb.append("Phương thức: ").append(hd.getPhuongThuc());
        if (HoaDon.TIEN_MAT.equals(hd.getPhuongThuc())) {
            sb.append("\nKhách đưa: ").append(TienIch.tien(hd.getKhachDua()));
            sb.append("\nTiền thừa: ").append(TienIch.tien(hd.getKhachDua() - hd.getTongTien()));
        }
        new AlertDialog.Builder(this)
                .setTitle("✓ Thanh toán thành công")
                .setMessage(sb.toString())
                .setCancelable(false)
                .setNeutralButton("Xem hóa đơn", (d, w) -> {
                    GiaoDienLichSuHoaDonActivity.hienThiChiTiet(this, hd, this::xong);
                })
                .setPositiveButton("Xong", (d, w) -> xong())
                .show();
    }

    private void xong() {
        DieuHuong.chuyenManHinh(this, GiaoDienOrderActivity.class);
        finish();
    }

    private void xemTruocHoaDon() {
        if (hoaDon == null) return;
        HoaDon xemTruoc = new HoaDon();
        xemTruoc.setId(hoaDon.getId());
        xemTruoc.setMaHoaDon(hoaDon.getMaHoaDon());
        xemTruoc.setTenBan(hoaDon.getTenBan());
        xemTruoc.setKhuVuc(hoaDon.getKhuVuc());
        xemTruoc.setSoKhach(hoaDon.getSoKhach());
        xemTruoc.setGioVao(hoaDon.getGioVao());
        xemTruoc.setTenNhanVien(hoaDon.getTenNhanVien());
        xemTruoc.setTrangThai(HoaDon.DANG_PHUC_VU);
        xemTruoc.setTamTinh(tongTienHang);
        xemTruoc.setGiamGia(giamGia);
        xemTruoc.setTongTien(tongThanhToan);
        GiaoDienLichSuHoaDonActivity.hienThiHoaDon(this, xemTruoc, listMon, null);
    }

    private void hienThiThaoTacKhac() {
        if (hoaDon == null) return;
        String[] tuyChon = {"Gọi thêm món", "Sửa món trong đơn", "In tạm tính", "Hủy đơn"};
        new AlertDialog.Builder(this)
                .setTitle(hoaDon.getTenBan() + " • #" + hoaDon.getMaHoaDon())
                .setItems(tuyChon, (d, i) -> {
                    if (i == 0) {
                        ThaoTacBan.moThucDon(this, hoaDonId);
                    } else if (i == 1) {
                        ThaoTacBan.moChiTietOrder(this, hoaDonId);
                    } else if (i == 2) {
                        inTamTinh();
                    } else {
                        huyDon();
                    }
                })
                .show();
    }

    private void inTamTinh() {
        ChayNen.chay(this, () -> {
            db.yeuCauThanhToan(hoaDonId);
            return true;
        }, ok -> {
            Toast.makeText(this, "Đã in tạm tính " + TienIch.tien(tongThanhToan), Toast.LENGTH_SHORT).show();
            xemTruocHoaDon();
        });
    }

    private void huyDon() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn " + hoaDon.getTenBan() + "?")
                .setMessage("Toàn bộ món sẽ bị hủy" + (hoaDon.isLuotKhach() ? "." : " và bàn trở về trống."))
                .setNegativeButton("Không", null)
                .setPositiveButton("Hủy đơn", (d, w) -> ChayNen.chay(this, () -> {
                    db.huyHoaDon(hoaDonId);
                    return true;
                }, ok -> {
                    Toast.makeText(this, "Đã hủy đơn " + hoaDon.getTenBan(), Toast.LENGTH_SHORT).show();
                    finish();
                }))
                .show();
    }
}
