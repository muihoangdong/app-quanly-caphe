package hutech.cntt.app_quanlycafe;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.HoaDon;
import hutech.cntt.app_quanlycafe.view.BieuDoCot;

/**
 * Báo cáo doanh thu: tổng thu, số hóa đơn, món bán chạy, khung giờ, phương thức thanh toán.
 */
public class GiaoDienBaoCaoActivity extends AppCompatActivity {

    static final long MUC_TIEU_MOT_NGAY = 3_000_000;
    static final long TIEN_DAU_CA = 1_000_000;
    static final int GIO_MO_CUA = 7;
    static final int GIO_DONG_CUA = 22;
    static final long MOT_NGAY = 24L * 60 * 60 * 1000;

    static final String[] PHUONG_THUC = {"VietQR", "Tiền mặt", "Quẹt POS", "Ví MoMo/Zalo"};
    static final int[] MAU_PHUONG_THUC = {
            Color.parseColor("#2E7D32"), Color.parseColor("#F57C00"),
            Color.parseColor("#271310"), Color.parseColor("#9C1C6B")};

    RadioGroup rgKhoangThoiGian;
    TextView txtTangTruong, txtTongDoanhThu, txtSoSanh, txtMucTieu, txtPhanTramMucTieu;
    TextView txtSoHoaDon, txtSoKhachPhucVu, txtTrungBinhDon, txtMonTrenDon;
    TextView txtMonBanChay, txtChiTietMonBanChay, txtDoanhThuMonBanChay, txtGioCaoDiem;
    TextView txtSoPhuongThuc, txtDoiSoat, txtTienMatKet;
    ProgressBar pbMucTieu;
    BieuDoCot bieuDoGio;
    LinearLayout thanhTiLe, layoutPhuongThuc, layoutTopMon;

