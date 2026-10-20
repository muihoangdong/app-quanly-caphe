package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.adapter.GoiMonAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.HoaDon;

/**
 * Chi tiết order của một bàn: danh sách món, trạng thái pha chế, tạm tính.
 */
public class GiaoDienGoiMonActivity extends AppCompatActivity {

    public static final double THUE_VAT = 0.08;

    ListView lvGoiMon;
    TextView txtTenBan, txtKhuVuc, txtThongTin, txtMaOrder;
    TextView txtTienDo, txtDaPhucVu, txtDangPha, txtSoDongMon, txtChuaCoMon;
    ProgressBar pbTienDo;
    TextView txtNhanTamTinh, txtTamTinh, txtVat;
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

        ((TextView) findViewById(R.id.txtTieuDeManHinh)).setText("Chi tiết order");
        findViewById(R.id.btnQuayLai).setOnClickListener(v -> finish());

        lvGoiMon = findViewById(R.id.lvGoiMon);
        View header = getLayoutInflater().inflate(R.layout.layout_goi_mon_header, lvGoiMon, false);
        lvGoiMon.addHeaderView(header, null, false);

        txtTenBan = header.findViewById(R.id.txtTenBanOrder);
        txtKhuVuc = header.findViewById(R.id.txtKhuVucOrder);
        txtThongTin = header.findViewById(R.id.txtThongTinOrder);
        txtMaOrder = header.findViewById(R.id.txtMaOrder);
        txtTienDo = header.findViewById(R.id.txtTienDo);
        txtDaPhucVu = header.findViewById(R.id.txtDaPhucVu);
        txtDangPha = header.findViewById(R.id.txtDangPha);
        txtSoDongMon = header.findViewById(R.id.txtSoDongMon);
        txtChuaCoMon = header.findViewById(R.id.txtChuaCoMon);
        pbTienDo = header.findViewById(R.id.pbTienDo);
        txtNhanTamTinh = findViewById(R.id.txtNhanTamTinh);
        txtTamTinh = findViewById(R.id.txtTamTinh);
        txtVat = findViewById(R.id.txtVat);
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
            int viTri = position - lvGoiMon.getHeaderViewsCount();
            if (viTri >= 0 && viTri < listMon.size()) xacNhanXoaMon(listMon.get(viTri));
            return true;
        });

        header.findViewById(R.id.btnThemMon).setOnClickListener(v ->
                ThaoTacBan.moThucDon(this, hoaDonId, false));
        header.findViewById(R.id.btnGiucBar).setOnClickListener(v -> giucBar());
        header.findViewById(R.id.btnTamTinh).setOnClickListener(v -> inTamTinh());
        header.findViewById(R.id.btnHuyOrder).setOnClickListener(v -> huyOrder());
        btnChuyenThanhToan.setOnClickListener(v -> {
            if (listMon.isEmpty()) {
                Toast.makeText(this, "Bàn chưa có món để thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent in = new Intent(this, GiaoDienThanhToanActivity.class);
            in.putExtra("hoaDonId", hoaDonId);
            in.putExtra("tuGoiMon", true);
            startActivity(in);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    /** Ghi thay đổi lên máy chủ rồi tải lại order. */
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
            // Đơn đã bị gộp sang bàn khác (không còn tồn tại) hoặc mất kết nối
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void hienThi(HoaDon hd, ArrayList<ChiTietHoaDon> dsMon) {
        hoaDon = hd;
        if (hoaDon.getTrangThai() != HoaDon.DANG_PHUC_VU) {
            // Hóa đơn đã thanh toán / hủy
            finish();
            return;
        }
        txtTenBan.setText(hoaDon.getTenBan());
        txtKhuVuc.setText(hoaDon.getKhuVuc());
        txtThongTin.setText(hoaDon.getSoKhach() + " khách • Vào: " + TienIch.gio(hoaDon.getGioVao())
                + " (" + TienIch.thoiGianNgoi(hoaDon.getGioVao()) + ")");
        txtMaOrder.setText("#" + hoaDon.getMaHoaDon());

        listMon.clear();
        listMon.addAll(dsMon);
        adapter.notifyDataSetChanged();

        int tongMon = 0, daLen = 0;
        long tamTinh = 0;
        for (ChiTietHoaDon ct : listMon) {
            tongMon += ct.getSoLuong();
            if (ct.getTrangThai() == ChiTietHoaDon.DA_LEN_MON) daLen += ct.getSoLuong();
            tamTinh += ct.getThanhTien();
        }
        txtSoDongMon.setText("Danh sách gọi món (" + listMon.size() + ")");
        txtChuaCoMon.setVisibility(listMon.isEmpty() ? View.VISIBLE : View.GONE);
        txtTienDo.setText("Đã lên " + daLen + "/" + tongMon + " món");
        pbTienDo.setProgress(tongMon == 0 ? 0 : daLen * 100 / tongMon);
        txtDaPhucVu.setText("● " + daLen + " món đã phục vụ");
        txtDangPha.setText("● " + (tongMon - daLen) + " món đang pha chế");

        long vat = Math.round(tamTinh * THUE_VAT);
        long tong = TienIch.lamTron(tamTinh + vat);
        txtNhanTamTinh.setText("Tạm tính (" + tongMon + " món)");
        txtTamTinh.setText(TienIch.tien(tamTinh));
        txtVat.setText("+" + TienIch.tien(vat));
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

    private void giucBar() {
        int dangPha = 0;
        for (ChiTietHoaDon ct : listMon) {
            if (ct.getTrangThai() == ChiTietHoaDon.DANG_PHA_CHE) dangPha += ct.getSoLuong();
        }
        if (dangPha == 0) {
            Toast.makeText(this, "Tất cả món đã được phục vụ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Đã giục quầy Bar " + dangPha + " món của "
                    + hoaDon.getTenBan(), Toast.LENGTH_SHORT).show();
        }
    }

    /** In phiếu tạm tính và chuyển bàn sang trạng thái chờ thanh toán. */
    private void inTamTinh() {
        if (listMon.isEmpty()) {
            Toast.makeText(this, "Bàn chưa có món", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        long tamTinh = 0;
        for (ChiTietHoaDon ct : listMon) {
            sb.append(ct.getSoLuong()).append(" × ").append(ct.getTenMon())
                    .append("\n      ").append(TienIch.tien(ct.getThanhTien())).append("\n");
            tamTinh += ct.getThanhTien();
        }
        long vat = Math.round(tamTinh * THUE_VAT);
        sb.append("\nTạm tính: ").append(TienIch.tien(tamTinh));
        sb.append("\nVAT 8%: ").append(TienIch.tien(vat));
        sb.append("\nTỔNG: ").append(TienIch.tien(TienIch.lamTron(tamTinh + vat)));

        // Bàn chuyển sang "Chờ thanh toán"
        ChayNen.chay(this, () -> {
            db.yeuCauThanhToan(hoaDonId);
            return true;
        }, ok -> new AlertDialog.Builder(this)
                .setTitle("Phiếu tạm tính • " + hoaDon.getTenBan())
                .setMessage(sb.toString())
                .setPositiveButton("Đóng", null)
                .show());
    }

    private void huyOrder() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy order " + hoaDon.getTenBan() + "?")
                .setMessage("Toàn bộ món sẽ bị hủy và bàn trở về trạng thái trống.")
                .setNegativeButton("Không", null)
                .setPositiveButton("Hủy order", (d, w) -> ChayNen.chay(this, () -> {
                    db.huyHoaDon(hoaDonId);
                    return true;
                }, ok -> {
                    Toast.makeText(this, "Đã hủy order " + hoaDon.getTenBan(), Toast.LENGTH_SHORT).show();
                    finish();
                }))
                .show();
    }
}
