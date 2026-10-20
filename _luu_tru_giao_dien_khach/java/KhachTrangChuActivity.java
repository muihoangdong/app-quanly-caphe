package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.GiaoDienKhach;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.DatBan;
import hutech.cntt.app_quanlycafe.model.DonOnline;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;
import hutech.cntt.app_quanlycafe.model.ThucUong;

/**
 * Khách: trang chủ (tham khảo trang homepage) — lối tắt Đặt bàn / Gọi món,
 * lịch đặt bàn sắp tới, danh mục và món nổi bật.
 */
public class KhachTrangChuActivity extends AppCompatActivity {

    CafeDatabase db;
    TaiKhoan khach;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PhienDangNhap.kiemTraQuyen(this, true, false, false)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_khach_trang_chu);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuongKhach(this, R.id.nav_k_trang_chu);
        db = CafeDatabase.getInstance(this);

        findViewById(R.id.oDatBan).setOnClickListener(v ->
                DieuHuong.chuyenManHinh(this, KhachDatBanActivity.class));
        findViewById(R.id.layoutBanner).setOnClickListener(v ->
                DieuHuong.chuyenManHinh(this, KhachDatBanActivity.class));
        findViewById(R.id.oGoiMon).setOnClickListener(v ->
                DieuHuong.chuyenManHinh(this, KhachThucDonActivity.class));
        findViewById(R.id.txtXemTatCaMon).setOnClickListener(v ->
                DieuHuong.chuyenManHinh(this, KhachThucDonActivity.class));
        findViewById(R.id.oDonCuaToi).setOnClickListener(v ->
                DieuHuong.chuyenManHinh(this, KhachDonCuaToiActivity.class));
        findViewById(R.id.imgThongBaoKhach).setOnClickListener(v -> hienThiThongBao());
    }

    @Override
    protected void onResume() {
        super.onResume();
        khach = PhienDangNhap.layTaiKhoan(this);
        if (khach == null) return;
        hienThiLoiChao();
        hienThiDatBanSapToi();
        hienThiDanhMuc();
        hienThiMonNoiBat();
    }

    private void hienThiLoiChao() {
        int gio = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String buoi = gio < 11 ? "Chào buổi sáng ☀" : gio < 14 ? "Chào buổi trưa"
                : gio < 18 ? "Chào buổi chiều" : "Chào buổi tối 🌙";
        ((TextView) findViewById(R.id.txtLoiChaoTrangChu)).setText(buoi);
        ((TextView) findViewById(R.id.txtTenTrangChu)).setText(khach.getHoTen());
        ((TextView) findViewById(R.id.txtAvatarTrangChu)).setText(GiaoDienKhach.chuCaiDau(khach.getHoTen()));
    }

    private void hienThiDatBanSapToi() {
        View the = findViewById(R.id.layoutDatBanSapToi);
        ArrayList<DatBan> ds = db.layDatBanSapToi(khach.getId());
        if (ds.isEmpty()) {
            the.setVisibility(View.GONE);
            return;
        }
        DatBan dat = ds.get(0);
        the.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtDatBanSapToi)).setText(dat.getTenBan() + " • "
                + TienIch.gio(dat.getThoiGian()) + " " + TienIch.ngay(dat.getThoiGian()));
        ((TextView) findViewById(R.id.txtMaDatBanSapToi)).setText("#" + dat.getMaDat()
                + (ds.size() > 1 ? "  (+" + (ds.size() - 1) + " lượt khác)" : ""));
        the.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setView(GiaoDienKhach.taoVeDatBan(this, dat))
                .setPositiveButton("Đóng", null)
                .show());
    }

    private void hienThiDanhMuc() {
        LinearLayout layout = findViewById(R.id.layoutDanhMucTrangChu);
        layout.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (String dm : db.layDanhSachDanhMuc()) {
            View o = inflater.inflate(R.layout.item_k_danh_muc, layout, false);
            ((ImageView) o.findViewById(R.id.imgDanhMucTron)).setImageResource(TienIch.bieuTuongDanhMuc(dm));
            ((TextView) o.findViewById(R.id.txtDanhMucTron)).setText(dm);
            o.setOnClickListener(v -> {
                Intent in = new Intent(this, KhachThucDonActivity.class);
                in.putExtra("danhMuc", dm);
                in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(in);
                overridePendingTransition(0, 0);
            });
            layout.addView(o);
        }
    }

    private void hienThiMonNoiBat() {
        LinearLayout layout = findViewById(R.id.layoutMonNoiBat);
        layout.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int dem = 0;
        // Danh sách đã xếp món bán chạy lên đầu
        for (ThucUong tu : db.layDanhSachThucUong(null, null, true)) {
            if (dem++ >= 6) break;
            View the = inflater.inflate(R.layout.item_k_mon_ngang, layout, false);
            TienIch.hienThiAnhMon(the.findViewById(R.id.imgMonNgang), tu.getUriAnh(), tu.getDanhMuc());
            ((TextView) the.findViewById(R.id.txtTenMonNgang)).setText(tu.getTenMon());
            ((TextView) the.findViewById(R.id.txtGiaMonNgang)).setText(TienIch.tien(tu.getGia()));
            View.OnClickListener chon = v -> GiaoDienKhach.hienThiChonMon(this, tu, null);
            the.setOnClickListener(chon);
            the.findViewById(R.id.btnThemMonNgang).setOnClickListener(chon);
            layout.addView(the);
        }
    }

    private void hienThiThongBao() {
        StringBuilder sb = new StringBuilder();
        for (DatBan d : db.layDatBanSapToi(khach.getId())) {
            sb.append("• Bạn có lịch đặt ").append(d.getTenBan()).append(" lúc ")
                    .append(TienIch.gio(d.getThoiGian())).append(" ngày ")
                    .append(TienIch.ngay(d.getThoiGian())).append("\n");
        }
        for (DonOnline don : db.layDanhSachDonOnline(khach.getId(), DonOnline.DANG_PHA_CHE)) {
            sb.append("• Đơn #").append(don.getMaDon()).append(" đang được pha chế\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Thông báo")
                .setMessage(sb.length() == 0 ? "Không có thông báo mới." : sb.toString().trim())
                .setPositiveButton("Đóng", null)
                .show();
    }
}
