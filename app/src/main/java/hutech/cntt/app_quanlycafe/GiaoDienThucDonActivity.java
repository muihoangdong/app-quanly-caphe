package hutech.cntt.app_quanlycafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import hutech.cntt.app_quanlycafe.adapter.ThucUongAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.CafeDatabase.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.data.TienIch;
import hutech.cntt.app_quanlycafe.model.Ban;
import hutech.cntt.app_quanlycafe.model.ChiTietHoaDon;
import hutech.cntt.app_quanlycafe.model.DanhMuc;
import hutech.cntt.app_quanlycafe.model.HoaDon;
import hutech.cntt.app_quanlycafe.model.ThucUong;


public class GiaoDienThucDonActivity extends AppCompatActivity {

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

    ListView lvThucUong;
    RadioGroup rgDanhMuc;
    EditText edtTimKiem;
    TextView txtKhongCoMon, txtTongSoMon;
    LinearLayout layoutGioHang;
    TextView txtSoLuongGio, txtTongGio;

    CafeDatabase db;
    ArrayList<ThucUong> listThucUong;
    ThucUongAdapter adapter;
    ArrayList<DanhMuc> dsDanhMuc = new ArrayList<>();

    boolean cheDoQuanLy;
    int hoaDonId;     // > 0: gọi thêm vào đơn này
    int banId;        // > 0: tạo đơn cho bàn này
    String tenBan;
    int danhMucDangChon = 0;
    int lanTai = 0;

    // Bàn đã chọn trong giỏ hàng (mục Thực đơn): null = chưa chọn, id 0 = mang đi / tại quầy
    Ban banChon;
    int soKhach = 1;
    boolean dangGui;

    final LinkedHashMap<String, DongGio> gioHang = new LinkedHashMap<>();
    final HashMap<Integer, Integer> soLuongTheoMon = new HashMap<>();

    protected boolean laQuanLy() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (laQuanLy() && !DieuHuong.chiQuanLy(this)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_thuc_don);
        DieuHuong.apDungInsets(this);

        db = CafeDatabase.getInstance(this);

        lvThucUong = findViewById(R.id.lvThucUong);
        rgDanhMuc = findViewById(R.id.rgDanhMuc);
        edtTimKiem = findViewById(R.id.edtTimKiem);
        txtKhongCoMon = findViewById(R.id.txtKhongCoMon);
        txtTongSoMon = findViewById(R.id.txtTongSoMon);
        layoutGioHang = findViewById(R.id.layoutGioHang);
        txtSoLuongGio = findViewById(R.id.txtSoLuongGio);
        txtTongGio = findViewById(R.id.txtTongGio);

        cheDoQuanLy = laQuanLy();
        hoaDonId = getIntent().getIntExtra("hoaDonId", 0);
        banId = getIntent().getIntExtra("banId", 0);
        tenBan = getIntent().getStringExtra("tenBan");
        if (banId > 0) {
            Ban b = new Ban(banId, tenBan, "", getIntent().getIntExtra("soCho", 4), Ban.TRONG);
            banChon = b;
        }

        if (cheDoQuanLy) {
            ganCheDoQuanLy();
        } else {
            ganCheDoGoiMon();
        }

        listThucUong = new ArrayList<>();
        adapter = new ThucUongAdapter(this, listThucUong, !cheDoQuanLy, soLuongTheoMon, tu -> {
            if (cheDoQuanLy) {
                moManHinhSuaMon(tu);
            } else if (!tu.isConBan()) {
                Toast.makeText(this, tu.getTenMon() + " đang tạm hết", Toast.LENGTH_SHORT).show();
            } else {
                themVaoGio(tu, 1, "");
            }
        }, this::botKhoiGio);
        lvThucUong.setAdapter(adapter);

