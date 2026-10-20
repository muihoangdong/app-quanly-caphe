package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.HoaDon;

/**
 * Thanh toán hóa đơn của bàn: voucher, chiết khấu thành viên, VAT, phương thức thanh toán.
 */
public class GiaoDienThanhToanActivity extends AppCompatActivity {

    static final String[] HANG_THANH_VIEN = {"Khách lẻ (0%)", "Thành viên Silver (5%)", "VIP Gold (10%)"};
    static final double[] TY_LE_THANH_VIEN = {0, 0.05, 0.10};

    TextView txtBanThanhToan, txtMaHoaDon, txtSoMonThanhToan, txtTongTienHang;
    LinearLayout layoutDanhSachMon, layoutVoucher, layoutChietKhau;
    TextView txtTenVoucher, txtGiamVoucher, txtTenChietKhau, txtGiamChietKhau, txtThueVat;
    TextView txtTongThanhToan, txtSoTienChuan, txtSoTienQR, txtNoiDungQR, txtTienThua, panelHuongDan;
    EditText edtVoucher, edtKhachDua, edtEmailVat;
    Spinner spThanhVien;
    RadioGroup rgPhuongThuc;
    View panelVietQR, panelTienMat;
    CheckBox chkXuatVat;
    MaterialButton btnHoanTat;

    CafeDatabase db;
    HoaDon hoaDon;
    ArrayList<ChiTietHoaDon> listMon;

