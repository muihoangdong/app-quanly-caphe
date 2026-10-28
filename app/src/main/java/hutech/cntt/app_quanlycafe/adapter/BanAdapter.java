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
import hutech.cntt.app_quanlycafe.model.Ban;

public class BanAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<Ban> listBan;

    public BanAdapter(Context context, ArrayList<Ban> listBan) {
        this.context = context;
        this.listBan = listBan;
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
        View layoutBan;
        ImageView imgBan;
        View viewCham;
        TextView txtTenBan;
        TextView txtTrangThai;
        TextView txtTamTinh;
        TextView txtKhuVuc;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_ban, parent, false);
            holder = new ViewHolder();
            holder.layoutBan = convertView.findViewById(R.id.layoutBan);
            holder.imgBan = convertView.findViewById(R.id.imgBan);
            holder.viewCham = convertView.findViewById(R.id.viewChamBan);
            holder.txtTenBan = convertView.findViewById(R.id.txtTenBan);
            holder.txtTrangThai = convertView.findViewById(R.id.txtTrangThaiBan);
            holder.txtTamTinh = convertView.findViewById(R.id.txtTamTinhBan);
            holder.txtKhuVuc = convertView.findViewById(R.id.txtKhuVucBan);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Ban ban = listBan.get(position);
        int trangThai = ban.getTrangThaiHienThi();
        holder.txtTenBan.setText(ban.getTenBan());
        holder.txtKhuVuc.setText(ban.getKhuVuc());
        holder.txtTrangThai.setText(Ban.tenTrangThai(trangThai));

        int nen, mau;
        if (trangThai == Ban.TRONG) {
            nen = R.drawable.bg_ban_trong;
            mau = R.color.mint_accent;
        } else if (trangThai == Ban.DA_DAT) {
            nen = R.drawable.bg_ban_dat_truoc;
            mau = R.color.tim;
        } else {
            nen = R.drawable.bg_ban_phuc_vu;
            mau = R.color.cam;
        }
        int mauChu = ContextCompat.getColor(context, mau);
        holder.layoutBan.setBackgroundResource(nen);
        holder.viewCham.setBackgroundTintList(ColorStateList.valueOf(mauChu));
        holder.imgBan.setImageTintList(ColorStateList.valueOf(trangThai == Ban.TRONG
                ? ContextCompat.getColor(context, R.color.forest_green) : mauChu));
        holder.txtTrangThai.setTextColor(trangThai == Ban.TRONG
                ? ContextCompat.getColor(context, R.color.text_secondary) : mauChu);

        if (ban.isTrong()) {
            holder.txtTamTinh.setVisibility(View.INVISIBLE);
        } else {
            holder.txtTamTinh.setVisibility(View.VISIBLE);
            holder.txtTamTinh.setText(TienIch.tien(ban.getTamTinh()));
        }
        return convertView;
    }
}
