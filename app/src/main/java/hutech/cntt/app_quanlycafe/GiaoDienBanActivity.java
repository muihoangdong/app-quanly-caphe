package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

import hutech.cntt.app_quanlycafe.adapter.BanAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.CafeDatabase.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.model.Ban;


public class GiaoDienBanActivity extends AppCompatActivity {

    GridView gvBan;
    RadioGroup rgTrangThai;
    RadioButton radTatCa, radTrong, radPhucVu, radCanXuLy;
    EditText edtTimBan;
    TextView txtKhongCoBan;

    ArrayList<Ban> tatCaBan = new ArrayList<>();
    ArrayList<Ban> listBan = new ArrayList<>();
    BanAdapter adapter;
    CafeDatabase db;
    boolean quanLy; // chỉ quản lý được thêm / sửa / xóa bàn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_ban);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_ban);
        DieuHuong.ganThanhTieuDe(this, "Quản lý bàn", true);
        DieuHuong.ganNutTieuDe(this, 1, R.drawable.ic_search, "Tìm bàn", v -> batTatTimKiem());

        db = CafeDatabase.getInstance(this);
        quanLy = db.laQuanLy();
        if (quanLy) DieuHuong.ganNutTieuDe(this, 2, R.drawable.ic_add, "Thêm bàn", v -> hienThiDialogBan(null));

        gvBan = findViewById(R.id.gvBan);
        rgTrangThai = findViewById(R.id.rgTrangThaiBan);
        radTatCa = findViewById(R.id.radTatCaBan);
        radTrong = findViewById(R.id.radBanTrong);
        radPhucVu = findViewById(R.id.radBanPhucVu);
        radCanXuLy = findViewById(R.id.radBanCanXuLy);
        edtTimBan = findViewById(R.id.edtTimBan);
        txtKhongCoBan = findViewById(R.id.txtKhongCoBan);

        adapter = new BanAdapter(this, listBan);
        gvBan.setAdapter(adapter);

        gvBan.setOnItemClickListener((parent, view, position, id) -> ThaoTacBan.goiMon(this, listBan.get(position)));
        gvBan.setOnItemLongClickListener((parent, view, position, id) -> {
            hienThiTuyChonBan(listBan.get(position));
            return true;
        });

        rgTrangThai.setOnCheckedChangeListener((group, checkedId) -> hienThi());
        edtTimBan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                hienThi();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    private void taiDuLieu() {
        ChayNen.chay(this, () -> db.layDanhSachBan(), ds -> {
            tatCaBan = ds;
            hienThi();
        });
    }

