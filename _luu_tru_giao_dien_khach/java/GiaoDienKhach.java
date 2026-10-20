package hutech.cntt.app_quanlycafe.data;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.R;
import hutech.cntt.app_quanlycafe.model.DatBan;
import hutech.cntt.app_quanlycafe.model.DonOnline;
import hutech.cntt.app_quanlycafe.model.ThucUong;

/**
 * Các hộp thoại dùng chung giữa màn hình khách và màn hình quản trị:
 * chọn món, vé đặt bàn (đưa cho nhân viên xem), chi tiết đơn app.
 */
public class GiaoDienKhach {

    /** Bottom sheet chọn số lượng + ghi chú rồi thêm vào giỏ khách. */
    public static void hienThiChonMon(Activity activity, ThucUong tu, Runnable sauKhiThem) {
        if (!tu.isConBan()) {
            Toast.makeText(activity, tu.getTenMon() + " đang tạm hết", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog sheet = new BottomSheetDialog(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_k_chon_mon, null);
        TienIch.hienThiAnhMon(view.findViewById(R.id.imgChonMon), tu.getUriAnh(), tu.getDanhMuc());
        ((TextView) view.findViewById(R.id.txtTenChonMon)).setText(tu.getTenMon());
        ((TextView) view.findViewById(R.id.txtGiaChonMon)).setText(TienIch.tien(tu.getGia()));
        ((TextView) view.findViewById(R.id.txtMoTaChonMon)).setText(tu.getMoTa());
        EditText edtGhiChu = view.findViewById(R.id.edtGhiChuChonMon);
        TextView txtSoLuong = view.findViewById(R.id.txtSoLuongChonMon);
        MaterialButton btnXacNhan = view.findViewById(R.id.btnXacNhanChonMon);

        final int[] soLuong = {1};
        Runnable capNhat = () -> {
            txtSoLuong.setText(String.valueOf(soLuong[0]));
            btnXacNhan.setText("Thêm • " + TienIch.tien(tu.getGia() * soLuong[0]));
        };
        capNhat.run();
        view.findViewById(R.id.btnGiamChonMon).setOnClickListener(v -> {
            if (soLuong[0] > 1) soLuong[0]--;
            capNhat.run();
        });
        view.findViewById(R.id.btnTangChonMon).setOnClickListener(v -> {
            if (soLuong[0] < 50) soLuong[0]++;
            capNhat.run();
        });
        btnXacNhan.setOnClickListener(v -> {
            GioHangKhach.them(tu, soLuong[0], edtGhiChu.getText().toString());
            Toast.makeText(activity, "Đã thêm " + soLuong[0] + " " + tu.getTenMon(), Toast.LENGTH_SHORT).show();
            sheet.dismiss();
            if (sauKhiThem != null) sauKhiThem.run();
        });
        sheet.setContentView(view);
        sheet.show();
    }

    /** Đổ thông tin lượt đặt vào layout vé (dialog_ve_dat_ban). */
    public static View taoVeDatBan(Activity activity, DatBan dat) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_ve_dat_ban, null);
        ((TextView) view.findViewById(R.id.txtMaVe)).setText(dat.getMaDat());
        ((TextView) view.findViewById(R.id.txtBanVe)).setText(dat.getTenBan());
        ((TextView) view.findViewById(R.id.txtKhuVucVe)).setText(dat.getKhuVuc());
        ((TextView) view.findViewById(R.id.txtGioVe)).setText(TienIch.gio(dat.getThoiGian()));
        ((TextView) view.findViewById(R.id.txtNgayVe)).setText(TienIch.ngay(dat.getThoiGian()));
        ((TextView) view.findViewById(R.id.txtSoKhachVe)).setText(dat.getSoKhach() + " người");
        ((TextView) view.findViewById(R.id.txtKhachVe)).setText(dat.getTenKhach() + " • " + dat.getSoDienThoai());
        TextView txtGhiChu = view.findViewById(R.id.txtGhiChuVe);
        if (dat.getGhiChu() == null || dat.getGhiChu().isEmpty()) {
            txtGhiChu.setVisibility(View.GONE);
        } else {
            txtGhiChu.setText("Ghi chú: " + dat.getGhiChu());
        }
        TextView txtTrangThai = view.findViewById(R.id.txtTrangThaiVe);
        datPillDatBan(txtTrangThai, dat);

        // Đơn gọi trước gắn với lượt đặt này
        TextView txtDonKem = view.findViewById(R.id.txtDonKemVe);
        ArrayList<DonOnline> dsDon = CafeDatabase.getInstance(activity).layDonOnlineTheoDatBan(dat.getId());
        if (dsDon.isEmpty()) {
            txtDonKem.setVisibility(View.GONE);
        } else {
            StringBuilder sb = new StringBuilder("Món gọi trước:");
            for (DonOnline don : dsDon) {
                sb.append("\n• #").append(don.getMaDon()).append(" — ").append(don.getSoMon()).append(" món, ")
                        .append(TienIch.tien(don.getTongTien()))
                        .append(don.isDaThanhToan() ? " (đã thanh toán)" : " (trả tại quầy)")
                        .append(" — ").append(don.tenTrangThai());
            }
            txtDonKem.setText(sb.toString());
        }
        return view;
    }

