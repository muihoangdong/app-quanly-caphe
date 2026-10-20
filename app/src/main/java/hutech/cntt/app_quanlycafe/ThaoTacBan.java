package hutech.cntt.app_quanlycafe;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.model.Ban;

/**
 * Thao tác chọn bàn để gọi món, dùng chung cho Trang chủ và Sơ đồ bàn.
 */
public class ThaoTacBan {

    /** Bàn đã có khách: thêm món; bàn trống / đã đặt: hỏi số khách rồi mở bàn. */
    public static void goiMon(Activity activity, Ban ban) {
        if (!ban.isTrong()) {
            moThucDon(activity, ban.getHoaDonId(), false);
        } else if (ban.getTrangThai() == Ban.BAO_TRI) {
            Toast.makeText(activity, ban.getTenBan() + " đang bảo trì, hãy đổi trạng thái bàn trước",
                    Toast.LENGTH_LONG).show();
        } else {
            hienThiMoBan(activity, ban);
        }
    }

    public static void hienThiMoBan(Activity activity, Ban ban) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_mo_ban, null);
        TextView txtSoKhach = view.findViewById(R.id.txtSoKhach);
        TextView txtGoiY = view.findViewById(R.id.txtGoiYSoCho);
        txtGoiY.setText("Tối đa " + ban.getSoCho() + " khách"
                + (ban.getTrangThai() == Ban.DA_DAT ? "\nBàn đang được đặt trước" : ""));
        final int[] soKhach = {Math.min(2, Math.max(1, ban.getSoCho()))};
        txtSoKhach.setText(String.valueOf(soKhach[0]));

        view.findViewById(R.id.btnGiamKhach).setOnClickListener(v -> {
            if (soKhach[0] > 1) soKhach[0]--;
            txtSoKhach.setText(String.valueOf(soKhach[0]));
        });
        view.findViewById(R.id.btnTangKhach).setOnClickListener(v -> {
            if (soKhach[0] < 99) soKhach[0]++;
            txtSoKhach.setText(String.valueOf(soKhach[0]));
        });

        new AlertDialog.Builder(activity)
                .setTitle("Mở " + ban.getTenBan() + " • " + ban.getKhuVuc())
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Mở bàn & gọi món", (d, w) -> ChayNen.chay(activity,
                        () -> CafeDatabase.getInstance(activity).moBan(ban.getId(), soKhach[0]),
                        hoaDonId -> {
                            Toast.makeText(activity, "Đã mở " + ban.getTenBan(), Toast.LENGTH_SHORT).show();
                            moThucDon(activity, hoaDonId, true);
                        }))
                .show();
    }

    /** Mở thực đơn ở chế độ gọi món cho đơn hoaDonId. */
    public static void moThucDon(Activity activity, int hoaDonId, boolean moGoiMonSauKhiGui) {
        Intent in = new Intent(activity, GiaoDienThucDonActivity.class);
        in.putExtra("hoaDonId", hoaDonId);
        in.putExtra("moGoiMonSauKhiGui", moGoiMonSauKhiGui);
        activity.startActivity(in);
    }

    public static void moChiTietOrder(Activity activity, int hoaDonId) {
        Intent in = new Intent(activity, GiaoDienGoiMonActivity.class);
        in.putExtra("hoaDonId", hoaDonId);
        activity.startActivity(in);
    }

    /** Hộp thoại chọn bàn để gọi món (lối tắt ở Trang chủ). */
    public static void chonBanDeGoiMon(Activity activity) {
        ChayNen.chay(activity, () -> CafeDatabase.getInstance(activity).layDanhSachBan(null), dsBan -> {
            ArrayList<Ban> chon = new ArrayList<>();
            ArrayList<String> ten = new ArrayList<>();
            for (Ban b : dsBan) {
                if (b.isTrong() && b.getTrangThai() == Ban.BAO_TRI) continue;
                chon.add(b);
                ten.add(b.getTenBan() + " • " + b.getKhuVuc() + "  —  "
                        + (b.isTrong() ? Ban.tenTrangThai(b.getTrangThai())
                        : b.getSoKhach() + " khách, " + b.getSoMon() + " món"));
            }
            if (chon.isEmpty()) {
                Toast.makeText(activity, "Chưa có bàn nào. Hãy thêm bàn ở mục Bàn.", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(activity)
                    .setTitle("Chọn bàn để gọi món")
                    .setItems(ten.toArray(new String[0]), (d, i) -> goiMon(activity, chon.get(i)))
                    .setNegativeButton("Đóng", null)
                    .show();
        });
    }
}
