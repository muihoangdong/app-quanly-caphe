package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.GiaoDienKhach;
import hutech.cntt.app_quanlycafe.data.GioHangKhach;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.Ban;
import hutech.cntt.app_quanlycafe.model.DatBan;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;

/**
 * Khách: chọn ngày, giờ, số người, khu vực -> xem bàn còn trống -> đặt bàn.
 * Đặt xong khách nhận "vé" có mã để đưa nhân viên xem khi đến quán.
 */
public class KhachDatBanActivity extends AppCompatActivity {

    /** Khung giờ nhận đặt bàn (quán mở 7h - 22h, nhận khách tới 21h). */
    static final int GIO_MO = 7, GIO_DAT_CUOI = 21;
    static final int SO_NGAY_DAT_TRUOC = 7;
    static final int SO_NGUOI_TOI_DA = 20;

    RadioGroup rgNgay, rgGio, rgKhuVuc;
    TextView txtSoNguoi, txtSoBanTrong, txtMoTaKhungGio, txtKhongCoBan, txtBanDaChon, txtHetGio;
    LinearLayout layoutDsBan;
    EditText edtGhiChu;
    MaterialButton btnXacNhan;

    CafeDatabase db;
    TaiKhoan khach;

    Calendar ngayChon;          // 00:00 của ngày đã chọn
    int phutTrongNgay = -1;     // giờ đến, tính bằng phút từ 00:00 (-1 = chưa chọn)
    int soNguoi = 2;
    String khuVucChon = null;   // null = tất cả
    Ban banChon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PhienDangNhap.kiemTraQuyen(this, true, false, false)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_khach_dat_ban);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuongKhach(this, R.id.nav_k_dat_ban);

        db = CafeDatabase.getInstance(this);
        khach = PhienDangNhap.layTaiKhoan(this);

        rgNgay = findViewById(R.id.rgNgayDat);
        rgGio = findViewById(R.id.rgGioDat);
        rgKhuVuc = findViewById(R.id.rgKhuVucDat);
        txtSoNguoi = findViewById(R.id.txtSoNguoi);
        txtSoBanTrong = findViewById(R.id.txtSoBanTrongDat);
        txtMoTaKhungGio = findViewById(R.id.txtMoTaKhungGio);
        txtKhongCoBan = findViewById(R.id.txtKhongCoBanTrong);
        txtBanDaChon = findViewById(R.id.txtBanDaChon);
        txtHetGio = findViewById(R.id.txtHetGioHomNay);
        layoutDsBan = findViewById(R.id.layoutDsBanTrong);
        edtGhiChu = findViewById(R.id.edtGhiChuDat);
        btnXacNhan = findViewById(R.id.btnXacNhanDat);

        findViewById(R.id.btnGiamNguoi).setOnClickListener(v -> doiSoNguoi(-1));
        findViewById(R.id.btnTangNguoi).setOnClickListener(v -> doiSoNguoi(1));
        btnXacNhan.setOnClickListener(v -> xacNhanDatBan());

        taoChipNgay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        taoChipKhuVuc();
        taiBanTrong();
    }

    // ---------------- Chọn ngày / giờ / số người / khu vực ----------------

    private RadioButton taoChip(RadioGroup group, String chu, Object tag) {
        RadioButton chip = (RadioButton) LayoutInflater.from(this).inflate(R.layout.item_k_chip, group, false);
        chip.setId(View.generateViewId());
        chip.setText(chu);
        chip.setTag(tag);
        group.addView(chip);
        return chip;
    }

    private void taoChipNgay() {
        SimpleDateFormat dinhDang = new SimpleDateFormat("EEE dd/MM", new Locale("vi", "VN"));
        Calendar ngay = homNay();
        for (int i = 0; i < SO_NGAY_DAT_TRUOC; i++) {
            String chu = i == 0 ? "Hôm nay " + TienIch.ngay(ngay.getTimeInMillis()).substring(0, 5)
                    : i == 1 ? "Ngày mai " + TienIch.ngay(ngay.getTimeInMillis()).substring(0, 5)
                    : dinhDang.format(ngay.getTime());
            RadioButton chip = taoChip(rgNgay, chu, ngay.clone());
            if (i == 0) chip.setChecked(true);
            ngay.add(Calendar.DAY_OF_MONTH, 1);
        }
        ngayChon = homNay();
        rgNgay.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton chip = group.findViewById(checkedId);
            if (chip == null) return;
            ngayChon = (Calendar) chip.getTag();
            taoChipGio();
            taiBanTrong();
        });
        taoChipGio();
    }

    private static Calendar homNay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private void taoChipGio() {
        rgGio.setOnCheckedChangeListener(null);
        rgGio.removeAllViews();
        int phutCu = phutTrongNgay;
        phutTrongNgay = -1;

        // Hôm nay chỉ cho đặt các khung giờ cách hiện tại ít nhất 30 phút
        long som = System.currentTimeMillis() + 30 * 60 * 1000L;
        RadioButton chipDauTien = null;
        for (int phut = GIO_MO * 60; phut <= GIO_DAT_CUOI * 60; phut += 30) {
            if (ngayChon.getTimeInMillis() + phut * 60000L < som) continue;
            RadioButton chip = taoChip(rgGio, String.format(Locale.getDefault(), "%02d:%02d",
                    phut / 60, phut % 60), phut);
            if (chipDauTien == null) chipDauTien = chip;
            if (phut == phutCu) {
                chip.setChecked(true);
                phutTrongNgay = phut;
            }
        }
        if (phutTrongNgay < 0 && chipDauTien != null) {
            chipDauTien.setChecked(true);
            phutTrongNgay = (int) chipDauTien.getTag();
        }
        txtHetGio.setVisibility(chipDauTien == null ? View.VISIBLE : View.GONE);
        findViewById(R.id.hsvGioDat).setVisibility(chipDauTien == null ? View.GONE : View.VISIBLE);

        rgGio.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton chip = group.findViewById(checkedId);
            if (chip == null) return;
            phutTrongNgay = (int) chip.getTag();
            taiBanTrong();
        });
    }

    private void taoChipKhuVuc() {
        rgKhuVuc.setOnCheckedChangeListener(null);
        rgKhuVuc.removeAllViews();
        ArrayList<String> dsKhuVuc = db.layDanhSachKhuVuc();
        if (khuVucChon != null && !dsKhuVuc.contains(khuVucChon)) khuVucChon = null;
        RadioButton tatCa = taoChip(rgKhuVuc, "Tất cả", null);
        if (khuVucChon == null) tatCa.setChecked(true);
        for (String kv : dsKhuVuc) {
            RadioButton chip = taoChip(rgKhuVuc, kv, kv);
            if (kv.equals(khuVucChon)) chip.setChecked(true);
        }
        rgKhuVuc.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton chip = group.findViewById(checkedId);
            if (chip == null) return;
            khuVucChon = (String) chip.getTag();
            taiBanTrong();
        });
    }

    private void doiSoNguoi(int buoc) {
        soNguoi = Math.max(1, Math.min(SO_NGUOI_TOI_DA, soNguoi + buoc));
        txtSoNguoi.setText(String.valueOf(soNguoi));
        taiBanTrong();
    }

    private long thoiGianHen() {
        return ngayChon.getTimeInMillis() + phutTrongNgay * 60000L;
    }

    // ---------------- Danh sách bàn trống ----------------

    private void taiBanTrong() {
        layoutDsBan.removeAllViews();
        if (phutTrongNgay < 0) {
            chonBan(null);
            txtSoBanTrong.setText("0 bàn");
            txtMoTaKhungGio.setText("Chưa chọn giờ đến");
            txtKhongCoBan.setVisibility(View.VISIBLE);
            return;
        }
        long hen = thoiGianHen();
        SimpleDateFormat dinhDang = new SimpleDateFormat("EEEE dd/MM", new Locale("vi", "VN"));
        txtMoTaKhungGio.setText(dinhDang.format(hen) + " • " + TienIch.gio(hen) + " • " + soNguoi + " người"
                + (khuVucChon != null ? " • " + khuVucChon : ""));

        ArrayList<Ban> dsBan = db.layBanTrongLuc(hen, soNguoi, khuVucChon);
        txtSoBanTrong.setText(dsBan.size() + " bàn");
        txtKhongCoBan.setVisibility(dsBan.isEmpty() ? View.VISIBLE : View.GONE);

        // Giữ lựa chọn cũ nếu bàn đó vẫn còn trống
        Ban giuLai = null;
        for (Ban b : dsBan) {
            if (banChon != null && b.getId() == banChon.getId()) giuLai = b;
        }
        banChon = giuLai;

        LayoutInflater inflater = getLayoutInflater();
        LinearLayout hang = null;
        for (int i = 0; i < dsBan.size(); i++) {
            if (i % 2 == 0) {
                hang = new LinearLayout(this);
                hang.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dp(10);
                layoutDsBan.addView(hang, lp);
            }
            Ban ban = dsBan.get(i);
            View the = inflater.inflate(R.layout.item_k_ban_trong, hang, false);
            ((TextView) the.findViewById(R.id.txtTenBanTrong)).setText(ban.getTenBan());
            ((TextView) the.findViewById(R.id.txtChiTietBanTrong)).setText(
                    ban.getKhuVuc() + " • " + ban.getSoCho() + " chỗ");
            the.setTag(ban);
            the.setOnClickListener(v -> chonBan(ban));
            if (i % 2 == 1) ((LinearLayout.LayoutParams) the.getLayoutParams()).setMarginStart(dp(10));
            hang.addView(the);
        }
        // Hàng lẻ: thêm ô trống để thẻ cuối không bị kéo giãn
        if (dsBan.size() % 2 == 1 && hang != null) {
            View trong = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1);
            lp.setMarginStart(dp(10));
            hang.addView(trong, lp);
        }
        chonBan(banChon);
    }

    private int dp(int gt) {
        return Math.round(gt * getResources().getDisplayMetrics().density);
    }

    private void chonBan(Ban ban) {
        banChon = ban;
        for (int i = 0; i < layoutDsBan.getChildCount(); i++) {
            LinearLayout hang = (LinearLayout) layoutDsBan.getChildAt(i);
            for (int j = 0; j < hang.getChildCount(); j++) {
                View the = hang.getChildAt(j);
                if (!(the.getTag() instanceof Ban)) continue;
                boolean dangChon = ban != null && ((Ban) the.getTag()).getId() == ban.getId();
                the.setBackgroundResource(dangChon ? R.drawable.bg_k_the_cam : R.drawable.bg_k_the);
            }
        }
        if (ban == null) {
            txtBanDaChon.setText("Chưa chọn bàn");
            btnXacNhan.setEnabled(false);
        } else {
            txtBanDaChon.setText(ban.getTenBan() + " • " + TienIch.gio(thoiGianHen()) + " "
                    + TienIch.ngay(thoiGianHen()).substring(0, 5));
            btnXacNhan.setEnabled(true);
        }
    }

    // ---------------- Xác nhận ----------------

    private void xacNhanDatBan() {
        if (banChon == null) return;
        long hen = thoiGianHen();
        GiaoDienKhach.xacNhan(this, "Xác nhận đặt bàn",
                banChon.getTenBan() + " (" + banChon.getKhuVuc() + ")\n"
                        + TienIch.gio(hen) + " ngày " + TienIch.ngay(hen) + "\n"
                        + soNguoi + " người\n\nBàn được giữ tối đa 30 phút sau giờ hẹn.",
                "Đặt bàn", () -> {
                    DatBan dat = db.taoDatBan(khach.getId(), banChon, soNguoi, hen,
                            edtGhiChu.getText().toString().trim());
                    if (dat == null) {
                        Toast.makeText(this, "Rất tiếc, bàn vừa có người đặt. Vui lòng chọn bàn khác",
                                Toast.LENGTH_LONG).show();
                        taiBanTrong();
                        return;
                    }
                    edtGhiChu.setText("");
                    banChon = null;
                    taiBanTrong();
                    hienThiVe(dat);
                });
    }

    private void hienThiVe(DatBan dat) {
        new AlertDialog.Builder(this)
                .setTitle("Đặt bàn thành công 🎉")
                .setView(GiaoDienKhach.taoVeDatBan(this, dat))
                .setNegativeButton("Đóng", null)
                .setNeutralButton("Xem đơn của tôi", (d, w) ->
                        DieuHuong.chuyenManHinh(this, KhachDonCuaToiActivity.class))
                .setPositiveButton("Gọi món trước", (d, w) -> {
                    GioHangKhach.datBanIdGoiY = dat.getId();
                    DieuHuong.chuyenManHinh(this, KhachThucDonActivity.class);
                })
                .show();
    }
}