        lvThucUong.setOnItemClickListener((parent, view, position, id) -> {
            ThucUong tu = listThucUong.get(position);
            if (cheDoQuanLy) {
                moManHinhSuaMon(tu);
            } else if (tu.isConBan()) {
                hienThiTuyChonMon(tu);
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
    }

    private void ganCheDoGoiMon() {
        boolean tuThanhDieuHuong = hoaDonId == 0 && banId == 0;
        String tieuDe = hoaDonId > 0 ? "Gọi thêm món" : banId > 0 ? "Gọi món • " + tenBan : "Gọi món";
        DieuHuong.ganThanhTieuDe(this, tieuDe, tuThanhDieuHuong);
        DieuHuong.ganNutTieuDe(this, 1, R.drawable.ic_search, "Tìm món", v -> {
            edtTimKiem.requestFocus();
            ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(edtTimKiem, 0);
        });
        if (tuThanhDieuHuong) {
            DieuHuong.ganThanhDieuHuong(this, R.id.nav_thuc_don);
        } else {
            findViewById(R.id.bottomNav).setVisibility(View.GONE);
        }
        if (hoaDonId > 0) {
            ChayNen.chay(this, () -> db.layHoaDon(hoaDonId), hd -> {
                if (hd.getTrangThai() != HoaDon.DANG_PHUC_VU) {
                    Toast.makeText(this, "Đơn đã đóng", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                ((TextView) findViewById(R.id.txtTieuDeManHinh)).setText("Gọi thêm • " + hd.getTenBan());
            });
        }
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
                        .setMessage("Các món trong giỏ chưa được tạo đơn.")
                        .setNegativeButton("Ở lại", null)
                        .setPositiveButton("Bỏ giỏ", (d, w) -> {
                            gioHang.clear();
                            capNhatGioHang();
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        })
                        .show();
            }
        });
    }

    private void ganCheDoQuanLy() {
        DieuHuong.ganThanhTieuDe(this, "Thực đơn", false);
        findViewById(R.id.bottomNav).setVisibility(View.GONE);
        txtTongSoMon.setVisibility(View.VISIBLE);
        DieuHuong.ganNutTieuDe(this, 1, R.drawable.ic_add, "Thêm món", v -> {
            if (dsDanhMuc.isEmpty()) {
                Toast.makeText(this, "Hãy tạo danh mục trước khi thêm món", Toast.LENGTH_LONG).show();
                nhapTenDanhMuc(null);
                return;
            }
            startActivity(new Intent(this, GiaoDienThemMonActivity.class));
        });
        DieuHuong.ganNutTieuDe(this, 2, R.drawable.ic_category, "Danh mục", v -> hienThiQuanLyDanhMuc());
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDanhMuc();
        taiThucUong();
    }

    //DANH MỤC

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
                .setTitle("Danh mục")
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

    private void nhapTenDanhMuc(DanhMuc dmCu) {
        EditText edt = new EditText(this);
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

    //THỨC UỐNG

    private void taiThucUong() {
        int lan = ++lanTai;
        int danhMuc = danhMucDangChon;
        String tuKhoa = edtTimKiem.getText().toString();
        ChayNen.chay(this, () -> db.layDanhSachThucUong(danhMuc, tuKhoa), ds -> {
            if (lan != lanTai) return;
            listThucUong.clear();
            listThucUong.addAll(ds);
            adapter.notifyDataSetChanged();
            txtKhongCoMon.setText(dsDanhMuc.isEmpty() && tuKhoa.trim().isEmpty() && cheDoQuanLy
                    ? "Chưa có món nào.\nBấm dấu + phía trên để thêm món." : "Không tìm thấy món phù hợp");
            txtKhongCoMon.setVisibility(listThucUong.isEmpty() ? View.VISIBLE : View.GONE);
            if (cheDoQuanLy) txtTongSoMon.setText(listThucUong.size() + " món • chạm để sửa");
        });
    }

    private void moManHinhSuaMon(ThucUong tu) {
        Intent in = new Intent(this, GiaoDienThemMonActivity.class);
        in.putExtra("thucUongId", tu.getId());
        startActivity(in);
    }

    //GIỎ HÀNG

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


    private void botKhoiGio(ThucUong tu) {
        String khoaCuoi = null;
        for (String khoa : gioHang.keySet()) {
            if (gioHang.get(khoa).thucUong.getId() == tu.getId()) khoaCuoi = khoa;
        }
        if (khoaCuoi == null) return;
        DongGio dong = gioHang.get(khoaCuoi);
        dong.soLuong--;
        if (dong.soLuong <= 0) gioHang.remove(khoaCuoi);
        capNhatGioHang();
    }

    private int tongSoLuongGio() {
        int tong = 0;
        for (DongGio d : gioHang.values()) tong += d.soLuong;
        return tong;
    }

    private long tongTienGio() {
        long tong = 0;
        for (DongGio d : gioHang.values()) tong += d.soLuong * d.thucUong.getGia();
        return tong;
    }

    private void capNhatGioHang() {
        soLuongTheoMon.clear();
        for (DongGio d : gioHang.values()) {
            Integer cu = soLuongTheoMon.get(d.thucUong.getId());
            soLuongTheoMon.put(d.thucUong.getId(), (cu == null ? 0 : cu) + d.soLuong);
        }
        int tongSoLuong = tongSoLuongGio();
        layoutGioHang.setVisibility(tongSoLuong > 0 ? View.VISIBLE : View.GONE);
        txtSoLuongGio.setText(tongSoLuong + " món");
        txtTongGio.setText(TienIch.tien(tongTienGio()));
        adapter.notifyDataSetChanged();
    }

    private void hienThiTuyChonMon(ThucUong tu) {
        View view = getLayoutInflater().inflate(R.layout.dialog_tuy_chon_mon, null);
        ImageView img = view.findViewById(R.id.imgMonTuyChon);
        img.setClipToOutline(true);
        TienIch.hienThiAnhMon(img, tu.getUriAnh(), tu.getTenMon(), tu.getDanhMuc());
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
                    themVaoGio(tu, soLuong[0], TextUtils.join(" • ", ghiChu));
                })
                .show();
    }

