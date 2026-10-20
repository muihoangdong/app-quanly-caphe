package hutech.cntt.app_quanlycafe.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.R;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;

public class GoiMonAdapter extends BaseAdapter {

    public interface OnMonListener {
        void onDoiTrangThai(ChiTietHoaDon ct);

        void onDoiSoLuong(ChiTietHoaDon ct, int soLuongMoi);
    }

    private final Context context;
    private final ArrayList<ChiTietHoaDon> listMon;
    private final OnMonListener listener;

    public GoiMonAdapter(Context context, ArrayList<ChiTietHoaDon> listMon, OnMonListener listener) {
        this.context = context;
        this.listMon = listMon;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return listMon.size();
    }

    @Override
    public Object getItem(int position) {
        return listMon.get(position);
    }

    @Override
    public long getItemId(int position) {
        return listMon.get(position).getId();
    }

    static class ViewHolder {
        View vachDangPha;
        ImageView imgMon;
        TextView txtTen;
        TextView txtThanhTien;
        TextView txtGhiChu;
        TextView txtTrangThai;
        TextView txtSoLuong;
        ImageButton btnGiam;
        ImageButton btnTang;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_goi_mon, parent, false);
            convertView.setClipToOutline(true);
            holder = new ViewHolder();
            holder.vachDangPha = convertView.findViewById(R.id.vachDangPha);
            holder.imgMon = convertView.findViewById(R.id.imgMonOrder);
            holder.txtTen = convertView.findViewById(R.id.txtTenMonOrder);
            holder.txtThanhTien = convertView.findViewById(R.id.txtThanhTienOrder);
            holder.txtGhiChu = convertView.findViewById(R.id.txtGhiChuOrder);
            holder.txtTrangThai = convertView.findViewById(R.id.txtTrangThaiMon);
            holder.txtSoLuong = convertView.findViewById(R.id.txtSoLuongMon);
            holder.btnGiam = convertView.findViewById(R.id.btnGiamMon);
            holder.btnTang = convertView.findViewById(R.id.btnTangMon);
            holder.imgMon.setClipToOutline(true);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ChiTietHoaDon ct = listMon.get(position);
        TienIch.hienThiAnhMon(holder.imgMon, ct.getUriAnh(), ct.getDanhMuc());
        holder.txtTen.setText(String.format(Locale.getDefault(), "%02d • %s", ct.getSoLuong(), ct.getTenMon()));
        holder.txtThanhTien.setText(TienIch.tien(ct.getThanhTien()));

        String ghiChu = TienIch.tien(ct.getDonGia()) + "/phần";
        if (ct.getGhiChu() != null && !ct.getGhiChu().isEmpty()) ghiChu += " • " + ct.getGhiChu();
        ghiChu += " • " + TienIch.gio(ct.getThoiGian());
        holder.txtGhiChu.setText(ghiChu);
        holder.txtSoLuong.setText(String.valueOf(ct.getSoLuong()));

        if (ct.getTrangThai() == ChiTietHoaDon.DA_LEN_MON) {
            holder.vachDangPha.setVisibility(View.INVISIBLE);
            holder.txtTrangThai.setText("✓ Đã lên món");
            holder.txtTrangThai.setBackgroundResource(R.drawable.bg_pill_green);
            holder.txtTrangThai.setTextColor(ContextCompat.getColor(context, R.color.status_green));
        } else {
            holder.vachDangPha.setVisibility(View.VISIBLE);
            holder.txtTrangThai.setText("⏳ Đang pha chế (" + TienIch.thoiGianNgoi(ct.getThoiGian()) + ")");
            holder.txtTrangThai.setBackgroundResource(R.drawable.bg_pill_amber);
            holder.txtTrangThai.setTextColor(ContextCompat.getColor(context, R.color.caramel_dark));
        }

        holder.txtTrangThai.setOnClickListener(v -> listener.onDoiTrangThai(ct));
        holder.btnTang.setOnClickListener(v -> listener.onDoiSoLuong(ct, ct.getSoLuong() + 1));
        holder.btnGiam.setOnClickListener(v -> listener.onDoiSoLuong(ct, ct.getSoLuong() - 1));
        return convertView;
    }
}
