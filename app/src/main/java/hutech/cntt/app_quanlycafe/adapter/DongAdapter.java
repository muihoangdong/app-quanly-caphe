package hutech.cntt.app_quanlycafe.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;


public class DongAdapter<T> extends BaseAdapter {

    public interface GanDuLieu<T> {
        void gan(View dong, T item);
    }

    private final Context context;
    private final ArrayList<T> list;
    private final int layoutDong;
    private final GanDuLieu<T> ganDuLieu;

    public DongAdapter(Context context, ArrayList<T> list, int layoutDong, GanDuLieu<T> ganDuLieu) {
        this.context = context;
        this.list = list;
        this.layoutDong = layoutDong;
        this.ganDuLieu = ganDuLieu;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(layoutDong, parent, false);
        }
        ganDuLieu.gan(convertView, list.get(position));
        return convertView;
    }
}
