package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.adapter.GoiMonAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.CafeDatabase.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.HoaDon;


public class GiaoDienGoiMonActivity extends AppCompatActivity {

    ListView lvGoiMon;
    TextView txtNhanTamTinh, txtTamTinh;
    MaterialButton btnChuyenThanhToan;

    CafeDatabase db;
    int hoaDonId;
    HoaDon hoaDon;
    ArrayList<ChiTietHoaDon> listMon;
    GoiMonAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_goi_mon);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);
        hoaDonId = getIntent().getIntExtra("hoaDonId", 0);

        DieuHuong.ganThanhTieuDe(this, "Chi tiết đơn", false);

        lvGoiMon = findViewById(R.id.lvGoiMon);
        txtNhanTamTinh = findViewById(R.id.txtNhanTamTinh);
        txtTamTinh = findViewById(R.id.txtTamTinh);
        btnChuyenThanhToan = findViewById(R.id.btnChuyenThanhToan);

        listMon = new ArrayList<>();
        adapter = new GoiMonAdapter(this, listMon, new GoiMonAdapter.OnMonListener() {
            @Override
            public void onDoiTrangThai(ChiTietHoaDon ct) {
                int moi = ct.getTrangThai() == ChiTietHoaDon.DA_LEN_MON
                        ? ChiTietHoaDon.DANG_PHA_CHE : ChiTietHoaDon.DA_LEN_MON;
                ghi(() -> {
                    db.capNhatTrangThaiMon(ct.getId(), moi);
                    return null;
                });
            }

            @Override
            public void onDoiSoLuong(ChiTietHoaDon ct, int soLuongMoi) {
                if (soLuongMoi <= 0) {
                    xacNhanXoaMon(ct);
                } else {
                    ghi(() -> {
                        db.capNhatSoLuong(ct.getId(), soLuongMoi);
                        return null;
                    });
                }
            }
        });
        lvGoiMon.setAdapter(adapter);

        lvGoiMon.setOnItemLongClickListener((parent, view, position, id) -> {
            xacNhanXoaMon(listMon.get(position));
            return true;
        });

        btnChuyenThanhToan.setOnClickListener(v -> {
            if (listMon.isEmpty()) {
                Toast.makeText(this, "Bàn chưa có món để thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }
            ThaoTacBan.moThanhToan(this, hoaDonId);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }


    private void ghi(ChayNen.Viec<?> viec) {
        ChayNen.chay(this, () -> {
            viec.lam();
            return true;
        }, ok -> taiDuLieu());
    }

    private void taiDuLieu() {
        ChayNen.chay(this, () -> {
            Object[] kq = {db.layHoaDon(hoaDonId), db.layChiTietHoaDon(hoaDonId)};
            return kq;
        }, kq -> {
            @SuppressWarnings("unchecked") ArrayList<ChiTietHoaDon> dsMon = (ArrayList<ChiTietHoaDon>) kq[1];
            hienThi((HoaDon) kq[0], dsMon);
        }, loi -> {
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void hienThi(HoaDon hd, ArrayList<ChiTietHoaDon> dsMon) {
        hoaDon = hd;
        if (hoaDon.getTrangThai() != HoaDon.DANG_PHUC_VU) {
            finish();
            return;
        }

        listMon.clear();
        listMon.addAll(dsMon);
        adapter.notifyDataSetChanged();

        int tongMon = 0;
        long tamTinh = 0;
        for (ChiTietHoaDon ct : listMon) {
            tongMon += ct.getSoLuong();
            tamTinh += ct.getThanhTien();
        }

        long tong = TienIch.lamTron(tamTinh);
        txtNhanTamTinh.setText("Tạm tính (" + tongMon + " món)");
        txtTamTinh.setText(TienIch.tien(tamTinh));
        btnChuyenThanhToan.setText("Thanh toán  " + TienIch.tien(tong));
    }

    private void xacNhanXoaMon(ChiTietHoaDon ct) {
        new AlertDialog.Builder(this)
                .setTitle("Bỏ món")
                .setMessage("Bỏ \"" + ct.getTenMon() + "\" (" + ct.getSoLuong() + " phần) khỏi order?")
                .setNegativeButton("Không", null)
                .setPositiveButton("Bỏ món", (d, w) -> ghi(() -> {
                    db.xoaChiTiet(ct.getId());
                    return null;
                }))
                .show();
    }
}