    CafeDatabase db;
    long tienMatTrongKet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_bao_cao);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_bao_cao);

        db = CafeDatabase.getInstance(this);

        rgKhoangThoiGian = findViewById(R.id.rgKhoangThoiGian);
        txtTangTruong = findViewById(R.id.txtTangTruong);
        txtTongDoanhThu = findViewById(R.id.txtTongDoanhThu);
        txtSoSanh = findViewById(R.id.txtSoSanh);
        txtMucTieu = findViewById(R.id.txtMucTieu);
        txtPhanTramMucTieu = findViewById(R.id.txtPhanTramMucTieu);
        pbMucTieu = findViewById(R.id.pbMucTieu);
        txtSoHoaDon = findViewById(R.id.txtSoHoaDon);
        txtSoKhachPhucVu = findViewById(R.id.txtSoKhachPhucVu);
        txtTrungBinhDon = findViewById(R.id.txtTrungBinhDon);
        txtMonTrenDon = findViewById(R.id.txtMonTrenDon);
        txtMonBanChay = findViewById(R.id.txtMonBanChay);
        txtChiTietMonBanChay = findViewById(R.id.txtChiTietMonBanChay);
        txtDoanhThuMonBanChay = findViewById(R.id.txtDoanhThuMonBanChay);
        txtGioCaoDiem = findViewById(R.id.txtGioCaoDiem);
        bieuDoGio = findViewById(R.id.bieuDoGio);
        txtSoPhuongThuc = findViewById(R.id.txtSoPhuongThuc);
        thanhTiLe = findViewById(R.id.thanhTiLe);
        layoutPhuongThuc = findViewById(R.id.layoutPhuongThuc);
        layoutTopMon = findViewById(R.id.layoutTopMon);
        txtDoiSoat = findViewById(R.id.txtDoiSoat);
        txtTienMatKet = findViewById(R.id.txtTienMatKet);
        thanhTiLe.setClipToOutline(true);

        rgKhoangThoiGian.setOnCheckedChangeListener((group, checkedId) -> taiBaoCao());
        findViewById(R.id.btnKetCa).setOnClickListener(v -> ketCa());
        findViewById(R.id.imgThongBao).setOnClickListener(v ->
                Toast.makeText(this, "Không có thông báo mới", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiBaoCao();
    }

    private long dauNgay(long thoiGian) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(thoiGian);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void taiBaoCao() {
        long homNay = dauNgay(System.currentTimeMillis());
        long den = homNay + MOT_NGAY;
        long tu;
        int soNgay;
        String tenKyTruoc;
        int id = rgKhoangThoiGian.getCheckedRadioButtonId();
        if (id == R.id.rad7Ngay) {
            soNgay = 7;
            tu = homNay - 6 * MOT_NGAY;
            tenKyTruoc = "7 ngày trước đó";
        } else if (id == R.id.radThangNay) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(homNay);
            c.set(Calendar.DAY_OF_MONTH, 1);
            tu = c.getTimeInMillis();
            soNgay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            tenKyTruoc = "cùng kỳ tháng trước";
        } else if (id == R.id.radTatCa) {
            tu = 0;
            soNgay = 0;
            tenKyTruoc = null;
        } else {
            soNgay = 1;
            tu = homNay;
            tenKyTruoc = "hôm qua";
        }

        long tuNgay = tu;
        long doDai = den - tu;
        boolean soSanh = tenKyTruoc != null;
        int soNgayChot = soNgay;
        String tenKyTruocChot = tenKyTruoc;
        ChayNen.chay(this, () -> {
            Object[] kq = new Object[3];
            kq[0] = db.layHoaDonDaThanhToan(tuNgay, den);
            kq[1] = soSanh ? db.layHoaDonDaThanhToan(tuNgay - doDai, tuNgay) : new ArrayList<HoaDon>();
            kq[2] = db.layMonBanChay(tuNgay, den, 5);
            return kq;
        }, kq -> {
            @SuppressWarnings("unchecked") ArrayList<HoaDon> dsHoaDon = (ArrayList<HoaDon>) kq[0];
            @SuppressWarnings("unchecked") ArrayList<HoaDon> dsKyTruoc = (ArrayList<HoaDon>) kq[1];
            @SuppressWarnings("unchecked") ArrayList<Object[]> topMon = (ArrayList<Object[]>) kq[2];
            hienThiBaoCao(tuNgay, den, soNgayChot, tenKyTruocChot, dsHoaDon, dsKyTruoc, topMon);
        });
    }

    private void hienThiBaoCao(long tu, long den, int soNgay, String tenKyTruoc, ArrayList<HoaDon> dsHoaDon,
                               ArrayList<HoaDon> dsKyTruoc, ArrayList<Object[]> topMon) {
        long tongThu = 0;
        int soKhach = 0, tongMon = 0;
        long[] doanhThuGio = new long[GIO_DONG_CUA - GIO_MO_CUA + 1];
        LinkedHashMap<String, long[]> theoPhuongThuc = new LinkedHashMap<>(); // {số GD, tiền}
        for (String pt : PHUONG_THUC) theoPhuongThuc.put(pt, new long[2]);

        Calendar cal = Calendar.getInstance();
        for (HoaDon hd : dsHoaDon) {
            tongThu += hd.getTongTien();
            soKhach += hd.getSoKhach();
            tongMon += hd.getSoMon();

            cal.setTimeInMillis(hd.getGioRa());
            int gio = cal.get(Calendar.HOUR_OF_DAY);
            int viTri = Math.min(Math.max(gio, GIO_MO_CUA), GIO_DONG_CUA) - GIO_MO_CUA;
            doanhThuGio[viTri] += hd.getTongTien();

            String pt = hd.getPhuongThuc() == null ? "Tiền mặt" : hd.getPhuongThuc();
            long[] gt = theoPhuongThuc.get(pt);
            if (gt == null) {
                gt = new long[2];
                theoPhuongThuc.put(pt, gt);
            }
            gt[0]++;
            gt[1] += hd.getTongTien();
        }

        // Tổng doanh thu & so sánh kỳ trước
        txtTongDoanhThu.setText(TienIch.tien(tongThu));
        if (tenKyTruoc != null) {
            long tongKyTruoc = 0;
            for (HoaDon hd : dsKyTruoc) tongKyTruoc += hd.getTongTien();
            long chenh = tongThu - tongKyTruoc;
            txtSoSanh.setText((chenh >= 0 ? "Cao hơn " : "Thấp hơn ") + tenKyTruoc + " "
                    + TienIch.tien(Math.abs(chenh)));
            txtTangTruong.setVisibility(View.VISIBLE);
            if (tongKyTruoc > 0) {
                double pt = chenh * 100.0 / tongKyTruoc;
                txtTangTruong.setText(String.format(Locale.getDefault(), "%s %+.1f%%", pt >= 0 ? "↗" : "↘", pt));
            } else {
                txtTangTruong.setText("Mới");
            }

            long mucTieu = MUC_TIEU_MOT_NGAY * soNgay;
            int phanTram = (int) Math.min(100, tongThu * 100 / mucTieu);
            pbMucTieu.setVisibility(View.VISIBLE);
            pbMucTieu.setProgress(phanTram);
            txtMucTieu.setText("Mục tiêu: " + TienIch.tien(mucTieu));
            txtPhanTramMucTieu.setText(String.format(Locale.getDefault(), "%.1f%% hoàn thành",
                    tongThu * 100.0 / mucTieu));
        } else {
            txtSoSanh.setText("Toàn bộ doanh thu đã ghi nhận");
            txtTangTruong.setVisibility(View.GONE);
            pbMucTieu.setVisibility(View.GONE);
            txtMucTieu.setText("");
            txtPhanTramMucTieu.setText("");
        }

        // Số hóa đơn, trung bình
        int soDon = dsHoaDon.size();
        txtSoHoaDon.setText(soDon + " đơn");
        txtSoKhachPhucVu.setText("Phục vụ " + soKhach + " khách");
        txtTrungBinhDon.setText(TienIch.tien(soDon == 0 ? 0 : tongThu / soDon));
        txtMonTrenDon.setText(String.format(Locale.getDefault(), "~ %.1f món / đơn",
                soDon == 0 ? 0 : tongMon / (double) soDon));

        // Món bán chạy
        if (topMon.isEmpty()) {
            txtMonBanChay.setText("Chưa có dữ liệu");
            txtChiTietMonBanChay.setText("Hãy thanh toán hóa đơn đầu tiên");
            txtDoanhThuMonBanChay.setText("");
        } else {
            Object[] dau = topMon.get(0);
            int sl = (int) dau[1];
            txtMonBanChay.setText((String) dau[0]);
            txtChiTietMonBanChay.setText(String.format(Locale.getDefault(),
                    "Đã phục vụ: %d phần (%.0f%% tổng món)", sl, tongMon == 0 ? 0 : sl * 100.0 / tongMon));
            txtDoanhThuMonBanChay.setText(TienIch.tien((long) dau[2]));
        }
        hienThiTopMon(topMon);

        // Biểu đồ khung giờ
        String[] nhanGio = new String[doanhThuGio.length];
        int gioDinh = -1;
        long dinh = 0;
        for (int i = 0; i < doanhThuGio.length; i++) {
            nhanGio[i] = (i % 3 == 0) ? String.format(Locale.getDefault(), "%02dh", GIO_MO_CUA + i) : "";
            if (doanhThuGio[i] > dinh) {
                dinh = doanhThuGio[i];
                gioDinh = GIO_MO_CUA + i;
            }
        }
        bieuDoGio.setDuLieu(doanhThuGio, nhanGio);
        txtGioCaoDiem.setText(gioDinh < 0 ? "Chưa có dữ liệu"
                : String.format(Locale.getDefault(), "Đỉnh: %02d:00 - %02d:00", gioDinh, gioDinh + 1));

        hienThiPhuongThuc(theoPhuongThuc, tongThu);

        // Đối soát két
        long tienMatThu = theoPhuongThuc.get("Tiền mặt")[1];
        tienMatTrongKet = TIEN_DAU_CA + tienMatThu;
        txtDoiSoat.setText("• Tiền mặt đầu ca: " + TienIch.tien(TIEN_DAU_CA)
                + "\n• (+) Tiền mặt thu trong kỳ: " + TienIch.tien(tienMatThu)
                + "\n• Chuyển khoản / thẻ / ví: " + TienIch.tien(tongThu - tienMatThu));
        txtTienMatKet.setText(TienIch.tien(tienMatTrongKet));
    }

    private void hienThiPhuongThuc(LinkedHashMap<String, long[]> theoPhuongThuc, long tongThu) {
        thanhTiLe.removeAllViews();
        layoutPhuongThuc.removeAllViews();
        int soHinhThuc = 0, chiSo = 0;
        for (Map.Entry<String, long[]> e : theoPhuongThuc.entrySet()) {
            int mau = MAU_PHUONG_THUC[Math.min(chiSo, MAU_PHUONG_THUC.length - 1)];
            chiSo++;
            long soGD = e.getValue()[0];
            long tien = e.getValue()[1];
            if (soGD > 0) soHinhThuc++;

            if (tien > 0) {
                View doan = new View(this);
                doan.setBackgroundColor(mau);
                thanhTiLe.addView(doan, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.MATCH_PARENT, tien));
            }

            View dong = getLayoutInflater().inflate(R.layout.item_bao_cao_dong, layoutPhuongThuc, false);
            TextView cham = dong.findViewById(R.id.txtChamMau);
            cham.setBackgroundTintList(ColorStateList.valueOf(mau));
            cham.setText("");
            ((TextView) dong.findViewById(R.id.txtTenDongBC)).setText(e.getKey());
            ((TextView) dong.findViewById(R.id.txtPhuDongBC)).setText(soGD + " giao dịch");
            ((TextView) dong.findViewById(R.id.txtGiaTriDongBC)).setText(TienIch.tien(tien));
            ((TextView) dong.findViewById(R.id.txtPhanTramDongBC)).setText(String.format(Locale.getDefault(),
                    "%.1f%%", tongThu == 0 ? 0 : tien * 100.0 / tongThu));
            layoutPhuongThuc.addView(dong);
        }
        txtSoPhuongThuc.setText(soHinhThuc + " hình thức");
    }

    private void hienThiTopMon(ArrayList<Object[]> topMon) {
        layoutTopMon.removeAllViews();
        long tongDoanhThuTop = 0;
        for (Object[] m : topMon) tongDoanhThuTop += (long) m[2];
        if (topMon.isEmpty()) {
            TextView txt = new TextView(this);
            txt.setText("Chưa có món nào được bán trong khoảng thời gian này.");
            txt.setTextColor(getColor(R.color.text_hint));
            layoutTopMon.addView(txt);
            return;
        }
        for (int i = 0; i < topMon.size(); i++) {
            Object[] m = topMon.get(i);
            View dong = getLayoutInflater().inflate(R.layout.item_bao_cao_dong, layoutTopMon, false);
            TextView hang = dong.findViewById(R.id.txtChamMau);
            hang.setText(String.valueOf(i + 1));
            hang.setBackgroundTintList(ColorStateList.valueOf(getColor(i == 0 ? R.color.caramel : R.color.espresso)));
            ((TextView) dong.findViewById(R.id.txtTenDongBC)).setText((String) m[0]);
            ((TextView) dong.findViewById(R.id.txtPhuDongBC)).setText(m[1] + " phần");
            ((TextView) dong.findViewById(R.id.txtGiaTriDongBC)).setText(TienIch.tien((long) m[2]));
            ((TextView) dong.findViewById(R.id.txtPhanTramDongBC)).setText(String.format(Locale.getDefault(),
                    "%.1f%% top 5", tongDoanhThuTop == 0 ? 0 : (long) m[2] * 100.0 / tongDoanhThuTop));
            layoutTopMon.addView(dong);
        }
    }

    private void ketCa() {
        new AlertDialog.Builder(this)
                .setTitle("Bàn giao ca")
                .setMessage("Doanh thu: " + txtTongDoanhThu.getText()
                        + "\nSố hóa đơn: " + txtSoHoaDon.getText()
                        + "\nTiền mặt trong két: " + TienIch.tien(tienMatTrongKet)
                        + "\n\nIn biên bản kết ca và bàn giao cho ca tiếp theo?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("In biên bản", (d, w) ->
                        Toast.makeText(this, "Đã in biên bản kết ca", Toast.LENGTH_SHORT).show())
                .show();
    }
}
