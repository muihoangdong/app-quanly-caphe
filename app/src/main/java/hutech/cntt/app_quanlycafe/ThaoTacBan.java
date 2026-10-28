package hutech.cntt.app_quanlycafe;

import android.app.Activity;
import android.content.Intent;

import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.model.Ban;

public class ThaoTacBan {

    public static void moGoiMon(Activity activity) {
        DieuHuong.chuyenManHinh(activity, GiaoDienThucDonActivity.class);
    }

    public static void goiMon(Activity activity, Ban ban) {
        if (!ban.isTrong()) {
            moThanhToan(activity, ban.getHoaDonId());
            return;
        }
        Intent in = new Intent(activity, GiaoDienThucDonActivity.class);
        in.putExtra("banId", ban.getId());
        in.putExtra("tenBan", ban.getTenBan());
        in.putExtra("soCho", ban.getSoCho());
        activity.startActivity(in);
    }

    public static void moThucDon(Activity activity, int hoaDonId) {
        Intent in = new Intent(activity, GiaoDienThucDonActivity.class);
        in.putExtra("hoaDonId", hoaDonId);
        activity.startActivity(in);
    }

    public static void moThanhToan(Activity activity, int hoaDonId) {
        Intent in = new Intent(activity, GiaoDienThanhToanActivity.class);
        in.putExtra("hoaDonId", hoaDonId);
        activity.startActivity(in);
    }

    public static void moChiTietOrder(Activity activity, int hoaDonId) {
        Intent in = new Intent(activity, GiaoDienGoiMonActivity.class);
        in.putExtra("hoaDonId", hoaDonId);
        activity.startActivity(in);
    }
}
