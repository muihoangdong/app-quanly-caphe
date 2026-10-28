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
import java.util.Map;

import hutech.cntt.app_quanlycafe.R;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ThucUong;

public class ThucUongAdapter extends BaseAdapter {

    public interface OnNutMonListener {
        void onNhanNut(ThucUong thucUong);
    }

    private final Context context;
    private final ArrayList<ThucUong> listThucUong;
    private final boolean cheDoGoiMon;
    private final Map<Integer, Integer> soLuongTrongGio; // thucUongId -> số lượng
    private final OnNutMonListener listener;
    private final OnNutMonListener listenerBot; // nút - : bớt 1 phần khỏi giỏ

    public ThucUongAdapter(Context context, ArrayList<ThucUong> listThucUong, boolean cheDoGoiMon,
                           Map<Integer, Integer> soLuongTrongGio, OnNutMonListener listener,
                           OnNutMonListener listenerBot) {
        this.context = context;
        this.listThucUong = listThucUong;
        this.cheDoGoiMon = cheDoGoiMon;
        this.soLuongTrongGio = soLuongTrongGio;
        this.listener = listener;
        this.listenerBot = listenerBot;
    }

    @Override
    public int getCount() {
        return listThucUong.size();
    }

    @Override
    public Object getItem(int position) {
        return listThucUong.get(position);
    }

    @Override
    public long getItemId(int position) {
        return listThucUong.get(position).getId();
    }

    static class ViewHolder {
        ImageView imgThucUong;
        TextView txtBanChay;
        TextView txtHetMon;
        TextView txtTen;
        TextView txtMoTa;
        TextView txtGia;
        ImageView btnThem;
        ImageView btnBot;
        TextView txtSoLuong;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_thuc_uong, parent, false);
            holder = new ViewHolder();
            holder.imgThucUong = convertView.findViewById(R.id.imgThucUong);
            holder.txtBanChay = convertView.findViewById(R.id.txtBanChay);
            holder.txtHetMon = convertView.findViewById(R.id.txtHetMon);
            holder.txtTen = convertView.findViewById(R.id.txtTenThucUong);
            holder.txtMoTa = convertView.findViewById(R.id.txtMoTaThucUong);
            holder.txtGia = convertView.findViewById(R.id.txtGiaThucUong);
            holder.btnThem = convertView.findViewById(R.id.btnThemVaoGio);
            holder.btnBot = convertView.findViewById(R.id.btnBotKhoiGio);
            holder.txtSoLuong = convertView.findViewById(R.id.txtSoLuongTrongGio);
            holder.imgThucUong.setClipToOutline(true);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ThucUong tu = listThucUong.get(position);
        TienIch.hienThiAnhMon(holder.imgThucUong, tu.getUriAnh(), tu.getTenMon(), tu.getDanhMuc());
        holder.txtTen.setText(tu.getTenMon());
        holder.txtMoTa.setText(tu.getMoTa() == null || tu.getMoTa().isEmpty() ? tu.getDanhMuc() : tu.getMoTa());
        holder.txtGia.setText(TienIch.tien(tu.getGia()));
        holder.txtBanChay.setVisibility(tu.isBanChay() ? View.VISIBLE : View.GONE);
        holder.txtHetMon.setVisibility(tu.isConBan() ? View.GONE : View.VISIBLE);

        if (cheDoGoiMon) {
            holder.btnThem.setImageResource(R.drawable.ic_add);
            holder.btnThem.setBackgroundResource(R.drawable.bg_nut_cong);
            holder.btnThem.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
            holder.btnThem.setAlpha(tu.isConBan() ? 1f : 0.35f);
            Integer sl = soLuongTrongGio.get(tu.getId());
            if (sl != null && sl > 0) {
                holder.txtSoLuong.setVisibility(View.VISIBLE);
                holder.txtSoLuong.setText(String.valueOf(sl));
                holder.btnBot.setVisibility(View.VISIBLE);
            } else {
                holder.txtSoLuong.setVisibility(View.GONE);
                holder.btnBot.setVisibility(View.GONE);
            }
        } else {
            holder.btnBot.setVisibility(View.GONE);
            holder.btnThem.setImageResource(R.drawable.ic_edit);
            holder.btnThem.setBackgroundResource(R.drawable.bg_icon_mint);
            holder.btnThem.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.forest_green)));
            holder.btnThem.setAlpha(1f);
            holder.txtSoLuong.setVisibility(View.GONE);
        }

        holder.btnThem.setOnClickListener(v -> {
            if (listener != null) listener.onNhanNut(tu);
        });
        holder.btnBot.setOnClickListener(v -> {
            if (listenerBot != null) listenerBot.onNhanNut(tu);
        });
        return convertView;
    }
}