    long tongTienHang, giamVoucher, giamThanhVien, vat, tongChuan, tongThanhToan;
    String maVoucher = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_thanh_toan);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);
        int hoaDonId = getIntent().getIntExtra("hoaDonId", 0);

        anhXa();
        btnHoanTat.setEnabled(false);

        ((TextView) findViewById(R.id.txtTieuDeManHinh)).setText("Thanh toán");
        TextView txtPhai = findViewById(R.id.txtPhaiTieuDe);
        txtPhai.setVisibility(View.VISIBLE);
        txtPhai.setText("Thu ngân • Quầy 1");
        findViewById(R.id.btnQuayLai).setOnClickListener(v -> finish());

        // Hạng thành viên
        ArrayAdapter<String> adapterTV = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, HANG_THANH_VIEN);
        adapterTV.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spThanhVien.setAdapter(adapterTV);
        spThanhVien.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (hoaDon != null) tinhTien();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.btnApDungVoucher).setOnClickListener(v -> {
            if (hoaDon != null) apDungVoucher();
        });

        rgPhuongThuc.setOnCheckedChangeListener((group, checkedId) -> hienThiPanelPhuongThuc());

        edtKhachDua.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tinhTienThua();
            }
        });
        findViewById(R.id.btnDuTien).setOnClickListener(v -> edtKhachDua.setText(String.valueOf(tongThanhToan)));
        findViewById(R.id.btn200k).setOnClickListener(v -> edtKhachDua.setText("200000"));
        findViewById(R.id.btn500k).setOnClickListener(v -> edtKhachDua.setText("500000"));

        chkXuatVat.setOnCheckedChangeListener((b, checked) ->
                edtEmailVat.setVisibility(checked ? View.VISIBLE : View.GONE));

        btnHoanTat.setOnClickListener(v -> hoanTat());
        findViewById(R.id.btnXemOrderThanhToan).setOnClickListener(v -> {
            // Mở từ màn hình order thì chỉ cần quay lại
            if (!getIntent().getBooleanExtra("tuGoiMon", false)) ThaoTacBan.moChiTietOrder(this, hoaDonId);
            finish();
        });
        findViewById(R.id.btnInTamTinhLai).setOnClickListener(v -> ChayNen.chay(this, () -> {
            db.yeuCauThanhToan(hoaDonId);
            return true;
        }, ok -> Toast.makeText(this, "Đã in lại phiếu tạm tính " + TienIch.tien(tongThanhToan),
                Toast.LENGTH_SHORT).show()));

        hienThiPanelPhuongThuc();
        taiHoaDon(hoaDonId);
    }

    private void taiHoaDon(int hoaDonId) {
        ChayNen.chay(this, () -> {
            Object[] kq = {db.layHoaDon(hoaDonId), db.layChiTietHoaDon(hoaDonId)};
            return kq;
        }, kq -> {
            HoaDon hd = (HoaDon) kq[0];
            if (hd.getTrangThai() != HoaDon.DANG_PHUC_VU) {
                Toast.makeText(this, "Hóa đơn không còn hiệu lực", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            hoaDon = hd;
            @SuppressWarnings("unchecked") ArrayList<ChiTietHoaDon> ds = (ArrayList<ChiTietHoaDon>) kq[1];
            listMon = ds;
            txtBanThanhToan.setText(hoaDon.getTenBan() + " • " + hoaDon.getKhuVuc());
            txtMaHoaDon.setText("#" + hoaDon.getMaHoaDon() + " • " + TienIch.gio(System.currentTimeMillis()));
            int soMon = 0;
            tongTienHang = 0;
            for (ChiTietHoaDon ct : listMon) {
                soMon += ct.getSoLuong();
                tongTienHang += ct.getThanhTien();
            }
            txtSoMonThanhToan.setText(soMon + " món");
            TienIch.hienThiDongMon(layoutDanhSachMon, listMon);
            btnHoanTat.setEnabled(true);
            tinhTien();
            hienThiPanelPhuongThuc();
        }, loi -> {
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void anhXa() {
        txtBanThanhToan = findViewById(R.id.txtBanThanhToan);
        txtMaHoaDon = findViewById(R.id.txtMaHoaDon);
        txtSoMonThanhToan = findViewById(R.id.txtSoMonThanhToan);
        txtTongTienHang = findViewById(R.id.txtTongTienHang);
        layoutDanhSachMon = findViewById(R.id.layoutDanhSachMon);
        layoutVoucher = findViewById(R.id.layoutVoucher);
        layoutChietKhau = findViewById(R.id.layoutChietKhau);
        txtTenVoucher = findViewById(R.id.txtTenVoucher);
        txtGiamVoucher = findViewById(R.id.txtGiamVoucher);
        txtTenChietKhau = findViewById(R.id.txtTenChietKhau);
        txtGiamChietKhau = findViewById(R.id.txtGiamChietKhau);
        txtThueVat = findViewById(R.id.txtThueVat);
        txtTongThanhToan = findViewById(R.id.txtTongThanhToan);
        txtSoTienChuan = findViewById(R.id.txtSoTienChuan);
        txtSoTienQR = findViewById(R.id.txtSoTienQR);
        txtNoiDungQR = findViewById(R.id.txtNoiDungQR);
        txtTienThua = findViewById(R.id.txtTienThua);
        panelHuongDan = findViewById(R.id.panelHuongDan);
        edtVoucher = findViewById(R.id.edtVoucher);
        edtKhachDua = findViewById(R.id.edtKhachDua);
        edtEmailVat = findViewById(R.id.edtEmailVat);
        spThanhVien = findViewById(R.id.spThanhVien);
        rgPhuongThuc = findViewById(R.id.rgPhuongThuc);
        panelVietQR = findViewById(R.id.panelVietQR);
        panelTienMat = findViewById(R.id.panelTienMat);
        chkXuatVat = findViewById(R.id.chkXuatVat);
        btnHoanTat = findViewById(R.id.btnHoanTat);
    }

    private void apDungVoucher() {
        String ma = edtVoucher.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (ma.isEmpty()) {
            maVoucher = null;
        } else if (ma.equals("COFFEE20K") || ma.equals("GIAM10")) {
            if (ma.equals("COFFEE20K") && tongTienHang < 100000) {
                edtVoucher.setError("Áp dụng cho hóa đơn từ 100.000đ");
                return;
            }
            maVoucher = ma;
            Toast.makeText(this, "Đã áp dụng voucher " + ma, Toast.LENGTH_SHORT).show();
        } else {
            edtVoucher.setError("Mã không hợp lệ");
            return;
        }
        tinhTien();
    }

    /** Tính lại toàn bộ số tiền: giảm giá -> VAT -> làm tròn. */
    private void tinhTien() {
        giamVoucher = 0;
        if ("COFFEE20K".equals(maVoucher)) giamVoucher = 20000;
        else if ("GIAM10".equals(maVoucher)) giamVoucher = Math.round(tongTienHang * 0.10);

        int hang = Math.max(0, spThanhVien.getSelectedItemPosition());
        giamThanhVien = Math.round((tongTienHang - giamVoucher) * TY_LE_THANH_VIEN[hang]);

        long sauGiam = Math.max(0, tongTienHang - giamVoucher - giamThanhVien);
        vat = Math.round(sauGiam * GiaoDienGoiMonActivity.THUE_VAT);
        tongChuan = sauGiam + vat;
        tongThanhToan = TienIch.lamTron(tongChuan);

        txtTongTienHang.setText(TienIch.tien(tongTienHang));
        layoutVoucher.setVisibility(giamVoucher > 0 ? View.VISIBLE : View.GONE);
        txtTenVoucher.setText("Voucher '" + maVoucher + "'");
        txtGiamVoucher.setText("-" + TienIch.tien(giamVoucher));
        layoutChietKhau.setVisibility(giamThanhVien > 0 ? View.VISIBLE : View.GONE);
        txtTenChietKhau.setText("Chiết khấu " + HANG_THANH_VIEN[hang].replaceAll(" \\(.*\\)", ""));
        txtGiamChietKhau.setText("-" + TienIch.tien(giamThanhVien));
        txtThueVat.setText("+" + TienIch.tien(vat));

        txtTongThanhToan.setText(TienIch.tien(tongThanhToan));
        long chenhLech = tongThanhToan - tongChuan;
        txtSoTienChuan.setText("Số tiền chuẩn: " + TienIch.tien(tongChuan)
                + " (Làm tròn " + (chenhLech >= 0 ? "+" : "") + TienIch.tien(chenhLech) + ")");
        txtSoTienQR.setText(TienIch.tien(tongThanhToan));
        txtNoiDungQR.setText("ND: " + hoaDon.getTenBan().toUpperCase().replace(" ", "")
                + " " + hoaDon.getMaHoaDon().replace("-", ""));
        btnHoanTat.setText("In Hóa đơn & Hoàn tất  •  " + TienIch.tien(tongThanhToan));
        tinhTienThua();
    }

    private void tinhTienThua() {
        long khachDua = TienIch.docSo(edtKhachDua.getText().toString());
        long thua = khachDua - tongThanhToan;
        if (khachDua == 0) {
            txtTienThua.setText("0đ");
        } else if (thua < 0) {
            txtTienThua.setText("Thiếu " + TienIch.tien(-thua));
            txtTienThua.setTextColor(getColor(R.color.status_red));
            return;
        } else {
            txtTienThua.setText(TienIch.tien(thua));
        }
        txtTienThua.setTextColor(getColor(R.color.status_green));
    }

    private String phuongThucDangChon() {
        int id = rgPhuongThuc.getCheckedRadioButtonId();
        if (id == R.id.radTienMat) return "Tiền mặt";
        if (id == R.id.radViDienTu) return "Ví MoMo/Zalo";
        if (id == R.id.radQuetThe) return "Quẹt POS";
        return "VietQR";
    }

    private void hienThiPanelPhuongThuc() {
        int id = rgPhuongThuc.getCheckedRadioButtonId();
        panelVietQR.setVisibility(id == R.id.radVietQR ? View.VISIBLE : View.GONE);
        panelTienMat.setVisibility(id == R.id.radTienMat ? View.VISIBLE : View.GONE);
        if (id == R.id.radViDienTu) {
            panelHuongDan.setVisibility(View.VISIBLE);
            panelHuongDan.setText("Mở ứng dụng MoMo / ZaloPay của quán, chọn \"Nhận tiền\" và nhập số tiền "
                    + TienIch.tien(tongThanhToan) + ". Xác nhận khi khách chuyển thành công.");
        } else if (id == R.id.radQuetThe) {
            panelHuongDan.setVisibility(View.VISIBLE);
            panelHuongDan.setText("Nhập " + TienIch.tien(tongThanhToan)
                    + " trên máy POS, đưa khách quẹt / chạm thẻ và chờ máy in biên lai.");
        } else {
            panelHuongDan.setVisibility(View.GONE);
        }
    }

    private void hoanTat() {
        if (hoaDon == null) return;
        String phuongThuc = phuongThucDangChon();
        long khachDua = tongThanhToan;
        if (phuongThuc.equals("Tiền mặt")) {
            khachDua = TienIch.docSo(edtKhachDua.getText().toString());
            if (khachDua < tongThanhToan) {
                edtKhachDua.setError("Khách đưa chưa đủ tiền");
                edtKhachDua.requestFocus();
                return;
            }
        }
        if (chkXuatVat.isChecked()) {
            String email = edtEmailVat.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmailVat.setError("Email không hợp lệ");
                edtEmailVat.requestFocus();
                return;
            }
        }

        long khachDuaGui = khachDua;
        btnHoanTat.setEnabled(false);
        ChayNen.chay(this, () -> db.thanhToan(hoaDon.getId(), giamVoucher + giamThanhVien, phuongThuc,
                khachDuaGui), this::hienThiKetQua, loi -> {
            btnHoanTat.setEnabled(true);
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
        });
    }

    /** Hóa đơn máy chủ vừa tạo (tổng tiền do máy chủ tính lại). */
    private void hienThiKetQua(HoaDon hd) {
        String phuongThuc = hd.getPhuongThuc();
        long tongThanhToan = hd.getTongTien();
        long khachDua = hd.getKhachDua();
        hoaDon = hd;
        StringBuilder sb = new StringBuilder();
        sb.append(hoaDon.getTenBan()).append(" • ").append(hoaDon.getMaHoaDon()).append("\n\n");
        sb.append("Tổng thanh toán: ").append(TienIch.tien(tongThanhToan)).append("\n");
        sb.append("Phương thức: ").append(phuongThuc).append("\n");
        if (phuongThuc.equals("Tiền mặt")) {
            sb.append("Khách đưa: ").append(TienIch.tien(khachDua)).append("\n");
            sb.append("Tiền thừa: ").append(TienIch.tien(khachDua - tongThanhToan)).append("\n");
        }
        if (chkXuatVat.isChecked()) {
            sb.append("Hóa đơn VAT gửi tới: ").append(edtEmailVat.getText().toString().trim()).append("\n");
        }
        sb.append("\n").append(hoaDon.getTenBan()).append(" đã được trả về trạng thái trống.");

        new AlertDialog.Builder(this)
                .setTitle("✓ Thanh toán thành công")
                .setMessage(sb.toString())
                .setCancelable(false)
                .setPositiveButton("Về sơ đồ bàn", (d, w) -> {
                    Intent in = new Intent(this, GiaoDienBanActivity.class);
                    in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(in);
                    finish();
                })
                .show();
    }
}