    public static void datPillDatBan(TextView txt, DatBan dat) {
        int nen, chu;
        switch (dat.getTrangThai()) {
            case DatBan.DA_NHAN_BAN:
                nen = R.drawable.bg_pill_green;
                chu = R.color.status_green;
                break;
            case DatBan.DA_HUY:
            case DatBan.KHONG_DEN:
                nen = R.drawable.bg_pill_red;
                chu = R.color.status_red;
                break;
            default:
                nen = R.drawable.bg_k_pill_nhat;
                chu = R.color.tch_cam_dam;
                break;
        }
        txt.setText(dat.tenTrangThai());
        txt.setBackgroundResource(nen);
        txt.setTextColor(ContextCompat.getColor(txt.getContext(), chu));
    }

    public static void datPillDon(TextView txt, DonOnline don) {
        int nen, chu;
        switch (don.getTrangThai()) {
            case DonOnline.DANG_PHA_CHE:
                nen = R.drawable.bg_pill_amber;
                chu = R.color.status_amber;
                break;
            case DonOnline.HOAN_THANH:
                nen = R.drawable.bg_pill_green;
                chu = R.color.status_green;
                break;
            case DonOnline.DA_HUY:
                nen = R.drawable.bg_pill_red;
                chu = R.color.status_red;
                break;
            default:
                nen = R.drawable.bg_k_pill_nhat;
                chu = R.color.tch_cam_dam;
                break;
        }
        txt.setText(don.tenTrangThai());
        txt.setBackgroundResource(nen);
        txt.setTextColor(ContextCompat.getColor(txt.getContext(), chu));
    }

    /** Nội dung chi tiết một đơn app (dialog_chi_tiet_hoa_don dùng lại). */
    public static View taoChiTietDon(Activity activity, DonOnline don) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_chi_tiet_hoa_don, null);
        String thongTin = "#" + don.getMaDon() + "\n"
                + "Đặt lúc " + TienIch.ngayGio(don.getThoiGian()) + "\n"
                + don.getHinhThuc()
                + (don.getThongTinBan() != null ? " • " + don.getThongTinBan() : "")
                + "\nKhách: " + don.getTenKhach() + " • " + don.getSoDienThoai()
                + "\nThanh toán: " + don.getPhuongThuc()
                + (don.isDaThanhToan() ? " (đã thanh toán)" : " (chưa thanh toán)")
                + "\nTrạng thái: " + don.tenTrangThai();
        if (don.getGhiChu() != null && !don.getGhiChu().isEmpty()) {
            thongTin += "\nGhi chú: " + don.getGhiChu();
        }
        ((TextView) view.findViewById(R.id.txtThongTinChiTiet)).setText(thongTin);
        TienIch.hienThiDongMon(view.findViewById(R.id.layoutMonChiTiet),
                CafeDatabase.getInstance(activity).layChiTietDonOnline(don.getId()));
        ((TextView) view.findViewById(R.id.txtTongKetChiTiet)).setText(don.getSoMon() + " món");
        ((TextView) view.findViewById(R.id.txtTongCongChiTiet)).setText(TienIch.tien(don.getTongTien()));
        return view;
    }

    /** Hỏi xác nhận rồi chạy hành động. */
    public static void xacNhan(Activity activity, String tieuDe, String noiDung, String nut, Runnable hanhDong) {
        new AlertDialog.Builder(activity)
                .setTitle(tieuDe)
                .setMessage(noiDung)
                .setNegativeButton("Không", null)
                .setPositiveButton(nut, (d, w) -> hanhDong.run())
                .show();
    }

    /**
     * Hộp thoại đổi mật khẩu. canMatKhauCu = false khi quản trị viên đặt lại mật khẩu cho nhân viên.
     */
    public static void hienThiDoiMatKhau(Activity activity, int taiKhoanId, boolean canMatKhauCu) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_doi_mat_khau, null);
        EditText edtCu = view.findViewById(R.id.edtMatKhauCu);
        EditText edtMoi = view.findViewById(R.id.edtMatKhauMoi);
        EditText edtNhapLai = view.findViewById(R.id.edtNhapLaiMoi);
        if (!canMatKhauCu) {
            edtCu.setVisibility(View.GONE);
            view.findViewById(R.id.txtNhanMatKhauCu).setVisibility(View.GONE);
        }
        CafeDatabase db = CafeDatabase.getInstance(activity);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(canMatKhauCu ? "Đổi mật khẩu" : "Đặt lại mật khẩu")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String moi = edtMoi.getText().toString();
            if (canMatKhauCu && !db.kiemTraMatKhau(taiKhoanId, edtCu.getText().toString())) {
                edtCu.setError("Mật khẩu hiện tại không đúng");
                edtCu.requestFocus();
                return;
            }
            if (moi.length() < 6) {
                edtMoi.setError("Mật khẩu ít nhất 6 ký tự");
                edtMoi.requestFocus();
                return;
            }
            if (!moi.equals(edtNhapLai.getText().toString())) {
                edtNhapLai.setError("Mật khẩu nhập lại không khớp");
                edtNhapLai.requestFocus();
                return;
            }
            db.datMatKhau(taiKhoanId, moi);
            Toast.makeText(activity, "Đã cập nhật mật khẩu", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    /** Chữ cái đầu của tên cuối, dùng làm ảnh đại diện. */
    public static String chuCaiDau(String hoTen) {
        if (hoTen == null || hoTen.trim().isEmpty()) return "?";
        String[] tu = hoTen.trim().split("\\s+");
        return tu[tu.length - 1].substring(0, 1).toUpperCase();
    }
}
