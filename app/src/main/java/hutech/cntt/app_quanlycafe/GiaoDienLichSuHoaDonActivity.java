package hutech.cntt.app_quanlycafe;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

import hutech.cntt.app_quanlycafe.adapter.HoaDonAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.HoaDon;



public class GiaoDienLichSuHoaDonActivity extends AppCompatActivity {

    ListView lvHoaDon;
    TextView txtTongKet, txtKhongCoHoaDon, txtTuNgay, txtDenNgay;

    CafeDatabase db;
    ArrayList<HoaDon> listHoaDon;
    HoaDonAdapter adapter;
    int lanTai = 0;


    long tuNgay, denNgay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_lich_su_hoa_don);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhTieuDe(this, "Lịch sử hóa đơn", false);

        db = CafeDatabase.getInstance(this);
        lvHoaDon = findViewById(R.id.lvHoaDon);
        txtTongKet = findViewById(R.id.txtTongKetHoaDon);
        txtKhongCoHoaDon = findViewById(R.id.txtKhongCoHoaDon);
        txtTuNgay = findViewById(R.id.txtTuNgay);
        txtDenNgay = findViewById(R.id.txtDenNgay);
        TextView edtTim = findViewById(R.id.edtTimHoaDon);

        Calendar c = Calendar.getInstance();
        denNgay = TienIch.dauNgay(c.getTimeInMillis());
        c.set(Calendar.DAY_OF_MONTH, 1);
        tuNgay = TienIch.dauNgay(c.getTimeInMillis());

        listHoaDon = new ArrayList<>();
        adapter = new HoaDonAdapter(this, listHoaDon);
        lvHoaDon.setAdapter(adapter);
        lvHoaDon.setOnItemClickListener((parent, view, position, id) ->
                hienThiChiTiet(this, listHoaDon.get(position)));

        findViewById(R.id.btnTuNgay).setOnClickListener(v -> chonNgay(true));
        findViewById(R.id.btnDenNgay).setOnClickListener(v -> chonNgay(false));
        edtTim.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                taiDuLieu();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    private void chonNgay(boolean laTuNgay) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(laTuNgay ? tuNgay : denNgay);
        new DatePickerDialog(this, (picker, nam, thang, ngay) -> {
            Calendar chon = Calendar.getInstance();
            chon.set(nam, thang, ngay);
            long moc = TienIch.dauNgay(chon.getTimeInMillis());
            if (laTuNgay) {
                tuNgay = moc;
                if (denNgay < tuNgay) denNgay = tuNgay;
            } else {
                denNgay = moc;
                if (tuNgay > denNgay) tuNgay = denNgay;
            }
            taiDuLieu();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void taiDuLieu() {
        txtTuNgay.setText(TienIch.ngay(tuNgay));
        txtDenNgay.setText(TienIch.ngay(denNgay));
        String tuKhoa = ((TextView) findViewById(R.id.edtTimHoaDon)).getText().toString();
        long tu = tuNgay;
        long den = denNgay + 24L * 60 * 60 * 1000;
        int lan = ++lanTai;
        ChayNen.chay(this, () -> db.timHoaDonDaThanhToan(tuKhoa, tu, den, 0), ds -> {
            if (lan != lanTai) return;
            listHoaDon.clear();
            listHoaDon.addAll(ds);
            adapter.notifyDataSetChanged();

            long doanhThu = 0;
            for (HoaDon hd : listHoaDon) doanhThu += hd.getTongTien();
            txtTongKet.setText(listHoaDon.size() + " hóa đơn • " + TienIch.tien(doanhThu));
            txtKhongCoHoaDon.setVisibility(listHoaDon.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    public static void hienThiChiTiet(Activity activity, HoaDon hd) {
        hienThiChiTiet(activity, hd, null);
    }

    public static void hienThiChiTiet(Activity activity, HoaDon hd, Runnable sauKhiDong) {
        ChayNen.chay(activity, () -> CafeDatabase.getInstance(activity).layChiTietHoaDon(hd.getId()),
                dsMon -> hienThiHoaDon(activity, hd, dsMon, sauKhiDong));
    }

    public static void hienThiHoaDon(Activity activity, HoaDon hd, ArrayList<ChiTietHoaDon> dsMon,
                                     Runnable sauKhiDong) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_chi_tiet_hoa_don, null);
        TextView txtThongTin = view.findViewById(R.id.txtThongTinChiTiet);
        LinearLayout layoutMon = view.findViewById(R.id.layoutMonChiTiet);
        TextView txtTongKetCT = view.findViewById(R.id.txtTongKetChiTiet);
        TextView txtTongCong = view.findViewById(R.id.txtTongCongChiTiet);

        StringBuilder thongTin = new StringBuilder("#" + hd.getMaHoaDon() + " • " + hd.getTenBan());
        thongTin.append("\nVào ").append(TienIch.ngayGio(hd.getGioVao()));
        if (hd.getGioRa() > 0) thongTin.append(" • Ra ").append(TienIch.gio(hd.getGioRa()));
        thongTin.append("\n").append(hd.getSoKhach()).append(" khách");
        if (hd.getPhuongThuc() != null) thongTin.append(" • ").append(hd.getPhuongThuc());
        if (hd.getTenNhanVien() != null) thongTin.append("\nNhân viên: ").append(hd.getTenNhanVien());
        txtThongTin.setText(thongTin);
        TienIch.hienThiDongMon(layoutMon, dsMon);

        if (hd.getTrangThai() == HoaDon.DA_HUY) {
            txtTongKetCT.setText("Đơn đã bị hủy, không ghi nhận doanh thu.");
            txtTongCong.setText(TienIch.tien(hd.getTamTinh()));
            txtTongCong.setTextColor(activity.getColor(R.color.status_red));
        } else {
            String tongKet = "Tạm tính: " + TienIch.tien(hd.getTamTinh());
            tongKet += "\nGiảm giá: " + (hd.getGiamGia() > 0 ? "-" + TienIch.tien(hd.getGiamGia()) : "0đ");
            if (HoaDon.TIEN_MAT.equals(hd.getPhuongThuc())) {
                tongKet += "\nKhách đưa: " + TienIch.tien(hd.getKhachDua())
                        + "\nTiền thừa: " + TienIch.tien(hd.getKhachDua() - hd.getTongTien());
            }
            txtTongKetCT.setText(tongKet);
            txtTongCong.setText(TienIch.tien(hd.getTongTien()));
        }

        String tieuDe = hd.getTrangThai() == HoaDon.DANG_PHUC_VU ? "Hóa đơn tạm tính" : "Hóa đơn #" + hd.getMaHoaDon();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(tieuDe)
                .setView(view)
                .setCancelable(sauKhiDong == null)
                .setPositiveButton("Đóng", (d, w) -> {
                    if (sauKhiDong != null) sauKhiDong.run();
                });
        if (hd.getTrangThai() == HoaDon.DA_THANH_TOAN) {
            builder.setNeutralButton("In lại", (d, w) -> {
                Toast.makeText(activity, "Đã gửi lệnh in hóa đơn #" + hd.getMaHoaDon(), Toast.LENGTH_SHORT).show();
                if (sauKhiDong != null) sauKhiDong.run();
            });
        }
        builder.show();
    }
}
