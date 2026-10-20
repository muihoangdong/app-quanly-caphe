package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.adapter.DongAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.GiaoDienKhach;
import hutech.cntt.app_quanlycafe.data.GioHangKhach;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.DatBan;
import hutech.cntt.app_quanlycafe.model.DonOnline;
import hutech.cntt.app_quanlycafe.model.TaiKhoan;

/**
 * Khách: lịch sử đặt bàn (xem vé / hủy) và đơn món (xem chi tiết / hủy khi quán chưa xác nhận).
 */
public class KhachDonCuaToiActivity extends AppCompatActivity {

    RadioGroup rgLoaiDon;
    ListView lvDon;
    View layoutKhongCo;
    TextView txtKhongCo;

    CafeDatabase db;
    TaiKhoan khach;
    final ArrayList<DatBan> listDatBan = new ArrayList<>();
    final ArrayList<DonOnline> listDon = new ArrayList<>();
    DongAdapter<DatBan> adapterDatBan;
    DongAdapter<DonOnline> adapterDon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PhienDangNhap.kiemTraQuyen(this, true, false, false)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_khach_don_cua_toi);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuongKhach(this, R.id.nav_k_don);

        db = CafeDatabase.getInstance(this);
        khach = PhienDangNhap.layTaiKhoan(this);
        rgLoaiDon = findViewById(R.id.rgLoaiDon);
        lvDon = findViewById(R.id.lvDonCuaToi);
        layoutKhongCo = findViewById(R.id.layoutKhongCoDon);
        txtKhongCo = findViewById(R.id.txtKhongCoDon);

        adapterDatBan = new DongAdapter<>(this, listDatBan, R.layout.item_k_dong_don, this::ganDongDatBan);
        adapterDon = new DongAdapter<>(this, listDon, R.layout.item_k_dong_don, this::ganDongDon);

        rgLoaiDon.setOnCheckedChangeListener((g, id) -> taiDuLieu());
        lvDon.setOnItemClickListener((p, v, position, id) -> {
            if (dangXemDatBan()) {
                hienThiDatBan(listDatBan.get(position));
            } else {
                hienThiDon(listDon.get(position));
            }
        });
        chonTab(getIntent());
        findViewById(R.id.btnBatDauDon).setOnClickListener(v -> DieuHuong.chuyenManHinh(this,
                dangXemDatBan() ? KhachDatBanActivity.class : KhachThucDonActivity.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        chonTab(intent);
    }

    /** Extra "xemDonMon" = true để mở sẵn tab Đơn món. */
    private void chonTab(Intent intent) {
        if (intent.hasExtra("xemDonMon")) {
            rgLoaiDon.check(intent.getBooleanExtra("xemDonMon", false) ? R.id.radDonMon : R.id.radDonDatBan);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    private boolean dangXemDatBan() {
        return rgLoaiDon.getCheckedRadioButtonId() == R.id.radDonDatBan;
    }

    private void taiDuLieu() {
        boolean rong;
        if (dangXemDatBan()) {
            listDatBan.clear();
            listDatBan.addAll(db.layDatBanCuaKhach(khach.getId()));
            lvDon.setAdapter(adapterDatBan);
            rong = listDatBan.isEmpty();
            txtKhongCo.setText("Bạn chưa đặt bàn nào");
        } else {
            listDon.clear();
            listDon.addAll(db.layDanhSachDonOnline(khach.getId(), -1));
            lvDon.setAdapter(adapterDon);
            rong = listDon.isEmpty();
            txtKhongCo.setText("Bạn chưa gọi món nào");
        }
        layoutKhongCo.setVisibility(rong ? View.VISIBLE : View.GONE);
    }

    // ---------------- Đặt bàn ----------------

    private void ganDongDatBan(View dong, DatBan dat) {
        ((ImageView) dong.findViewById(R.id.imgDongDon)).setImageResource(R.drawable.ic_calendar);
        ((TextView) dong.findViewById(R.id.txtTieuDeDongDon)).setText(dat.getTenBan() + " • " + dat.getKhuVuc());
        ((TextView) dong.findViewById(R.id.txtPhuDeDongDon)).setText(TienIch.gio(dat.getThoiGian()) + " • "
                + TienIch.ngay(dat.getThoiGian()) + " • " + dat.getSoKhach() + " người");
        ((TextView) dong.findViewById(R.id.txtMaDongDon)).setText("#" + dat.getMaDat());
        GiaoDienKhach.datPillDatBan(dong.findViewById(R.id.txtTrangThaiDongDon), dat);
        TextView chan = dong.findViewById(R.id.txtChanDongDon);
        if (dat.getTrangThai() == DatBan.CHO_DEN) {
            chan.setVisibility(View.VISIBLE);
            chan.setText("Chạm để xem vé • đưa mã cho nhân viên khi đến quán");
        } else {
            chan.setVisibility(View.GONE);
        }
    }

    private void hienThiDatBan(DatBan dat) {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setView(GiaoDienKhach.taoVeDatBan(this, dat))
                .setPositiveButton("Đóng", null);
        if (dat.getTrangThai() == DatBan.CHO_DEN) {
            b.setNeutralButton("Gọi món trước", (d, w) -> {
                GioHangKhach.datBanIdGoiY = dat.getId();
                DieuHuong.chuyenManHinh(this, KhachThucDonActivity.class);
            });
            b.setNegativeButton("Hủy đặt bàn", (d, w) -> GiaoDienKhach.xacNhan(this, "Hủy đặt bàn",
                    "Bạn chắc chắn muốn hủy lịch " + dat.getTenBan() + " lúc " + TienIch.gio(dat.getThoiGian())
                            + " ngày " + TienIch.ngay(dat.getThoiGian()) + "?", "Hủy đặt bàn", () -> {
                        db.capNhatTrangThaiDatBan(dat.getId(), DatBan.DA_HUY);
                        Toast.makeText(this, "Đã hủy đặt bàn", Toast.LENGTH_SHORT).show();
                        taiDuLieu();
                    }));
        }
        b.show();
    }

    // ---------------- Đơn món ----------------

    private void ganDongDon(View dong, DonOnline don) {
        ((ImageView) dong.findViewById(R.id.imgDongDon)).setImageResource(
                DonOnline.MANG_DI.equals(don.getHinhThuc()) ? R.drawable.ic_bag : R.drawable.ic_coffee);
        ((TextView) dong.findViewById(R.id.txtTieuDeDongDon)).setText(don.getHinhThuc() + " • "
                + TienIch.tien(don.getTongTien()));
        ((TextView) dong.findViewById(R.id.txtPhuDeDongDon)).setText(TienIch.ngayGio(don.getThoiGian())
                + " • " + don.getSoMon() + " món");
        ((TextView) dong.findViewById(R.id.txtMaDongDon)).setText("#" + don.getMaDon());
        GiaoDienKhach.datPillDon(dong.findViewById(R.id.txtTrangThaiDongDon), don);
        TextView chan = dong.findViewById(R.id.txtChanDongDon);
        chan.setVisibility(View.VISIBLE);
        String noiDung = don.getPhuongThuc() + (don.isDaThanhToan() ? " • Đã thanh toán" : " • Chưa thanh toán");
        if (don.getThongTinBan() != null) noiDung += "\n" + don.getThongTinBan();
        chan.setText(noiDung);
    }

    private void hienThiDon(DonOnline don) {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(don.getHinhThuc())
                .setView(GiaoDienKhach.taoChiTietDon(this, don))
                .setPositiveButton("Đóng", null);
        if (don.getTrangThai() == DonOnline.CHO_XAC_NHAN) {
            b.setNegativeButton("Hủy đơn", (d, w) -> GiaoDienKhach.xacNhan(this, "Hủy đơn",
                    "Hủy đơn #" + don.getMaDon() + "?"
                            + (don.isDaThanhToan() ? "\nTiền đã thanh toán sẽ được quán hoàn lại." : ""),
                    "Hủy đơn", () -> {
                        db.capNhatTrangThaiDon(don.getId(), DonOnline.DA_HUY);
                        Toast.makeText(this, "Đã hủy đơn", Toast.LENGTH_SHORT).show();
                        taiDuLieu();
                    }));
        }
        b.show();
    }
}
