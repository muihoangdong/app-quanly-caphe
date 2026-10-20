package hutech.cntt.app_quanlycafe.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.R;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.Ban;

public class BanAdapter extends BaseAdapter {

    public interface OnHanhDongBanListener {
        void onHanhDong(Ban ban);
    }

    private final Context context;
    private final ArrayList<Ban> listBan;
    private final OnHanhDongBanListener listener;

    public BanAdapter(Context context, ArrayList<Ban> listBan, OnHanhDongBanListener listener) {
        this.context = context;
        this.listBan = listBan;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return listBan.size();
    }

    @Override
    public Object getItem(int position) {
        return listBan.get(position);
    }

    @Override
    public long getItemId(int position) {
        return listBan.get(position).getId();
    }

    static class ViewHolder {
        LinearLayout layoutBan;
        TextView txtTenBan;
        TextView txtTrangThai;
        TextView txtKhuVuc;
        TextView txtSoKhach;
        ImageView imgSoMon;
        TextView txtSoMon;
        LinearLayout layoutTamTinh;
        TextView txtTamTinh;
        MaterialButton btnHanhDong;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_ban, parent, false);
            holder = new ViewHolder();
            holder.layoutBan = convertView.findViewById(R.id.layoutBan);
            holder.txtTenBan = convertView.findViewById(R.id.txtTenBan);
            holder.txtTrangThai = convertView.findViewById(R.id.txtTrangThaiBan);
            holder.txtKhuVuc = convertView.findViewById(R.id.txtKhuVucBan);
            holder.txtSoKhach = convertView.findViewById(R.id.txtSoKhachBan);
            holder.imgSoMon = convertView.findViewById(R.id.imgSoMon);
            holder.txtSoMon = convertView.findViewById(R.id.txtSoMonBan);
            holder.layoutTamTinh = convertView.findViewById(R.id.layoutTamTinhBan);
            holder.txtTamTinh = convertView.findViewById(R.id.txtTamTinhBan);
            holder.btnHanhDong = convertView.findViewById(R.id.btnHanhDongBan);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Ban ban = listBan.get(position);
        holder.txtTenBan.setText(ban.getTenBan());
        holder.txtKhuVuc.setText(ban.getKhuVuc());

        if (ban.isTrong()) {
            holder.layoutBan.setBackgroundResource(R.drawable.bg_card_latte);
            holder.layoutBan.setAlpha(ban.getTrangThai() == Ban.BAO_TRI ? 0.55f : 1f);
            if (ban.getTrangThai() == Ban.DA_DAT) {
                datPill(holder.txtTrangThai, "Đã đặt", R.drawable.bg_pill_caramel, R.color.white);
            } else if (ban.getTrangThai() == Ban.BAO_TRI) {
                datPill(holder.txtTrangThai, "Bảo trì", R.drawable.bg_pill_red, R.color.status_red);
            } else {
                datPill(holder.txtTrangThai, "Trống", R.drawable.bg_pill_latte, R.color.text_secondary);
            }
            holder.txtSoKhach.setText("Tối đa " + ban.getSoCho() + " khách");
            holder.imgSoMon.setVisibility(View.GONE);
            holder.txtSoMon.setVisibility(View.GONE);
            holder.layoutTamTinh.setVisibility(View.GONE);
            holder.btnHanhDong.setVisibility(ban.getTrangThai() == Ban.BAO_TRI ? View.GONE : View.VISIBLE);
            holder.btnHanhDong.setText("Mở bàn");
            holder.btnHanhDong.setIconResource(R.drawable.ic_add);
            holder.btnHanhDong.setBackgroundTintList(mau(R.color.espresso_dark));
        } else {
            holder.layoutBan.setAlpha(1f);
            holder.txtSoKhach.setText(ban.getSoKhach() + "/" + ban.getSoCho());
            holder.imgSoMon.setVisibility(View.VISIBLE);
            holder.txtSoMon.setVisibility(View.VISIBLE);
            holder.txtSoMon.setText(ban.getSoMon() + " món");
            holder.txtTamTinh.setText(TienIch.tien(ban.getTamTinh()));

            if (ban.getTrangThai() == Ban.CHO_THANH_TOAN) {
                holder.layoutBan.setBackgroundResource(R.drawable.bg_card_amber);
                datPill(holder.txtTrangThai, "Chờ tính", R.drawable.bg_pill_caramel, R.color.white);
                holder.layoutTamTinh.setVisibility(View.VISIBLE);
                holder.btnHanhDong.setVisibility(View.VISIBLE);
                holder.btnHanhDong.setText("Thanh toán");
                holder.btnHanhDong.setIconResource(R.drawable.ic_receipt);
                holder.btnHanhDong.setBackgroundTintList(mau(R.color.caramel));
            } else {
                holder.layoutBan.setBackgroundResource(R.drawable.bg_card);
                datPill(holder.txtTrangThai, "● " + TienIch.thoiGianNgoi(ban.getGioVao()),
                        R.drawable.bg_pill_amber, R.color.status_amber);
                holder.layoutTamTinh.setVisibility(View.VISIBLE);
                holder.btnHanhDong.setVisibility(View.GONE);
            }
        }

        holder.btnHanhDong.setOnClickListener(v -> {
            if (listener != null) listener.onHanhDong(ban);
        });
        return convertView;
    }

    private void datPill(TextView txt, String noiDung, int nen, int mauChu) {
        txt.setText(noiDung);
        txt.setBackgroundResource(nen);
        txt.setTextColor(ContextCompat.getColor(context, mauChu));
    }

    private ColorStateList mau(int idMau) {
        return ColorStateList.valueOf(ContextCompat.getColor(context, idMau));
    }
}
