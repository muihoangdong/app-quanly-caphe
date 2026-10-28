package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.Ban;
import hutech.cntt.app_quanlycafe.model.HoaDon;
import hutech.cntt.app_quanlycafe.model.NhanVien;


public class GiaoDienTrangChuActivity extends AppCompatActivity {

    static final int SO_DONG_TOI_DA = 3;

    TextView txtLoiKetNoi, txtTenNhanVien, txtVaiTro, txtTieuDeHeader;
    LinearLayout layoutBanDangPhucVu, layoutHoatDong;
    View layoutLoiKetNoi;

    CafeDatabase db;
    ArrayList<Ban> dsBan = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_trang_chu);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_trang_chu);

        db = CafeDatabase.getInstance(this);
        txtLoiKetNoi = findViewById(R.id.txtLoiKetNoi);
        txtTenNhanVien = findViewById(R.id.txtTenNhanVien);
        txtVaiTro = findViewById(R.id.txtVaiTro);
        txtTieuDeHeader = findViewById(R.id.txtTieuDeHeader);
        layoutBanDangPhucVu = findViewById(R.id.layoutBanDangPhucVu);
        layoutHoatDong = findViewById(R.id.layoutHoatDong);
        layoutLoiKetNoi = findViewById(R.id.layoutLoiKetNoi);

        ganThaoTac(R.id.btnSoDoBanNhanh, R.drawable.ic_table, "Sơ đồ bàn", "Xem trạng thái bàn",
                v -> DieuHuong.chuyenManHinh(this, GiaoDienBanActivity.class));
        ganThaoTac(R.id.btnGoiMonNhanh, R.drawable.ic_ly, "Gọi món", "Thêm món nhanh",
                v -> ThaoTacBan.moGoiMon(this));
        ganThaoTac(R.id.btnDonHangNhanh, R.drawable.ic_order, "Đơn hàng", "Xem & xử lý",
                v -> DieuHuong.chuyenManHinh(this, GiaoDienOrderActivity.class));
        ganThaoTac(R.id.btnHoaDonNhanh, R.drawable.ic_receipt, "Hóa đơn", "Đã thanh toán",
                v -> startActivity(new Intent(this, GiaoDienLichSuHoaDonActivity.class)));

        findViewById(R.id.btnTaoDonHang).setOnClickListener(v -> ThaoTacBan.moGoiMon(this));
        findViewById(R.id.txtXemTatCaBan).setOnClickListener(v ->
                DieuHuong.chuyenManHinh(this, GiaoDienBanActivity.class));
        findViewById(R.id.txtXemTatCaHoaDon).setOnClickListener(v ->
                startActivity(new Intent(this, GiaoDienLichSuHoaDonActivity.class)));
        findViewById(R.id.btnDangXuat).setOnClickListener(v -> DieuHuong.xacNhanDangXuat(this));
        findViewById(R.id.btnThuLai).setOnClickListener(v -> taiDuLieu());
        findViewById(R.id.btnMayChu).setOnClickListener(v -> DieuHuong.hienThiCaiDatMayChu(this, this::taiDuLieu));

    }

    private void ganThaoTac(int id, int icon, String ten, String moTa, View.OnClickListener xuLy) {
        View o = findViewById(id);
        ((ImageView) o.findViewById(R.id.imgThaoTac)).setImageResource(icon);
        ((TextView) o.findViewById(R.id.txtTenThaoTac)).setText(ten);
        ((TextView) o.findViewById(R.id.txtMoTaThaoTac)).setText(moTa);
        o.setOnClickListener(xuLy);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!db.daDangNhap()) {
            DieuHuong.dangXuat(this);
            return;
        }
        taiDuLieu();
    }

    private void taiDuLieu() {
        ChayNen.chay(this, () -> new Object[]{
                db.layDanhSachBan(),
                db.timHoaDonDaThanhToan(null, 0, 0, SO_DONG_TOI_DA),
                db.layNguoiDangNhap(),
                db.layCauHinh()
        }, kq -> {
            layoutLoiKetNoi.setVisibility(View.GONE);
            @SuppressWarnings("unchecked") ArrayList<Ban> ban = (ArrayList<Ban>) kq[0];
            @SuppressWarnings("unchecked") ArrayList<HoaDon> hoaDon = (ArrayList<HoaDon>) kq[1];
            @SuppressWarnings("unchecked") LinkedHashMap<String, String> cauHinh = (LinkedHashMap<String, String>) kq[3];
            NhanVien nv = (NhanVien) kq[2];
            if (nv == null) {
                // Tài khoản vừa bị quản lý khóa
                Toast.makeText(this, "Tài khoản đã bị khóa, hãy đăng nhập lại", Toast.LENGTH_LONG).show();
                DieuHuong.dangXuat(this);
                return;
            }
            txtTenNhanVien.setText(nv.getHoTen());
            txtVaiTro.setText(nv.getVaiTro());
            txtTieuDeHeader.setText(cauHinh.get(CafeDatabase.CD_TEN_QUAN));
            dsBan = ban;
            hienThiBan();
            hienThiHoatDong(hoaDon);
        }, loi -> {
            txtLoiKetNoi.setText(loi);
            layoutLoiKetNoi.setVisibility(View.VISIBLE);
        });
    }

    private View taoDong(LinearLayout cha, int icon, String tieuDe, String phu, String phai) {
        View dong = getLayoutInflater().inflate(R.layout.item_dong_tom_tat, cha, false);
        ((ImageView) dong.findViewById(R.id.imgDong)).setImageResource(icon);
        ((TextView) dong.findViewById(R.id.txtTieuDeDong)).setText(tieuDe);
        ((TextView) dong.findViewById(R.id.txtPhuDong)).setText(phu);
        ((TextView) dong.findViewById(R.id.txtPhaiDong)).setText(phai);
        cha.addView(dong);
        return dong;
    }

    private void themDongTrong(LinearLayout cha, String thongBao) {
        TextView txt = new TextView(this);
        txt.setText(thongBao);
        txt.setTextColor(getColor(R.color.text_hint));
        txt.setTextSize(13);
        int dem = Math.round(16 * getResources().getDisplayMetrics().density);
        txt.setPadding(dem, dem, dem, dem);
        cha.addView(txt);
    }

    private void hienThiBan() {
        layoutBanDangPhucVu.removeAllViews();
        int dem = 0;
        for (Ban ban : dsBan) {
            if (ban.isTrong() || dem >= SO_DONG_TOI_DA) continue;
            dem++;
            View dong = taoDong(layoutBanDangPhucVu, R.drawable.ic_table, ban.getTenBan(),
                    ban.getSoMon() + " món - " + TienIch.tien(ban.getTamTinh()), TienIch.gio(ban.getGioVao()));
            dong.setOnClickListener(v -> ThaoTacBan.moThanhToan(this, ban.getHoaDonId()));
        }
        if (dem == 0) {
            themDongTrong(layoutBanDangPhucVu, dsBan.isEmpty()
                    ? "Quán chưa có bàn nào. Vào mục Bàn để thêm bàn."
                    : "Chưa có bàn nào đang phục vụ");
        }
    }

    private void hienThiHoatDong(ArrayList<HoaDon> dsHoaDon) {
        layoutHoatDong.removeAllViews();
        for (HoaDon hd : dsHoaDon) {
            View dong = taoDong(layoutHoatDong, R.drawable.ic_receipt, "Hóa đơn #" + hd.getMaHoaDon(),
                    "Đã thanh toán - " + TienIch.tien(hd.getTongTien()), TienIch.gio(hd.getGioRa()));
            dong.setOnClickListener(v -> GiaoDienLichSuHoaDonActivity.hienThiChiTiet(this, hd));
        }
        if (dsHoaDon.isEmpty()) themDongTrong(layoutHoatDong, "Chưa có hóa đơn nào được thanh toán");
    }
}
