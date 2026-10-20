package hutech.cntt.app_quanlycafe.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.R;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.HoaDon;

public class HoaDonAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<HoaDon> listHoaDon;

    public HoaDonAdapter(Context context, ArrayList<HoaDon> listHoaDon) {
        this.context = context;
        this.listHoaDon = listHoaDon;
    }

    @Override
    public int getCount() {
        return listHoaDon.size();
    }

    @Override
    public Object getItem(int position) {
        return listHoaDon.get(position);
    }

    @Override
    public long getItemId(int position) {
        return listHoaDon.get(position).getId();
    }

    static class ViewHolder {
        ImageView imgLoai;
        TextView txtBan;
        TextView txtMa;
        TextView txtThoiGian;
        TextView txtTong;
        TextView txtTrangThai;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_hoa_don, parent, false);
            holder = new ViewHolder();
            holder.imgLoai = convertView.findViewById(R.id.imgLoaiHoaDon);
            holder.txtBan = convertView.findViewById(R.id.txtBanHoaDon);
            holder.txtMa = convertView.findViewById(R.id.txtMaHoaDonItem);
            holder.txtThoiGian = convertView.findViewById(R.id.txtThoiGianHoaDon);
            holder.txtTong = convertView.findViewById(R.id.txtTongHoaDon);
            holder.txtTrangThai = convertView.findViewById(R.id.txtTrangThaiHoaDon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HoaDon hd = listHoaDon.get(position);
        holder.txtBan.setText(hd.getTenBan() + " • " + hd.getKhuVuc());
        holder.txtMa.setText("#" + hd.getMaHoaDon());

        String thoiGian = TienIch.gio(hd.getGioVao());
        if (hd.getGioRa() > 0) thoiGian += " → " + TienIch.gio(hd.getGioRa());
        thoiGian += " • " + hd.getSoMon() + " món";
        if (hd.getTrangThai() == HoaDon.DA_THANH_TOAN && hd.getPhuongThuc() != null) {
            thoiGian += " • " + hd.getPhuongThuc();
        }
        holder.txtThoiGian.setText(thoiGian);

        switch (hd.getTrangThai()) {
            case HoaDon.DANG_PHUC_VU:
                holder.txtTong.setText(TienIch.tien(hd.getTamTinh()));
                datTrangThai(holder, "Đang phục vụ", R.drawable.bg_pill_amber, R.color.status_amber,
                        R.drawable.ic_coffee, R.color.caramel_soft, R.color.caramel_dark);
                break;
            case HoaDon.DA_THANH_TOAN:
                holder.txtTong.setText(TienIch.tien(hd.getTongTien()));
                datTrangThai(holder, "Đã thanh toán", R.drawable.bg_pill_green, R.color.status_green,
                        R.drawable.ic_check, R.color.status_green_bg, R.color.status_green);
                break;
            default:
                holder.txtTong.setText(TienIch.tien(hd.getTamTinh()));
                datTrangThai(holder, "Đã hủy", R.drawable.bg_pill_red, R.color.status_red,
                        R.drawable.ic_close, R.color.status_red_bg, R.color.status_red);
                break;
        }
        return convertView;
    }

    private void datTrangThai(ViewHolder holder, String chu, int nenPill, int mauChu,
                              int icon, int nenIcon, int mauIcon) {
        holder.txtTrangThai.setText(chu);
        holder.txtTrangThai.setBackgroundResource(nenPill);
        holder.txtTrangThai.setTextColor(ContextCompat.getColor(context, mauChu));
        holder.imgLoai.setImageResource(icon);
        holder.imgLoai.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, nenIcon)));
        holder.imgLoai.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, mauIcon)));
    }
}
