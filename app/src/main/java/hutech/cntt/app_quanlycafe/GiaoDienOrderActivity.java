package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.adapter.HoaDonAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.CafeDatabase.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.model.HoaDon;

public class GiaoDienOrderActivity extends AppCompatActivity {

    ListView lvOrder;
    TextView txtKhongCoOrder;

    CafeDatabase db;
    ArrayList<HoaDon> listOrder;
    HoaDonAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_order);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_order);
        DieuHuong.ganThanhTieuDe(this, "Đơn hàng", true);
        DieuHuong.ganNutTieuDe(this, 1, R.drawable.ic_receipt, "Lịch sử hóa đơn", v ->
                startActivity(new Intent(this, GiaoDienLichSuHoaDonActivity.class)));

        db = CafeDatabase.getInstance(this);
        lvOrder = findViewById(R.id.lvOrder);
        txtKhongCoOrder = findViewById(R.id.txtKhongCoOrder);

        listOrder = new ArrayList<>();
        adapter = new HoaDonAdapter(this, listOrder);
        lvOrder.setAdapter(adapter);

        lvOrder.setOnItemClickListener((parent, view, position, id) ->
                ThaoTacBan.moThanhToan(this, listOrder.get(position).getId()));
        lvOrder.setOnItemLongClickListener((parent, view, position, id) -> {
            HoaDon hd = listOrder.get(position);
            new AlertDialog.Builder(this)
                    .setTitle(hd.getTenBan() + " • #" + hd.getMaHoaDon())
                    .setItems(new String[]{"Thanh toán", "Gọi thêm món", "Sửa món trong đơn"}, (d, i) -> {
                        if (i == 0) ThaoTacBan.moThanhToan(this, hd.getId());
                        else if (i == 1) ThaoTacBan.moThucDon(this, hd.getId());
                        else ThaoTacBan.moChiTietOrder(this, hd.getId());
                    })
                    .show();
            return true;
        });

        findViewById(R.id.btnTaoOrder).setOnClickListener(v -> ThaoTacBan.moGoiMon(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    private void taiDuLieu() {
        ChayNen.chay(this, () -> db.layDonDangPhucVu(), ds -> {
            listOrder.clear();
            listOrder.addAll(ds);
            adapter.notifyDataSetChanged();
            txtKhongCoOrder.setVisibility(listOrder.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