    private void batTatTimKiem() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (edtTimBan.getVisibility() == View.VISIBLE) {
            edtTimBan.setText("");
            edtTimBan.setVisibility(View.GONE);
            imm.hideSoftInputFromWindow(edtTimBan.getWindowToken(), 0);
        } else {
            edtTimBan.setVisibility(View.VISIBLE);
            edtTimBan.requestFocus();
            imm.showSoftInput(edtTimBan, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hienThi() {
        int trong = 0, phucVu = 0, canXuLy = 0;
        for (Ban b : tatCaBan) {
            if (b.isTrong() && !b.isCanXuLy()) trong++;
            if (!b.isTrong()) phucVu++;
            if (b.isCanXuLy()) canXuLy++;
        }
        radTatCa.setText("Tất cả  " + tatCaBan.size());
        radTrong.setText("Trống  " + trong);
        radPhucVu.setText("Đang phục vụ  " + phucVu);
        radCanXuLy.setText("Bàn đặt trước" + "  " + canXuLy);

        int loc = rgTrangThai.getCheckedRadioButtonId();
        String tuKhoa = edtTimBan.getText().toString().trim().toLowerCase(Locale.ROOT);
        listBan.clear();
        for (Ban b : tatCaBan) {
            if (loc == R.id.radBanTrong && !(b.isTrong() && !b.isCanXuLy())) continue;
            if (loc == R.id.radBanPhucVu && b.isTrong()) continue;
            if (loc == R.id.radBanCanXuLy && !b.isCanXuLy()) continue;
            if (!tuKhoa.isEmpty() && !b.getTenBan().toLowerCase(Locale.ROOT).contains(tuKhoa)
                    && !b.getKhuVuc().toLowerCase(Locale.ROOT).contains(tuKhoa)) continue;
            listBan.add(b);
        }
        adapter.notifyDataSetChanged();
        txtKhongCoBan.setText(!tatCaBan.isEmpty() ? "Không có bàn nào"
                : quanLy ? "Quán chưa có bàn nào.\nBấm dấu + phía trên để thêm bàn."
                : "Quán chưa có bàn nào.\nHãy nhờ quản lý thêm bàn.");
        txtKhongCoBan.setVisibility(listBan.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void ghi(ChayNen.Viec<?> viec, String thongBao) {
        ChayNen.chay(this, () -> {
            viec.lam();
            return true;
        }, ok -> {
            Toast.makeText(this, thongBao, Toast.LENGTH_SHORT).show();
            taiDuLieu();
        });
    }

    // ---------------- Tùy chọn bàn (nhấn giữ) ----------------

    private void hienThiTuyChonBan(Ban ban) {
        ArrayList<String> tuyChon = new ArrayList<>();
        if (ban.isTrong()) {
            tuyChon.add("Gọi món");
            tuyChon.add(ban.getTrangThai() == Ban.DA_DAT ? "Hủy đặt trước" : "Đánh dấu đặt trước");
            if (quanLy) {
                tuyChon.add("Sửa thông tin bàn");
                tuyChon.add("Xóa bàn");
            }
        } else {
            tuyChon.add("Xem đơn & thanh toán");
            tuyChon.add("Gọi thêm món");
            tuyChon.add("Chuyển sang bàn khác");
            tuyChon.add("Gộp vào bàn khác");
            if (quanLy) tuyChon.add("Sửa thông tin bàn");
        }
        new AlertDialog.Builder(this)
                .setTitle(ban.getTenBan() + " • " + Ban.tenTrangThai(ban.getTrangThaiHienThi()))
                .setItems(tuyChon.toArray(new String[0]), (d, which) -> {
                    String chon = tuyChon.get(which);
                    if (chon.equals("Gọi món") || chon.startsWith("Xem")) {
                        ThaoTacBan.goiMon(this, ban);
                    } else if (chon.startsWith("Gọi thêm")) {
                        ThaoTacBan.moThucDon(this, ban.getHoaDonId());
                    } else if (chon.endsWith("đặt trước")) {
                        doiDatTruoc(ban);
                    } else if (chon.startsWith("Sửa")) {
                        hienThiDialogBan(ban);
                    } else if (chon.startsWith("Xóa")) {
                        xacNhanXoaBan(ban);
                    } else if (chon.startsWith("Chuyển")) {
                        chonBanDeDoi(ban);
                    } else {
                        chonBanDeGop(ban);
                    }
                })
                .show();
    }

    /** Bàn chưa có đơn: bật / tắt Đặt trước. */
    private void doiDatTruoc(Ban ban) {
        int moi = ban.getTrangThai() == Ban.DA_DAT ? Ban.TRONG : Ban.DA_DAT;
        ghi(() -> {
            db.capNhatTrangThaiBan(ban.getId(), moi);
            return null;
        }, ban.getTenBan() + " → " + Ban.tenTrangThai(moi));
    }

    private void hienThiDialogBan(Ban banSua) {
        View view = getLayoutInflater().inflate(R.layout.dialog_ban, null);
        EditText edtTenBan = view.findViewById(R.id.edtTenBan);
        AutoCompleteTextView edtKhuVuc = view.findViewById(R.id.edtKhuVuc);
        EditText edtSoCho = view.findViewById(R.id.edtSoCho);

        LinkedHashSet<String> khuVuc = new LinkedHashSet<>();
        for (Ban b : tatCaBan) khuVuc.add(b.getKhuVuc());
        edtKhuVuc.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(khuVuc)));
        edtKhuVuc.setOnClickListener(v -> edtKhuVuc.showDropDown());

        if (banSua != null) {
            edtTenBan.setText(banSua.getTenBan());
            edtKhuVuc.setText(banSua.getKhuVuc());
            edtSoCho.setText(String.valueOf(banSua.getSoCho()));
        } else {
            edtTenBan.setText("Bàn " + (tatCaBan.size() + 1));
            if (!khuVuc.isEmpty()) edtKhuVuc.setText(khuVuc.iterator().next());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(banSua == null ? "Thêm bàn mới" : "Sửa " + banSua.getTenBan())
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ten = edtTenBan.getText().toString().trim();
            String kv = edtKhuVuc.getText().toString().trim();
            String soChoStr = edtSoCho.getText().toString().trim();
            if (ten.isEmpty()) {
                edtTenBan.setError("Nhập tên bàn");
                edtTenBan.requestFocus();
                return;
            }
            if (kv.isEmpty()) {
                edtKhuVuc.setError("Nhập khu vực");
                edtKhuVuc.requestFocus();
                return;
            }
            int soCho = soChoStr.isEmpty() ? 4 : Integer.parseInt(soChoStr);
            if (soCho <= 0) {
                edtSoCho.setError("Số chỗ phải lớn hơn 0");
                return;
            }
            Ban ban = banSua != null ? banSua : new Ban();
            ban.setTenBan(ten);
            ban.setKhuVuc(kv);
            ban.setSoCho(soCho);
            ChayNen.chay(this, () -> {
                if (banSua == null) db.themBan(ban);
                else db.suaBan(ban);
                return true;
            }, ok -> {
                Toast.makeText(this, (banSua == null ? "Đã thêm " : "Đã cập nhật ") + ten,
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                taiDuLieu();
            }, loi -> edtTenBan.setError(loi));
        });
    }

    private void xacNhanXoaBan(Ban ban) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bàn")
                .setMessage("Bạn có chắc muốn xóa " + ban.getTenBan() + " (" + ban.getKhuVuc() + ")?")
                .setNegativeButton("Không", null)
                .setPositiveButton("Xóa", (d, w) -> ghi(() -> {
                    db.xoaBan(ban.getId());
                    return null;
                }, "Đã xóa " + ban.getTenBan()))
                .show();
    }

    /** coKhach = true: bàn đang có đơn; false: bàn trống (kể cả đặt trước). */
    private ArrayList<Ban> locBan(boolean coKhach, Ban boQua) {
        ArrayList<Ban> kq = new ArrayList<>();
        for (Ban b : tatCaBan) {
            if (b.getId() == boQua.getId()) continue;
            if (coKhach != b.isTrong()) kq.add(b);
        }
        return kq;
    }

    private String[] tenCacBan(ArrayList<Ban> ds) {
        String[] ten = new String[ds.size()];
        for (int i = 0; i < ds.size(); i++) ten[i] = ds.get(i).toString();
        return ten;
    }

    private void chonBanDeDoi(Ban banNguon) {
        ArrayList<Ban> dsTrong = locBan(false, banNguon);
        if (dsTrong.isEmpty()) {
            Toast.makeText(this, "Không còn bàn trống", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Chuyển " + banNguon.getTenBan() + " sang")
                .setItems(tenCacBan(dsTrong), (d, i) -> {
                    Ban banMoi = dsTrong.get(i);
                    ghi(() -> {
                        db.doiBan(banNguon.getHoaDonId(), banMoi.getId());
                        return null;
                    }, "Đã chuyển " + banNguon.getTenBan() + " → " + banMoi.getTenBan());
                })
                .show();
    }

    private void chonBanDeGop(Ban nguon) {
        ArrayList<Ban> dsDich = locBan(true, nguon);
        if (dsDich.isEmpty()) {
            Toast.makeText(this, "Cần thêm một bàn đang phục vụ để gộp", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Gộp " + nguon.getTenBan() + " vào")
                .setItems(tenCacBan(dsDich), (d, j) -> {
                    Ban dich = dsDich.get(j);
                    ghi(() -> {
                        db.gopBan(nguon.getHoaDonId(), dich.getHoaDonId());
                        return null;
                    }, "Đã gộp " + nguon.getTenBan() + " vào " + dich.getTenBan());
                })
                .show();
    }
}
