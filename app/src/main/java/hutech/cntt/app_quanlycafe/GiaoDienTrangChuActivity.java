package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.Ban;

/**
 * Trang chủ: tổng số bàn / bàn trống / bàn đang phục vụ, doanh thu trong ngày, truy cập nhanh gọi món.
 */
public class GiaoDienTrangChuActivity extends AppCompatActivity {

    static final long MOT_NGAY = 24L * 60 * 60 * 1000;

    TextView txtTongSoBan, txtSoBanTrong, txtSoBanPhucVu, txtChiTietBan;
    TextView txtDoanhThu, txtSoHoaDon, txtDangPhucVu, txtLoiKetNoi, txtKhongCoBanPhucVu;
    LinearLayout layoutBanDangPhucVu;
    View layoutLoiKetNoi;

    CafeDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_trang_chu);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_trang_chu);

        db = CafeDatabase.getInstance(this);
        txtTongSoBan = findViewById(R.id.txtTongSoBan);
        txtSoBanTrong = findViewById(R.id.txtSoBanTrongTC);
        txtSoBanPhucVu = findViewById(R.id.txtSoBanPhucVuTC);
        txtChiTietBan = findViewById(R.id.txtChiTietBanTC);
        txtDoanhThu = findViewById(R.id.txtDoanhThuTC);
        txtSoHoaDon = findViewById(R.id.txtSoHoaDonTC);
        txtDangPhucVu = findViewById(R.id.txtDangPhucVuTC);
        txtLoiKetNoi = findViewById(R.id.txtLoiKetNoi);
        txtKhongCoBanPhucVu = findViewById(R.id.txtKhongCoBanPhucVu);
        layoutBanDangPhucVu = findViewById(R.id.layoutBanDangPhucVu);
        layoutLoiKetNoi = findViewById(R.id.layoutLoiKetNoi);

        ((TextView) findViewById(R.id.txtNgayHomNay)).setText(TienIch.ngay(System.currentTimeMillis()));

        findViewById(R.id.btnGoiMonNhanh).setOnClickListener(v -> ThaoTacBan.chonBanDeGoiMon(this));
        findViewById(R.id.btnSoDoBanNhanh).setOnClickListener(v ->
                DieuHuong.chuyenManHinh(this, GiaoDienBanActivity.class));
        findViewById(R.id.btnThemMonNhanh).setOnClickListener(v ->
                startActivity(new Intent(this, GiaoDienThemMonActivity.class)));
        findViewById(R.id.btnHoaDonNhanh).setOnClickListener(v ->
                DieuHuong.chuyenManHinh(this, GiaoDienHoaDonActivity.class));
        findViewById(R.id.btnThuLai).setOnClickListener(v -> taiDuLieu());
        findViewById(R.id.imgThongBao).setOnClickListener(v -> taiDuLieu());
        // Đổi địa chỉ máy chủ xong thì tải lại
        findViewById(R.id.imgCaiDat).setOnClickListener(v -> DieuHuong.hienThiCaiDatMayChu(this, this::taiDuLieu));
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    /** Kết quả gọi API gộp lại để cập nhật giao diện một lần. */
    static class DuLieu {
        JSONObject tongQuan;
        ArrayList<Ban> dsBan;
    }

    private void taiDuLieu() {
        long homNay = TienIch.dauNgay(System.currentTimeMillis());
        ChayNen.chay(this, () -> {
            DuLieu d = new DuLieu();
            d.tongQuan = db.layTongQuan(homNay, homNay + MOT_NGAY);
            d.dsBan = db.layDanhSachBan(null);
            return d;
        }, d -> {
            layoutLoiKetNoi.setVisibility(View.GONE);
            hienThi(d);
        }, loi -> {
            txtLoiKetNoi.setText(loi);
            layoutLoiKetNoi.setVisibility(View.VISIBLE);
        });
    }

    private void hienThi(DuLieu d) {
        JSONObject tq = d.tongQuan;
        JSONObject theoTrangThai = tq.optJSONObject("tables_by_status");
        int trong = theoTrangThai.optInt("EMPTY");
        int phucVu = theoTrangThai.optInt("SERVING");
        int choTinh = theoTrangThai.optInt("WAITING_PAYMENT");
        int daDat = theoTrangThai.optInt("RESERVED");
        int baoTri = theoTrangThai.optInt("MAINTENANCE");

        txtTongSoBan.setText(String.valueOf(tq.optInt("total_tables")));
        txtSoBanTrong.setText(String.valueOf(trong));
        txtSoBanPhucVu.setText(String.valueOf(phucVu + choTinh));
        txtChiTietBan.setText("Trong đó " + choTinh + " bàn chờ thanh toán • " + daDat + " đã đặt • "
                + baoTri + " bảo trì");

        txtDoanhThu.setText(TienIch.tien(tq.optLong("revenue")));
        txtSoHoaDon.setText(tq.optInt("invoice_count") + " hóa đơn • Tiền mặt "
                + TienIch.tien(tq.optLong("cash_revenue")));
        txtDangPhucVu.setText("Đang phục vụ " + tq.optInt("open_orders") + " bàn • Tạm tính "
                + TienIch.tien(tq.optLong("open_amount")));

        // Danh sách bàn đang có khách
        layoutBanDangPhucVu.removeAllViews();
        int dem = 0;
        for (Ban ban : d.dsBan) {
            if (ban.isTrong()) continue;
            dem++;
            View dong = getLayoutInflater().inflate(R.layout.item_hoa_don, layoutBanDangPhucVu, false);
            ((TextView) dong.findViewById(R.id.txtBanHoaDon)).setText(ban.getTenBan() + " • " + ban.getKhuVuc());
            ((TextView) dong.findViewById(R.id.txtMaHoaDonItem)).setText(ban.getSoKhach() + " khách • "
                    + ban.getSoMon() + " món");
            ((TextView) dong.findViewById(R.id.txtThoiGianHoaDon)).setText("Vào " + TienIch.gio(ban.getGioVao())
                    + " (" + TienIch.thoiGianNgoi(ban.getGioVao()) + ")");
            ((TextView) dong.findViewById(R.id.txtTongHoaDon)).setText(TienIch.tien(ban.getTamTinh()));
            boolean choTinhTien = ban.getTrangThai() == Ban.CHO_THANH_TOAN;
            TextView pill = dong.findViewById(R.id.txtTrangThaiHoaDon);
            pill.setText(Ban.tenTrangThai(ban.getTrangThai()));
            pill.setBackgroundResource(choTinhTien ? R.drawable.bg_pill_caramel : R.drawable.bg_pill_amber);
            pill.setTextColor(getColor(choTinhTien ? R.color.white : R.color.status_amber));
            ImageView img = dong.findViewById(R.id.imgLoaiHoaDon);
            img.setImageResource(R.drawable.ic_table);
            img.setImageTintList(ColorStateList.valueOf(getColor(R.color.caramel_dark)));
            img.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.caramel_soft)));
            dong.setOnClickListener(v -> ThaoTacBan.moChiTietOrder(this, ban.getHoaDonId()));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dong.getLayoutParams();
            lp.bottomMargin = Math.round(10 * getResources().getDisplayMetrics().density);
            layoutBanDangPhucVu.addView(dong, lp);
        }
        txtKhongCoBanPhucVu.setVisibility(dem == 0 ? View.VISIBLE : View.GONE);
        if (tq.optInt("total_tables") == 0) {
            txtKhongCoBanPhucVu.setText("Quán chưa có bàn nào. Vào mục Bàn → Thêm để tạo bàn.");
        } else {
            txtKhongCoBanPhucVu.setText("Chưa có bàn nào đang phục vụ");
        }
    }
}
