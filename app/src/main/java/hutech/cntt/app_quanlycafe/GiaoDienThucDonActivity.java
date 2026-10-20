package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import hutech.cntt.app_quanlycafe.adapter.ThucUongAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.DanhMuc;
import hutech.cntt.app_quanlycafe.model.HoaDon;
import hutech.cntt.app_quanlycafe.model.ThucUong;

/**
 * Màn hình thực đơn, có 2 chế độ:
 * - Gọi món (có extra "hoaDonId"): chọn món + số lượng vào giỏ rồi "Gửi Bar".
 * - Quản lý thực đơn (mở từ thanh điều hướng): thêm / sửa / xóa thức uống và danh mục.
 */
public class GiaoDienThucDonActivity extends AppCompatActivity {

    /** Một dòng trong giỏ hàng. */
    static class DongGio {
        ThucUong thucUong;
        int soLuong;
        String ghiChu;

        DongGio(ThucUong thucUong, int soLuong, String ghiChu) {
            this.thucUong = thucUong;
            this.soLuong = soLuong;
            this.ghiChu = ghiChu;
        }
    }

    GridView gvThucUong;
    RadioGroup rgDanhMuc;
    EditText edtTimKiem;
    TextView txtKhongCoMon, txtTongSoMon;
    LinearLayout layoutGioHang;
    TextView txtSoLuongGio, txtTieuDeGio, txtTongGio;

    CafeDatabase db;
    ArrayList<ThucUong> listThucUong;
    ThucUongAdapter adapter;
    ArrayList<DanhMuc> dsDanhMuc = new ArrayList<>();

    boolean cheDoGoiMon;
    boolean moGoiMonSauKhiGui;
    int hoaDonId;
    HoaDon hoaDon;
    int danhMucDangChon = 0; // 0 = tất cả
    int lanTai = 0;          // bỏ qua kết quả tìm kiếm cũ trả về muộn

