package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.adapter.HoaDonAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.HoaDon;

/**
 * Hóa đơn: đơn đang phục vụ, lịch sử hóa đơn đã thanh toán và đơn đã hủy.
 */
public class GiaoDienHoaDonActivity extends AppCompatActivity {

    ListView lvHoaDon;
    RadioGroup rgTrangThai;
    TextView txtTongKet, txtKhongCoHoaDon;

    CafeDatabase db;
    ArrayList<HoaDon> listHoaDon;
    HoaDonAdapter adapter;
    int lanTai = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_hoa_don);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_hoa_don);

        db = CafeDatabase.getInstance(this);
        lvHoaDon = findViewById(R.id.lvHoaDon);
        rgTrangThai = findViewById(R.id.rgTrangThaiHoaDon);
        txtTongKet = findViewById(R.id.txtTongKetHoaDon);
        txtKhongCoHoaDon = findViewById(R.id.txtKhongCoHoaDon);

        listHoaDon = new ArrayList<>();
        adapter = new HoaDonAdapter(this, listHoaDon);
        lvHoaDon.setAdapter(adapter);

        rgTrangThai.setOnCheckedChangeListener((group, checkedId) -> taiDuLieu());

        lvHoaDon.setOnItemClickListener((parent, view, position, id) -> {
            HoaDon hd = listHoaDon.get(position);
            if (hd.getTrangThai() == HoaDon.DANG_PHUC_VU) {
                ThaoTacBan.moChiTietOrder(this, hd.getId());
            } else {
                hienThiChiTiet(hd);
            }
        });

        findViewById(R.id.imgThongBao).setOnClickListener(v -> taiDuLieu());
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    private void taiDuLieu() {
        int id = rgTrangThai.getCheckedRadioButtonId();
        int trangThai = -1;
        if (id == R.id.radDangPhucVu) trangThai = HoaDon.DANG_PHUC_VU;
        else if (id == R.id.radDaThanhToan) trangThai = HoaDon.DA_THANH_TOAN;
        else if (id == R.id.radDaHuy) trangThai = HoaDon.DA_HUY;

        int loc = trangThai;
        int lan = ++lanTai;
        ChayNen.chay(this, () -> db.layDanhSachHoaDon(loc, 0, 0), ds -> {
            if (lan != lanTai) return;
            listHoaDon.clear();
            listHoaDon.addAll(ds);
            adapter.notifyDataSetChanged();

            long doanhThu = 0;
            for (HoaDon hd : listHoaDon) {
                if (hd.getTrangThai() == HoaDon.DA_THANH_TOAN) doanhThu += hd.getTongTien();
            }
            txtTongKet.setText(listHoaDon.size() + " đơn • Thu " + TienIch.tien(doanhThu));
            txtKhongCoHoaDon.setVisibility(listHoaDon.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void hienThiChiTiet(HoaDon hd) {
        ChayNen.chay(this, () -> db.layChiTietHoaDon(hd.getId()), dsMon -> {
            View view = getLayoutInflater().inflate(R.layout.dialog_chi_tiet_hoa_don, null);
            TextView txtThongTin = view.findViewById(R.id.txtThongTinChiTiet);
            LinearLayout layoutMon = view.findViewById(R.id.layoutMonChiTiet);
            TextView txtTongKetCT = view.findViewById(R.id.txtTongKetChiTiet);
            TextView txtTongCong = view.findViewById(R.id.txtTongCongChiTiet);

            txtThongTin.setText("#" + hd.getMaHoaDon() + "\n"
                    + "Vào " + TienIch.ngayGio(hd.getGioVao())
                    + (hd.getGioRa() > 0 ? " • Ra " + TienIch.gio(hd.getGioRa()) : "")
                    + "\n" + hd.getSoKhach() + " khách"
                    + (hd.getPhuongThuc() != null ? " • " + hd.getPhuongThuc() : ""));
            TienIch.hienThiDongMon(layoutMon, dsMon);

            if (hd.getTrangThai() == HoaDon.DA_THANH_TOAN) {
                String tongKet = "Tổng tiền hàng: " + TienIch.tien(hd.getTamTinh());
                if (hd.getGiamGia() > 0) tongKet += "\nGiảm giá: -" + TienIch.tien(hd.getGiamGia());
                tongKet += "\nVAT 8%: +" + TienIch.tien(hd.getVat());
                if ("Tiền mặt".equals(hd.getPhuongThuc())) {
                    tongKet += "\nKhách đưa: " + TienIch.tien(hd.getKhachDua())
                            + "\nTiền thừa: " + TienIch.tien(hd.getKhachDua() - hd.getTongTien());
                }
                txtTongKetCT.setText(tongKet);
                txtTongCong.setText(TienIch.tien(hd.getTongTien()));
            } else {
                txtTongKetCT.setText("Order đã bị hủy, không ghi nhận doanh thu.");
                txtTongCong.setText(TienIch.tien(hd.getTamTinh()));
                txtTongCong.setTextColor(getColor(R.color.status_red));
            }

            new AlertDialog.Builder(this)
                    .setTitle(hd.getTenBan() + " • " + hd.getKhuVuc())
                    .setView(view)
                    .setPositiveButton("Đóng", null)
                    .setNeutralButton("In lại", (d, w) ->
                            Toast.makeText(this, "Đã gửi lệnh in hóa đơn " + hd.getMaHoaDon(),
                                    Toast.LENGTH_SHORT).show())
                    .show();
        });
    }
}
