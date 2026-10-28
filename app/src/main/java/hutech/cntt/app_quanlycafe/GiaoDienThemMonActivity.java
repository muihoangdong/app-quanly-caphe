package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.DanhMuc;
import hutech.cntt.app_quanlycafe.model.ThucUong;


public class GiaoDienThemMonActivity extends AppCompatActivity {

    ImageView imgAnhMon;
    EditText edtTenMon, edtMaMon, edtMoTa, edtGiaBan, edtGiaVon;
    Spinner spDanhMuc;
    TextView txtLoiNhuanGop, txtBienLoiNhuan, txtDanhGiaLoiNhuan;
    MaterialSwitch swConBan, swBanChay;

    CafeDatabase db;
    ThucUong thucUongSua;
    ArrayList<DanhMuc> arrDanhMuc = new ArrayList<>();
    ArrayAdapter<DanhMuc> adapterDanhMuc;
    String uriAnh = null;

    private final ActivityResultLauncher<String[]> chonAnhLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {

                    }
                    uriAnh = uri.toString();
                    hienThiAnh();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!DieuHuong.chiQuanLy(this)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_them_mon);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);

        imgAnhMon = findViewById(R.id.imgAnhMon);
        edtTenMon = findViewById(R.id.edtTenMon);
        edtMaMon = findViewById(R.id.edtMaMon);
        edtMoTa = findViewById(R.id.edtMoTa);
        edtGiaBan = findViewById(R.id.edtGiaBan);
        edtGiaVon = findViewById(R.id.edtGiaVon);
        spDanhMuc = findViewById(R.id.spDanhMuc);
        txtLoiNhuanGop = findViewById(R.id.txtLoiNhuanGop);
        txtBienLoiNhuan = findViewById(R.id.txtBienLoiNhuan);
        txtDanhGiaLoiNhuan = findViewById(R.id.txtDanhGiaLoiNhuan);
        swConBan = findViewById(R.id.swConBan);
        swBanChay = findViewById(R.id.swBanChay);
        imgAnhMon.setClipToOutline(true);

        // Danh mục lấy từ bảng Categories
        adapterDanhMuc = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arrDanhMuc);
        adapterDanhMuc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDanhMuc.setAdapter(adapterDanhMuc);

        int thucUongId = getIntent().getIntExtra("thucUongId", 0);
        TextView txtTieuDe = findViewById(R.id.txtTieuDeManHinh);
        txtTieuDe.setText(thucUongId > 0 ? "Sửa món" : "Thêm món mới");
        if (thucUongId == 0) findViewById(R.id.btnXoaMon).setVisibility(View.GONE);
        ChayNen.chay(this, () -> {
            Object[] kq = {db.layDanhSachDanhMuc(), thucUongId > 0 ? db.layThucUong(thucUongId) : null};
            return kq;
        }, kq -> {
            @SuppressWarnings("unchecked") ArrayList<DanhMuc> ds = (ArrayList<DanhMuc>) kq[0];
            arrDanhMuc.clear();
            arrDanhMuc.addAll(ds);
            adapterDanhMuc.notifyDataSetChanged();
            thucUongSua = (ThucUong) kq[1];
            if (thucUongSua != null) hienThiMonCanSua();
            if (arrDanhMuc.isEmpty()) {
                Toast.makeText(this, "Chưa có danh mục, hãy tạo nhóm món trước", Toast.LENGTH_LONG).show();
                taoNhomMoi();
            }
            hienThiAnh();
        }, loi -> {
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
            finish();
        });

        hienThiAnh();
        tinhLoiNhuan();

        TextWatcher theoDoiGia = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tinhLoiNhuan();
            }
        };
        edtGiaBan.addTextChangedListener(theoDoiGia);
        edtGiaVon.addTextChangedListener(theoDoiGia);

        findViewById(R.id.btnQuayLai).setOnClickListener(v -> finish());
        findViewById(R.id.btnChonAnh).setOnClickListener(v ->
                chonAnhLauncher.launch(new String[]{"image/*"}));
        imgAnhMon.setOnClickListener(v -> chonAnhLauncher.launch(new String[]{"image/*"}));
        findViewById(R.id.btnXoaAnh).setOnClickListener(v -> {
            uriAnh = null;
            hienThiAnh();
        });
        findViewById(R.id.txtTaoNhomMoi).setOnClickListener(v -> taoNhomMoi());
        findViewById(R.id.btnLuuMon).setOnClickListener(v -> luuMon());
        findViewById(R.id.btnXoaMon).setOnClickListener(v -> xoaMon());
    }

    private void hienThiMonCanSua() {
        edtTenMon.setText(thucUongSua.getTenMon());
        edtMaMon.setText(thucUongSua.getMaMon());
        edtMoTa.setText(thucUongSua.getMoTa());
        edtGiaBan.setText(String.valueOf(thucUongSua.getGia()));
        edtGiaVon.setText(String.valueOf(thucUongSua.getGiaVon()));
        swConBan.setChecked(thucUongSua.isConBan());
        swBanChay.setChecked(thucUongSua.isBanChay());
        uriAnh = thucUongSua.getUriAnh();

        for (int i = 0; i < arrDanhMuc.size(); i++) {
            if (arrDanhMuc.get(i).getId() == thucUongSua.getDanhMucId()) spDanhMuc.setSelection(i);
        }
    }

    private void hienThiAnh() {
        String danhMuc = spDanhMuc.getSelectedItem() == null ? null : spDanhMuc.getSelectedItem().toString();
        TienIch.hienThiAnhMon(imgAnhMon, uriAnh, danhMuc);
    }

    private void tinhLoiNhuan() {
        long gia = TienIch.docSo(edtGiaBan.getText().toString());
        long von = TienIch.docSo(edtGiaVon.getText().toString());
        if (gia <= 0) {
            txtLoiNhuanGop.setText("Lợi nhuận gộp: --");
            txtBienLoiNhuan.setText("Nhập giá bán và giá vốn");
            txtDanhGiaLoiNhuan.setVisibility(View.GONE);
            return;
        }
        long loi = gia - von;
        double tyLe = loi * 100.0 / gia;
        txtLoiNhuanGop.setText(String.format(Locale.getDefault(), "Lợi nhuận gộp: %.1f%%", tyLe));
        txtBienLoiNhuan.setText("Biên lợi nhuận ròng: " + TienIch.tien(loi) + " / ly phục vụ");
        txtDanhGiaLoiNhuan.setVisibility(View.VISIBLE);
        if (tyLe >= 60) {
            txtDanhGiaLoiNhuan.setText("Lãi tốt");
            txtDanhGiaLoiNhuan.setBackgroundResource(R.drawable.bg_pill_caramel);
        } else if (tyLe >= 30) {
            txtDanhGiaLoiNhuan.setText("Trung bình");
            txtDanhGiaLoiNhuan.setBackgroundResource(R.drawable.bg_pill_caramel);
        } else {
            txtDanhGiaLoiNhuan.setText("Lãi thấp");
            txtDanhGiaLoiNhuan.setBackgroundResource(R.drawable.bg_logo);
        }
    }

    private void taoNhomMoi() {
        EditText edt = new EditText(this);
        edt.setHint("Tên nhóm, VD: Sinh tố");
        edt.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        edt.setPadding(pad, pad / 2, pad, pad / 2);
        new AlertDialog.Builder(this)
                .setTitle("Tạo nhóm món mới")
                .setView(edt)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo", (d, w) -> {
                    String ten = edt.getText().toString().trim();
                    if (ten.isEmpty()) return;
                    ChayNen.chay(this, () -> db.themDanhMuc(ten), dm -> {
                        arrDanhMuc.add(dm);
                        adapterDanhMuc.notifyDataSetChanged();
                        spDanhMuc.setSelection(arrDanhMuc.size() - 1);
                    });
                })
                .show();
    }

    private void luuMon() {
        String ten = edtTenMon.getText().toString().trim();
        String ma = edtMaMon.getText().toString().trim().toUpperCase(Locale.ROOT);
        String moTa = edtMoTa.getText().toString().trim();
        long gia = TienIch.docSo(edtGiaBan.getText().toString());
        long von = TienIch.docSo(edtGiaVon.getText().toString());

        // kiểm tra dữ liệu
        if (ten.isEmpty()) {
            edtTenMon.setError("Nhập tên món");
            edtTenMon.requestFocus();
            return;
        }
        if (gia <= 0) {
            edtGiaBan.setError("Nhập giá bán");
            edtGiaBan.requestFocus();
            return;
        }
        if (spDanhMuc.getSelectedItem() == null) {
            Toast.makeText(this, "Hãy chọn hoặc tạo nhóm món", Toast.LENGTH_SHORT).show();
            return;
        }
        if (von > gia) {
            edtGiaVon.setError("Giá vốn đang lớn hơn giá bán");
            edtGiaVon.requestFocus();
            return;
        }

        ThucUong tu = thucUongSua != null ? thucUongSua : new ThucUong();
        tu.setTenMon(ten);
        tu.setMaMon(ma);
        tu.setMoTa(moTa);
        tu.setGia(gia);
        tu.setGiaVon(von);
        DanhMuc dm = (DanhMuc) spDanhMuc.getSelectedItem();
        tu.setDanhMucId(dm.getId());
        tu.setDanhMuc(dm.getTen());
        tu.setUriAnh(uriAnh);
        tu.setConBan(swConBan.isChecked());
        tu.setBanChay(swBanChay.isChecked());

        boolean laSua = thucUongSua != null;
        findViewById(R.id.btnLuuMon).setEnabled(false);
        ChayNen.chay(this, () -> {
            if (laSua) db.suaThucUong(tu);
            else db.themThucUong(tu);
            return true;
        }, ok -> {
            Toast.makeText(this, (laSua ? "Đã cập nhật món " : "Đã thêm món ") + ten, Toast.LENGTH_SHORT).show();
            finish();
        }, loi -> {
            findViewById(R.id.btnLuuMon).setEnabled(true);
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
        });
    }

    private void xoaMon() {
        if (thucUongSua == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Xóa món")
                .setMessage("Xóa \"" + thucUongSua.getTenMon() + "\" khỏi thực đơn?\n"
                        + "Các hóa đơn cũ vẫn giữ tên món.")
                .setNegativeButton("Không", null)
                .setPositiveButton("Xóa", (d, w) -> ChayNen.chay(this, () -> {
                    db.xoaThucUong(thucUongSua.getId());
                    return true;
                }, ok -> {
                    Toast.makeText(this, "Đã xóa món", Toast.LENGTH_SHORT).show();
                    finish();
                }))
                .show();
    }
}