    // Khóa: thucUongId + "|" + ghiChu
    final LinkedHashMap<String, DongGio> gioHang = new LinkedHashMap<>();
    final HashMap<Integer, Integer> soLuongTheoMon = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_thuc_don);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);

        gvThucUong = findViewById(R.id.gvThucUong);
        rgDanhMuc = findViewById(R.id.rgDanhMuc);
        edtTimKiem = findViewById(R.id.edtTimKiem);
        txtKhongCoMon = findViewById(R.id.txtKhongCoMon);
        txtTongSoMon = findViewById(R.id.txtTongSoMon);
        layoutGioHang = findViewById(R.id.layoutGioHang);
        txtSoLuongGio = findViewById(R.id.txtSoLuongGio);
        txtTieuDeGio = findViewById(R.id.txtTieuDeGio);
        txtTongGio = findViewById(R.id.txtTongGio);

        hoaDonId = getIntent().getIntExtra("hoaDonId", 0);
        moGoiMonSauKhiGui = getIntent().getBooleanExtra("moGoiMonSauKhiGui", false);
        cheDoGoiMon = hoaDonId > 0;

        if (cheDoGoiMon) {
            ganCheDoGoiMon();
        } else {
            ganCheDoQuanLy();
        }

        listThucUong = new ArrayList<>();
        adapter = new ThucUongAdapter(this, listThucUong, cheDoGoiMon, soLuongTheoMon, tu -> {
            if (cheDoGoiMon) {
                themVaoGio(tu, 1, "");
            } else {
                moManHinhSuaMon(tu);
            }
        });
        gvThucUong.setAdapter(adapter);

        gvThucUong.setOnItemClickListener((parent, view, position, id) -> {
            ThucUong tu = listThucUong.get(position);
            if (cheDoGoiMon) {
                hienThiTuyChonMon(tu);
            } else {
                moManHinhSuaMon(tu);
            }
        });

        edtTimKiem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                taiThucUong();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.imgThongBao).setOnClickListener(v ->
                Toast.makeText(this, "Không có thông báo mới", Toast.LENGTH_SHORT).show());
    }

    private void ganCheDoGoiMon() {
        findViewById(R.id.layoutQuanLyMon).setVisibility(View.GONE);
        findViewById(R.id.layoutBanDangGoi).setVisibility(View.VISIBLE);
        findViewById(R.id.bottomNav).setVisibility(View.GONE);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_thuc_don);

        // Thông tin bàn đang gọi món
        ChayNen.chay(this, () -> db.layHoaDon(hoaDonId), hd -> {
            if (hd.getTrangThai() != HoaDon.DANG_PHUC_VU) {
                Toast.makeText(this, "Order đã đóng", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            hoaDon = hd;
            ((TextView) findViewById(R.id.txtBanDangGoi)).setText(hd.getTenBan() + " • " + hd.getKhuVuc());
            ((TextView) findViewById(R.id.txtSoKhachDangGoi)).setText(hd.getSoKhach() + " khách");
            ((TextView) findViewById(R.id.txtGioVaoDangGoi)).setText(
                    "● Đang phục vụ • Bắt đầu lúc " + TienIch.gio(hd.getGioVao()));
            txtTieuDeGio.setText("GIỎ HÀNG " + hd.getTenBan().toUpperCase());
        });

        findViewById(R.id.btnXemOrder).setOnClickListener(v -> {
            if (moGoiMonSauKhiGui) ThaoTacBan.moChiTietOrder(this, hoaDonId);
            finish();
        });
        findViewById(R.id.btnGuiBar).setOnClickListener(v -> guiBar());
        findViewById(R.id.btnXemGio).setOnClickListener(v -> hienThiGioHang());

        // Hỏi lại trước khi thoát nếu giỏ còn món chưa gửi
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (gioHang.isEmpty()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    return;
                }
                new AlertDialog.Builder(GiaoDienThucDonActivity.this)
                        .setTitle("Bỏ giỏ hàng?")
                        .setMessage("Các món trong giỏ chưa được gửi xuống quầy Bar.")
                        .setNegativeButton("Ở lại", null)
                        .setPositiveButton("Bỏ giỏ", (d, w) -> finish())
                        .show();
            }
        });
    }

    private void ganCheDoQuanLy() {
        findViewById(R.id.layoutQuanLyMon).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutBanDangGoi).setVisibility(View.GONE);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_thuc_don);
        findViewById(R.id.btnThemMonMoi).setOnClickListener(v -> {
            if (dsDanhMuc.isEmpty()) {
                Toast.makeText(this, "Hãy tạo danh mục trước khi thêm món", Toast.LENGTH_LONG).show();
                nhapTenDanhMuc(null);
                return;
            }
            startActivity(new Intent(this, GiaoDienThemMonActivity.class));
        });
        findViewById(R.id.btnDanhMuc).setOnClickListener(v -> hienThiQuanLyDanhMuc());
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDanhMuc();
        taiThucUong();
    }

    // ---------------- Danh mục ----------------

    private void taiDanhMuc() {
        ChayNen.chay(this, db::layDanhSachDanhMuc, ds -> {
            dsDanhMuc = ds;
            taoChipDanhMuc();
        });
    }

    private void taoChipDanhMuc() {
        rgDanhMuc.setOnCheckedChangeListener(null);
        rgDanhMuc.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        boolean conDanhMuc = false;
        for (DanhMuc dm : dsDanhMuc) if (dm.getId() == danhMucDangChon) conDanhMuc = true;
        if (!conDanhMuc && danhMucDangChon != 0) {
            danhMucDangChon = 0;
            taiThucUong();
        }

        RadioButton chipTatCa = (RadioButton) inflater.inflate(R.layout.item_chip, rgDanhMuc, false);
        chipTatCa.setId(View.generateViewId());
        chipTatCa.setText("Tất cả");
        chipTatCa.setTag(0);
        rgDanhMuc.addView(chipTatCa);
        if (danhMucDangChon == 0) chipTatCa.setChecked(true);

        for (DanhMuc dm : dsDanhMuc) {
            RadioButton chip = (RadioButton) inflater.inflate(R.layout.item_chip, rgDanhMuc, false);
            chip.setId(View.generateViewId());
            chip.setText(dm.getTen());
            chip.setTag(dm.getId());
            rgDanhMuc.addView(chip);
            if (dm.getId() == danhMucDangChon) chip.setChecked(true);
        }

        rgDanhMuc.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton chip = group.findViewById(checkedId);
            if (chip == null) return;
            danhMucDangChon = (int) chip.getTag();
            taiThucUong();
        });
    }

    private void hienThiQuanLyDanhMuc() {
        String[] dong = new String[dsDanhMuc.size() + 1];
        dong[0] = "+ Thêm danh mục mới";
        for (int i = 0; i < dsDanhMuc.size(); i++) {
            DanhMuc dm = dsDanhMuc.get(i);
            dong[i + 1] = dm.getTen() + "  (" + dm.getSoMon() + " món)";
        }
        new AlertDialog.Builder(this)
                .setTitle("Danh mục thức uống")
                .setItems(dong, (d, which) -> {
                    if (which == 0) {
                        nhapTenDanhMuc(null);
                        return;
                    }
                    DanhMuc dm = dsDanhMuc.get(which - 1);
                    new AlertDialog.Builder(this)
                            .setTitle(dm.getTen())
                            .setItems(new String[]{"Đổi tên", "Xóa danh mục"}, (d2, i) -> {
                                if (i == 0) {
                                    nhapTenDanhMuc(dm);
                                } else {
                                    xoaDanhMuc(dm);
                                }
                            })
                            .show();
                })
                .setPositiveButton("Đóng", null)
                .show();
    }

    /** dmCu = null: thêm mới; khác null: đổi tên. */
    private void nhapTenDanhMuc(DanhMuc dmCu) {
        EditText edt = new EditText(this);
        edt.setHint("VD: Cà phê, Trà trái cây, Bánh ngọt...");
        if (dmCu != null) {
            edt.setText(dmCu.getTen());
            edt.setSelection(dmCu.getTen().length());
        }
        int dem = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout khung = new FrameLayout(this);
        khung.setPadding(dem, dem / 2, dem, 0);
        khung.addView(edt);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(dmCu == null ? "Thêm danh mục" : "Đổi tên danh mục")
                .setView(khung)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ten = edt.getText().toString().trim();
            if (ten.isEmpty()) {
                edt.setError("Nhập tên danh mục");
                return;
            }
            ChayNen.chay(this, () -> {
                if (dmCu == null) db.themDanhMuc(ten);
                else db.doiTenDanhMuc(dmCu.getId(), ten);
                return true;
            }, ok -> {
                Toast.makeText(this, "Đã lưu danh mục " + ten, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                taiDanhMuc();
                taiThucUong();
            }, edt::setError);
        });
    }

    private void xoaDanhMuc(DanhMuc dm) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa danh mục")
                .setMessage("Xóa danh mục \"" + dm.getTen() + "\"?")
                .setNegativeButton("Không", null)
                .setPositiveButton("Xóa", (d, w) -> ChayNen.chay(this, () -> {
                    db.xoaDanhMuc(dm.getId());
                    return true;
                }, ok -> {
                    Toast.makeText(this, "Đã xóa danh mục " + dm.getTen(), Toast.LENGTH_SHORT).show();
                    taiDanhMuc();
                }, loi -> new AlertDialog.Builder(this)
                        .setTitle("Không thể xóa")
                        .setMessage(loi)
                        .setPositiveButton("Đã hiểu", null)
                        .show()))
                .show();
    }

    // ---------------- Thức uống ----------------

    private void taiThucUong() {
        int lan = ++lanTai;
        int danhMuc = danhMucDangChon;
        String tuKhoa = edtTimKiem.getText().toString();
        ChayNen.chay(this, () -> db.layDanhSachThucUong(danhMuc, tuKhoa, cheDoGoiMon), ds -> {
            if (lan != lanTai) return;
            listThucUong.clear();
            listThucUong.addAll(ds);
            adapter.notifyDataSetChanged();
            txtKhongCoMon.setVisibility(listThucUong.isEmpty() ? View.VISIBLE : View.GONE);
            if (!cheDoGoiMon) {
                txtTongSoMon.setText(listThucUong.size() + " món • chạm vào món để sửa");
            }
        });
    }

    private void moManHinhSuaMon(ThucUong tu) {
        Intent in = new Intent(this, GiaoDienThemMonActivity.class);
        in.putExtra("thucUongId", tu.getId());
        startActivity(in);
    }

    // ---------------- Giỏ hàng (chế độ gọi món) ----------------

    private void themVaoGio(ThucUong tu, int soLuong, String ghiChu) {
        String khoa = tu.getId() + "|" + ghiChu;
        DongGio dong = gioHang.get(khoa);
        if (dong == null) {
            gioHang.put(khoa, new DongGio(tu, soLuong, ghiChu));
        } else {
            dong.soLuong += soLuong;
        }
        capNhatGioHang();
    }

    private void capNhatGioHang() {
        soLuongTheoMon.clear();
        int tongSoLuong = 0;
        long tongTien = 0;
        for (DongGio d : gioHang.values()) {
            Integer cu = soLuongTheoMon.get(d.thucUong.getId());
            soLuongTheoMon.put(d.thucUong.getId(), (cu == null ? 0 : cu) + d.soLuong);
            tongSoLuong += d.soLuong;
            tongTien += d.soLuong * d.thucUong.getGia();
        }
        layoutGioHang.setVisibility(tongSoLuong > 0 ? View.VISIBLE : View.GONE);
        txtSoLuongGio.setText(String.valueOf(tongSoLuong));
        txtTongGio.setText(TienIch.tien(tongTien));
        adapter.notifyDataSetChanged();
    }

    private void hienThiTuyChonMon(ThucUong tu) {
        View view = getLayoutInflater().inflate(R.layout.dialog_tuy_chon_mon, null);
        ImageView img = view.findViewById(R.id.imgMonTuyChon);
        img.setClipToOutline(true);
        TienIch.hienThiAnhMon(img, tu.getUriAnh(), tu.getDanhMuc());
        ((TextView) view.findViewById(R.id.txtMoTaTuyChon)).setText(tu.getMoTa());
        ((TextView) view.findViewById(R.id.txtGiaTuyChon)).setText(TienIch.tien(tu.getGia()));

        TextView txtSoLuong = view.findViewById(R.id.txtSoLuongTuyChon);
        final int[] soLuong = {1};
        view.findViewById(R.id.btnGiamSoLuong).setOnClickListener(v -> {
            if (soLuong[0] > 1) soLuong[0]--;
            txtSoLuong.setText(String.valueOf(soLuong[0]));
        });
        view.findViewById(R.id.btnTangSoLuong).setOnClickListener(v -> {
            soLuong[0]++;
            txtSoLuong.setText(String.valueOf(soLuong[0]));
        });

        boolean laDoUong = tu.getDanhMuc() == null || !tu.getDanhMuc().startsWith("Bánh");
        view.findViewById(R.id.layoutDuongDa).setVisibility(laDoUong ? View.VISIBLE : View.GONE);
        RadioGroup rgDuong = view.findViewById(R.id.rgDuong);
        RadioGroup rgDa = view.findViewById(R.id.rgDa);
        EditText edtGhiChu = view.findViewById(R.id.edtGhiChuMon);

        new AlertDialog.Builder(this)
                .setTitle(tu.getTenMon())
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Thêm vào giỏ", (d, w) -> {
                    ArrayList<String> ghiChu = new ArrayList<>();
                    if (laDoUong) {
                        int duong = rgDuong.getCheckedRadioButtonId();
                        if (duong == R.id.radDuong0) ghiChu.add("0% đường");
                        else if (duong == R.id.radDuong30) ghiChu.add("30% đường");
                        else if (duong == R.id.radDuong50) ghiChu.add("50% đường");
                        int da = rgDa.getCheckedRadioButtonId();
                        if (da == R.id.radDa0) ghiChu.add("Không đá");
                        else if (da == R.id.radDa50) ghiChu.add("Ít đá");
                    }
                    String khac = edtGhiChu.getText().toString().trim();
                    if (!khac.isEmpty()) ghiChu.add(khac);
                    themVaoGio(tu, soLuong[0], android.text.TextUtils.join(" • ", ghiChu));
                })
                .show();
    }

    private void hienThiGioHang() {
        ArrayList<String> khoa = new ArrayList<>(gioHang.keySet());
        String[] dong = new String[khoa.size()];
        for (int i = 0; i < khoa.size(); i++) {
            DongGio d = gioHang.get(khoa.get(i));
            dong[i] = d.soLuong + " × " + d.thucUong.getTenMon()
                    + (d.ghiChu.isEmpty() ? "" : " (" + d.ghiChu + ")")
                    + "  —  " + TienIch.tien(d.soLuong * d.thucUong.getGia());
        }
        new AlertDialog.Builder(this)
                .setTitle("Giỏ hàng • chạm để bớt 1")
                .setItems(dong, (dialog, i) -> {
                    DongGio d = gioHang.get(khoa.get(i));
                    d.soLuong--;
                    if (d.soLuong <= 0) gioHang.remove(khoa.get(i));
                    capNhatGioHang();
                    if (!gioHang.isEmpty()) hienThiGioHang();
                })
                .setNeutralButton("Xóa giỏ", (d, w) -> {
                    gioHang.clear();
                    capNhatGioHang();
                })
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void guiBar() {
        if (gioHang.isEmpty()) return;
        ArrayList<ChiTietHoaDon> dsMon = new ArrayList<>();
        int tong = 0;
        for (DongGio d : gioHang.values()) {
            ChiTietHoaDon ct = new ChiTietHoaDon();
            ct.setThucUongId(d.thucUong.getId());
            ct.setSoLuong(d.soLuong);
            ct.setGhiChu(d.ghiChu);
            dsMon.add(ct);
            tong += d.soLuong;
        }
        int tongMon = tong;
        findViewById(R.id.btnGuiBar).setEnabled(false);
        ChayNen.chay(this, () -> {
            db.themMonVaoHoaDon(hoaDonId, dsMon);
            return true;
        }, ok -> {
            gioHang.clear();
            Toast.makeText(this, "Đã gửi " + tongMon + " món xuống quầy Bar", Toast.LENGTH_SHORT).show();
            if (moGoiMonSauKhiGui) ThaoTacBan.moChiTietOrder(this, hoaDonId);
            finish();
        }, loi -> {
            findViewById(R.id.btnGuiBar).setEnabled(true);
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
        });
    }
}