    private void hienThiGioHang() {
        if (gioHang.isEmpty()) return;
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_gio_hang, null);
        sheet.setContentView(view);

        LinearLayout layoutDong = view.findViewById(R.id.layoutDongGio);
        TextView txtSoMon = view.findViewById(R.id.txtSoMonGio);
        TextView txtTongTien = view.findViewById(R.id.txtTongTienGio);
        TextView txtBan = view.findViewById(R.id.txtBanDaChon);
        TextView txtSoKhach = view.findViewById(R.id.txtSoKhachGio);
        EditText edtTenKhach = view.findViewById(R.id.edtTenKhachGio);
        View layoutChonBan = view.findViewById(R.id.layoutChonBan);
        MaterialButton btnTaoDon = view.findViewById(R.id.btnTaoDon);

        Runnable veLai = new Runnable() {
            @Override
            public void run() {
                if (gioHang.isEmpty()) {
                    sheet.dismiss();
                    return;
                }
                layoutDong.removeAllViews();
                for (String khoa : new ArrayList<>(gioHang.keySet())) {
                    DongGio d = gioHang.get(khoa);
                    View dong = getLayoutInflater().inflate(R.layout.item_gio_hang, layoutDong, false);
                    ImageView img = dong.findViewById(R.id.imgMonGio);
                    img.setClipToOutline(true);
                    TienIch.hienThiAnhMon(img, d.thucUong.getUriAnh(), d.thucUong.getTenMon(), d.thucUong.getDanhMuc());
                    ((TextView) dong.findViewById(R.id.txtTenMonGio)).setText(d.thucUong.getTenMon());
                    ((TextView) dong.findViewById(R.id.txtGiaMonGio)).setText(TienIch.tien(d.thucUong.getGia())
                            + (d.ghiChu.isEmpty() ? "" : " • " + d.ghiChu));
                    ((TextView) dong.findViewById(R.id.txtSoLuongMonGio)).setText(String.valueOf(d.soLuong));
                    Runnable self = this;
                    dong.findViewById(R.id.btnBotMon).setOnClickListener(v -> {
                        d.soLuong--;
                        if (d.soLuong <= 0) gioHang.remove(khoa);
                        capNhatGioHang();
                        self.run();
                    });
                    dong.findViewById(R.id.btnThemMonGio).setOnClickListener(v -> {
                        d.soLuong++;
                        capNhatGioHang();
                        self.run();
                    });
                    layoutDong.addView(dong);
                }
                txtSoMon.setText("Tổng cộng (" + tongSoLuongGio() + " món)");
                txtTongTien.setText(TienIch.tien(tongTienGio()));
                txtSoKhach.setText(String.valueOf(soKhach));
                if (hoaDonId > 0) return;
                if (banChon == null) {
                    txtBan.setText("Chọn bàn");
                    txtBan.setTextColor(getColor(R.color.mint_accent));
                } else {
                    txtBan.setText(banChon.getId() == 0 ? "Mang đi / tại quầy" : banChon.getTenBan());
                    txtBan.setTextColor(getColor(R.color.forest_green));
                }
                edtTenKhach.setVisibility(banChon != null && banChon.getId() == 0 ? View.VISIBLE : View.GONE);
            }
        };

