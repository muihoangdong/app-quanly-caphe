package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import hutech.cntt.app_quanlycafe.adapter.DongAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.GiaoDienKhach;
import hutech.cntt.app_quanlycafe.data.GioHangKhach;
import hutech.cntt.app_quanlycafe.data.PhienDangNhap;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ThucUong;

/**
 * Khách: danh sách món theo danh mục, thêm vào giỏ (tham khảo trang product-listing).
 * Extra "danhMuc" để mở sẵn một danh mục.
 */
public class KhachThucDonActivity extends AppCompatActivity {

    ListView lvSanPham;
    RadioGroup rgDanhMuc;
    EditText edtTimMon;
    TextView txtKhongCo, txtSoMonGio;
    View layoutThanhGio;

    CafeDatabase db;
    ArrayList<ThucUong> listMon;
    DongAdapter<ThucUong> adapter;
    String danhMucDangChon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PhienDangNhap.kiemTraQuyen(this, true, false, false)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_khach_thuc_don);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuongKhach(this, R.id.nav_k_thuc_don);

        db = CafeDatabase.getInstance(this);
        lvSanPham = findViewById(R.id.lvSanPham);
        rgDanhMuc = findViewById(R.id.rgDanhMucKhach);
        edtTimMon = findViewById(R.id.edtTimMon);
        txtKhongCo = findViewById(R.id.txtKhongCoSanPham);
        txtSoMonGio = findViewById(R.id.txtSoMonGio);
        layoutThanhGio = findViewById(R.id.layoutThanhGio);

        danhMucDangChon = getIntent().getStringExtra("danhMuc");

        listMon = new ArrayList<>();
        adapter = new DongAdapter<>(this, listMon, R.layout.item_k_san_pham, this::ganDongMon);
        lvSanPham.setAdapter(adapter);
        lvSanPham.setOnItemClickListener((p, v, position, id) ->
                GiaoDienKhach.hienThiChonMon(this, listMon.get(position), this::capNhatGio));

        edtTimMon.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                taiMon();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        View.OnClickListener moGio = v -> startActivity(new Intent(this, KhachGioHangActivity.class));
        layoutThanhGio.setOnClickListener(moGio);
        findViewById(R.id.imgGioHangTop).setOnClickListener(moGio);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String dm = intent.getStringExtra("danhMuc");
        if (dm != null) danhMucDangChon = dm;
    }

    @Override
    protected void onResume() {
        super.onResume();
        taoChipDanhMuc();
        taiMon();
    }

    private void ganDongMon(View dong, ThucUong tu) {
        TienIch.hienThiAnhMon(dong.findViewById(R.id.imgSanPham), tu.getUriAnh(), tu.getDanhMuc());
        ((TextView) dong.findViewById(R.id.txtTenSanPham)).setText(
                tu.isBanChay() ? "★ " + tu.getTenMon() : tu.getTenMon());
        ((TextView) dong.findViewById(R.id.txtMoTaSanPham)).setText(tu.getMoTa());
        ((TextView) dong.findViewById(R.id.txtGiaSanPham)).setText(TienIch.tien(tu.getGia()));
        dong.findViewById(R.id.txtHetSanPham).setVisibility(tu.isConBan() ? View.GONE : View.VISIBLE);
        View btnThem = dong.findViewById(R.id.btnThemSanPham);
        btnThem.setAlpha(tu.isConBan() ? 1f : 0.3f);
        btnThem.setOnClickListener(v -> GiaoDienKhach.hienThiChonMon(this, tu, this::capNhatGio));
        TextView txtSoLuong = dong.findViewById(R.id.txtSoLuongSanPham);
        int sl = GioHangKhach.soLuongCuaMon(tu.getId());
        txtSoLuong.setVisibility(sl > 0 ? View.VISIBLE : View.GONE);
        txtSoLuong.setText(String.valueOf(sl));
    }

    private void taoChipDanhMuc() {
        rgDanhMuc.setOnCheckedChangeListener(null);
        rgDanhMuc.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        ArrayList<String> dsDanhMuc = db.layDanhSachDanhMuc();
        if (danhMucDangChon != null && !dsDanhMuc.contains(danhMucDangChon)) danhMucDangChon = null;

        RadioButton chipTatCa = (RadioButton) inflater.inflate(R.layout.item_k_chip, rgDanhMuc, false);
        chipTatCa.setId(View.generateViewId());
        chipTatCa.setText("Tất cả");
        rgDanhMuc.addView(chipTatCa);
        if (danhMucDangChon == null) chipTatCa.setChecked(true);

        for (String dm : dsDanhMuc) {
            RadioButton chip = (RadioButton) inflater.inflate(R.layout.item_k_chip, rgDanhMuc, false);
            chip.setId(View.generateViewId());
            chip.setText(dm);
            chip.setTag(dm);
            rgDanhMuc.addView(chip);
            if (dm.equals(danhMucDangChon)) chip.setChecked(true);
        }

        rgDanhMuc.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton chip = group.findViewById(checkedId);
            if (chip == null) return;
            danhMucDangChon = (String) chip.getTag();
            taiMon();
        });
    }

    private void taiMon() {
        listMon.clear();
        // Khách thấy cả món tạm hết (làm mờ) để biết thực đơn đầy đủ
        listMon.addAll(db.layDanhSachThucUong(danhMucDangChon, edtTimMon.getText().toString(), false));
        adapter.notifyDataSetChanged();
        txtKhongCo.setVisibility(listMon.isEmpty() ? View.VISIBLE : View.GONE);
        capNhatGio();
    }

    private void capNhatGio() {
        adapter.notifyDataSetChanged();
        if (GioHangKhach.rong()) {
            layoutThanhGio.setVisibility(View.GONE);
        } else {
            layoutThanhGio.setVisibility(View.VISIBLE);
            txtSoMonGio.setText(GioHangKhach.tongSoLuong() + " món • " + TienIch.tien(GioHangKhach.tongTien()));
        }
    }
}
