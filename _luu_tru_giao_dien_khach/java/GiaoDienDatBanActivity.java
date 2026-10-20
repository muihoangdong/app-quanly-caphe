package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

import hutech.cntt.app_quanlycafe.adapter.DongAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.GiaoDienKhach;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.DatBan;

/**
 * Nhân viên / quản trị: tra cứu lượt đặt bàn (theo mã khách đưa), nhận bàn khi khách đến,
 * hủy hoặc đánh dấu khách không đến.
 */
public class GiaoDienDatBanActivity extends AppCompatActivity {

    EditText edtTim;
    RadioGroup rgLoc;
    ListView lvDatBan;
    TextView txtTong, txtKhongCo;

    CafeDatabase db;
    final ArrayList<DatBan> listDatBan = new ArrayList<>();
    DongAdapter<DatBan> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PhienDangNhap.kiemTraQuyen(this, false, true, true)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_dat_ban);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);
        ((TextView) findViewById(R.id.txtTieuDeManHinh)).setText("Đặt bàn trước");
        findViewById(R.id.btnQuayLai).setOnClickListener(v -> finish());

        edtTim = findViewById(R.id.edtTimDatBan);
        rgLoc = findViewById(R.id.rgLocDatBan);
        lvDatBan = findViewById(R.id.lvDatBan);
        txtTong = findViewById(R.id.txtTongDatBan);
        txtKhongCo = findViewById(R.id.txtKhongCoDatBan);

        adapter = new DongAdapter<>(this, listDatBan, R.layout.item_hoa_don, this::ganDong);
        lvDatBan.setAdapter(adapter);
        lvDatBan.setOnItemClickListener((p, v, position, id) -> hienThiChiTiet(listDatBan.get(position)));
        rgLoc.setOnCheckedChangeListener((g, id) -> taiDuLieu());
        edtTim.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                taiDuLieu();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    private void taiDuLieu() {
        int id = rgLoc.getCheckedRadioButtonId();
        String tuKhoa = edtTim.getText().toString();
        ArrayList<DatBan> ds;
        if (!tuKhoa.trim().isEmpty()) {
            // Đang tra mã: tìm trên tất cả lượt đặt
            ds = db.layDanhSachDatBan(-1, 0, 0, tuKhoa);
        } else if (id == R.id.radDatHomNay) {
            Calendar dau = Calendar.getInstance();
            dau.set(Calendar.HOUR_OF_DAY, 0);
            dau.set(Calendar.MINUTE, 0);
            dau.set(Calendar.SECOND, 0);
            dau.set(Calendar.MILLISECOND, 0);
            long tu = dau.getTimeInMillis();
            ds = db.layDanhSachDatBan(-1, tu, tu + 24 * 60 * 60 * 1000L, null);
        } else if (id == R.id.radDatSapToi) {
            ds = db.layDanhSachDatBan(DatBan.CHO_DEN, 0, 0, null);
        } else if (id == R.id.radDatDaNhan) {
            ds = db.layDanhSachDatBan(DatBan.DA_NHAN_BAN, 0, 0, null);
        } else if (id == R.id.radDatDaHuy) {
            ds = db.layDanhSachDatBan(DatBan.DA_HUY, 0, 0, null);
            ds.addAll(db.layDanhSachDatBan(DatBan.KHONG_DEN, 0, 0, null));
        } else {
            ds = db.layDanhSachDatBan(-1, 0, 0, null);
        }
        listDatBan.clear();
        listDatBan.addAll(ds);
        adapter.notifyDataSetChanged();

        int soKhach = 0;
        for (DatBan d : ds) soKhach += d.getSoKhach();
        txtTong.setText(ds.size() + " lượt đặt • " + soKhach + " khách");
        txtKhongCo.setVisibility(ds.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void ganDong(View dong, DatBan dat) {
        ((TextView) dong.findViewById(R.id.txtBanHoaDon)).setText(dat.getTenBan() + " • " + dat.getKhuVuc());
        ((TextView) dong.findViewById(R.id.txtMaHoaDonItem)).setText("#" + dat.getMaDat());
        ((TextView) dong.findViewById(R.id.txtThoiGianHoaDon)).setText(dat.getTenKhach() + " • "
                + dat.getSoDienThoai() + " • " + dat.getSoKhach() + " khách");
        ((TextView) dong.findViewById(R.id.txtTongHoaDon)).setText(TienIch.gio(dat.getThoiGian()));
        GiaoDienKhach.datPillDatBan(dong.findViewById(R.id.txtTrangThaiHoaDon), dat);
        ImageView img = dong.findViewById(R.id.imgLoaiHoaDon);
        img.setImageResource(R.drawable.ic_calendar);
        img.setImageTintList(ColorStateList.valueOf(getColor(R.color.caramel_dark)));
        img.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.caramel_soft)));
        // Ngày hẹn hiện ở dòng mã cho dễ nhìn khi xem "Tất cả"
        ((TextView) dong.findViewById(R.id.txtMaHoaDonItem)).append("  •  " + TienIch.ngay(dat.getThoiGian()));
    }

    private void hienThiChiTiet(DatBan dat) {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setView(GiaoDienKhach.taoVeDatBan(this, dat))
                .setNegativeButton("Đóng", null);
        if (dat.getTrangThai() == DatBan.CHO_DEN) {
            b.setPositiveButton("Nhận bàn", (d, w) -> nhanBan(dat));
            b.setNeutralButton("Hủy / Không đến", (d, w) -> new AlertDialog.Builder(this)
                    .setTitle("#" + dat.getMaDat())
                    .setItems(new String[]{"Hủy đặt bàn (khách báo hủy)", "Khách không đến"}, (d2, i) -> {
                        db.capNhatTrangThaiDatBan(dat.getId(), i == 0 ? DatBan.DA_HUY : DatBan.KHONG_DEN);
                        Toast.makeText(this, "Đã cập nhật lượt đặt", Toast.LENGTH_SHORT).show();
                        taiDuLieu();
                    })
                    .show());
        } else if (dat.getTrangThai() == DatBan.DA_NHAN_BAN && dat.getHoaDonId() > 0) {
            b.setPositiveButton("Xem order bàn", (d, w) -> moGoiMon(dat.getHoaDonId()));
        }
        b.show();
    }

    private void nhanBan(DatBan dat) {
        long somHon = dat.getThoiGian() - System.currentTimeMillis();
        String canhBao = somHon > 60 * 60 * 1000L
                ? "\n\nLưu ý: khách đến sớm hơn giờ hẹn " + (somHon / 60000) + " phút." : "";
        GiaoDienKhach.xacNhan(this, "Nhận bàn",
                "Dẫn khách " + dat.getTenKhach() + " tới " + dat.getTenBan() + " (" + dat.getKhuVuc() + ")"
                        + " và mở bàn cho " + dat.getSoKhach() + " khách?" + canhBao,
                "Mở bàn", () -> {
                    int hoaDonId = db.nhanBan(dat);
                    if (hoaDonId == 0) {
                        Toast.makeText(this, dat.getTenBan() + " đang có khách. Hãy đổi bàn cho khách trước.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(this, "Đã mở " + dat.getTenBan(), Toast.LENGTH_SHORT).show();
                    moGoiMon(hoaDonId);
                });
    }

    private void moGoiMon(int hoaDonId) {
        Intent in = new Intent(this, GiaoDienGoiMonActivity.class);
        in.putExtra("hoaDonId", hoaDonId);
        startActivity(in);
    }
}