        if (hoaDonId > 0) {
            // Gọi thêm vào đơn có sẵn: không cần chọn bàn / số khách
            layoutChonBan.setVisibility(View.GONE);
            view.findViewById(R.id.layoutSoKhachGio).setVisibility(View.GONE);
            btnTaoDon.setText("Gửi thêm vào đơn");
        } else if (banId > 0) {
            layoutChonBan.setClickable(false);
            view.findViewById(R.id.imgChonBan).setVisibility(View.GONE);
        } else {
            layoutChonBan.setOnClickListener(v -> chonBan(veLai));
        }
        view.findViewById(R.id.btnGiamKhachGio).setOnClickListener(v -> {
            if (soKhach > 1) soKhach--;
            veLai.run();
        });
        view.findViewById(R.id.btnTangKhachGio).setOnClickListener(v -> {
            if (soKhach < 99) soKhach++;
            veLai.run();
        });
        view.findViewById(R.id.btnXoaGio).setOnClickListener(v -> {
            gioHang.clear();
            capNhatGioHang();
            sheet.dismiss();
        });
        btnTaoDon.setOnClickListener(v -> {
            if (hoaDonId == 0 && banChon == null) {
                chonBan(veLai);
                return;
            }
            guiDon(sheet, btnTaoDon, edtTenKhach.getText().toString());
        });
        veLai.run();
        sheet.show();
    }

    private void chonBan(Runnable sauKhiChon) {
        ChayNen.chay(this, () -> db.layDanhSachBan(), dsBan -> {
            ArrayList<Ban> chon = new ArrayList<>();
            ArrayList<String> ten = new ArrayList<>();
            chon.add(new Ban(0, "Mang đi / tại quầy", "", 0, Ban.TRONG));
            ten.add("Mang đi / tại quầy");
            for (Ban b : dsBan) {
                if (!b.isTrong()) continue;
                chon.add(b);
                ten.add(b.getTenBan() + " • " + b.getKhuVuc()
                        + (b.getTrangThai() == Ban.DA_DAT ? " (đặt trước)" : ""));
            }
            new AlertDialog.Builder(this)
                    .setTitle("Phục vụ tại")
                    .setItems(ten.toArray(new String[0]), (d, i) -> {
                        banChon = chon.get(i);
                        sauKhiChon.run();
                    })
                    .setNegativeButton("Đóng", null)
                    .show();
        });
    }

    private void guiDon(BottomSheetDialog sheet, MaterialButton nut, String tenKhach) {
        if (gioHang.isEmpty() || dangGui) return;
        ArrayList<ChiTietHoaDon> dsMon = new ArrayList<>();
        for (DongGio d : gioHang.values()) {
            ChiTietHoaDon ct = new ChiTietHoaDon();
            ct.setThucUongId(d.thucUong.getId());
            ct.setSoLuong(d.soLuong);
            ct.setGhiChu(d.ghiChu);
            dsMon.add(ct);
        }
        int tongMon = tongSoLuongGio();
        dangGui = true;
        nut.setEnabled(false);
        ChayNen.chay(this, () -> {
            if (hoaDonId > 0) {
                db.themMonVaoHoaDon(hoaDonId, dsMon);
                return hoaDonId;
            }
            return db.taoDonHang(banChon.getId(), soKhach, tenKhach, dsMon);
        }, idDon -> {
            dangGui = false;
            gioHang.clear();
            capNhatGioHang();
            sheet.dismiss();
            if (hoaDonId > 0) {
                Toast.makeText(this, "Đã thêm " + tongMon + " món vào đơn", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Toast.makeText(this, "Đã tạo đơn " + (banChon.getId() == 0 ? "mang đi" : banChon.getTenBan()),
                    Toast.LENGTH_SHORT).show();
            if (banId == 0) {
                banChon = null;
                soKhach = 1;
            }
            ThaoTacBan.moThanhToan(this, idDon);
            if (banId > 0) finish();
        }, loi -> {
            dangGui = false;
            nut.setEnabled(true);
            Toast.makeText(this, loi, Toast.LENGTH_LONG).show();
        });
    }
}
