package hutech.cntt.app_quanlycafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import hutech.cntt.app_quanlycafe.adapter.BanAdapter;
import hutech.cntt.app_quanlycafe.data.CafeDatabase;
import hutech.cntt.app_quanlycafe.data.ChayNen;
import hutech.cntt.app_quanlycafe.data.DieuHuong;
import hutech.cntt.app_quanlycafe.model.Ban;

/**
 * Quản lý bàn: sơ đồ bàn theo khu vực, chọn bàn để gọi món, cập nhật trạng thái,
 * thêm / sửa / xóa bàn, đổi bàn, gộp bàn.
 */
public class GiaoDienBanActivity extends AppCompatActivity {

    GridView gvBan;
    RadioGroup rgKhuVuc;
    TextView txtSoCoKhach, txtSoBanTrong, txtSoChoThanhToan, txtKhongCoBan;

    ArrayList<Ban> tatCaBan = new ArrayList<>();
    ArrayList<Ban> listBan = new ArrayList<>();
    LinkedHashMap<String, Integer> dsKhuVuc = new LinkedHashMap<>();
    BanAdapter adapter;
    CafeDatabase db;

    String khuVucDangChon = null; // null = tất cả

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_giao_dien_ban);
        DieuHuong.apDungInsets(this);
        DieuHuong.ganThanhDieuHuong(this, R.id.nav_ban);

        db = CafeDatabase.getInstance(this);

        gvBan = findViewById(R.id.gvBan);
        rgKhuVuc = findViewById(R.id.rgKhuVuc);
        txtSoCoKhach = findViewById(R.id.txtSoCoKhach);
        txtSoBanTrong = findViewById(R.id.txtSoBanTrong);
        txtSoChoThanhToan = findViewById(R.id.txtSoChoThanhToan);
        txtKhongCoBan = findViewById(R.id.txtKhongCoBan);

        adapter = new BanAdapter(this, listBan, ban -> {
            if (ban.getTrangThai() == Ban.CHO_THANH_TOAN) {
                ThaoTacBan.moChiTietOrder(this, ban.getHoaDonId());
            } else {
                ThaoTacBan.goiMon(this, ban);
            }
        });
        gvBan.setAdapter(adapter);

        gvBan.setOnItemClickListener((parent, view, position, id) -> {
            Ban ban = listBan.get(position);
            if (ban.isTrong()) {
                ThaoTacBan.goiMon(this, ban);
            } else {
                ThaoTacBan.moChiTietOrder(this, ban.getHoaDonId());
            }
        });
        gvBan.setOnItemLongClickListener((parent, view, position, id) -> {
            hienThiTuyChonBan(listBan.get(position));
            return true;
        });

        findViewById(R.id.btnThemBan).setOnClickListener(v -> hienThiDialogBan(null));
        findViewById(R.id.btnDoiBan).setOnClickListener(v -> chonBanDeDoi(null));
        findViewById(R.id.btnGopBan).setOnClickListener(v -> chonBanDeGop());
        findViewById(R.id.imgThongBao).setOnClickListener(v -> hienThiThongBao());
    }

    @Override
    protected void onResume() {
        super.onResume();
        taiDuLieu();
    }

    private void taiDuLieu() {
        ChayNen.chay(this, () -> {
            Object[] kq = new Object[2];
            kq[0] = db.layDanhSachBan(null);
            kq[1] = db.layDanhSachKhuVuc();
            return kq;
        }, kq -> {
            @SuppressWarnings("unchecked") ArrayList<Ban> ds = (ArrayList<Ban>) kq[0];
            @SuppressWarnings("unchecked") LinkedHashMap<String, Integer> kv = (LinkedHashMap<String, Integer>) kq[1];
            tatCaBan = ds;
            dsKhuVuc = kv;
            if (khuVucDangChon != null && !dsKhuVuc.containsKey(khuVucDangChon)) khuVucDangChon = null;
            hienThi();
        });
    }

    private void hienThi() {
        int coKhach = 0, trong = 0, choTinh = 0;
        for (Ban b : tatCaBan) {
            if (b.getTrangThai() == Ban.CHO_THANH_TOAN) choTinh++;
            else if (!b.isTrong()) coKhach++;
            else if (b.getTrangThai() == Ban.TRONG) trong++;
        }
        txtSoCoKhach.setText(String.format("%02d", coKhach));
        txtSoBanTrong.setText(String.format("%02d", trong));
        txtSoChoThanhToan.setText(String.format("%02d", choTinh));

        taoChipKhuVuc();

        listBan.clear();
        for (Ban b : tatCaBan) {
            if (khuVucDangChon == null || khuVucDangChon.equals(b.getKhuVuc())) listBan.add(b);
        }
        adapter.notifyDataSetChanged();
        txtKhongCoBan.setVisibility(tatCaBan.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void taoChipKhuVuc() {
        rgKhuVuc.setOnCheckedChangeListener(null);
        rgKhuVuc.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        RadioButton chipTatCa = (RadioButton) inflater.inflate(R.layout.item_chip, rgKhuVuc, false);
        chipTatCa.setId(View.generateViewId());
        chipTatCa.setText("Tất cả  " + tatCaBan.size());
        rgKhuVuc.addView(chipTatCa);
        if (khuVucDangChon == null) chipTatCa.setChecked(true);

        for (Map.Entry<String, Integer> e : dsKhuVuc.entrySet()) {
            String kv = e.getKey();
            RadioButton chip = (RadioButton) inflater.inflate(R.layout.item_chip, rgKhuVuc, false);
            chip.setId(View.generateViewId());
            chip.setText(kv + "  " + e.getValue());
            chip.setTag(kv);
            chip.setOnLongClickListener(v -> {
                doiTenKhuVuc(kv);
                return true;
            });
            rgKhuVuc.addView(chip);
            if (kv.equals(khuVucDangChon)) chip.setChecked(true);
        }

        rgKhuVuc.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton chip = group.findViewById(checkedId);
            if (chip == null) return;
            khuVucDangChon = (String) chip.getTag();
            hienThi();
        });
    }

    /** Chạy một thao tác ghi rồi báo và tải lại sơ đồ bàn. */
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
            tuyChon.add("Mở bàn & gọi món");
            tuyChon.add("Cập nhật trạng thái bàn");
            tuyChon.add("Sửa thông tin bàn");
            tuyChon.add("Xóa bàn");
        } else {
            tuyChon.add("Xem order");
            tuyChon.add("Gọi thêm món");
            tuyChon.add("Đổi sang bàn khác");
            tuyChon.add("Sửa thông tin bàn");
        }
        new AlertDialog.Builder(this)
                .setTitle(ban.getTenBan() + " • " + Ban.tenTrangThai(ban.getTrangThai()))
                .setItems(tuyChon.toArray(new String[0]), (d, which) -> {
                    String chon = tuyChon.get(which);
                    if (chon.startsWith("Mở") || chon.startsWith("Gọi")) {
                        ThaoTacBan.goiMon(this, ban);
                    } else if (chon.startsWith("Cập nhật")) {
                        chonTrangThai(ban);
                    } else if (chon.startsWith("Sửa")) {
                        hienThiDialogBan(ban);
                    } else if (chon.startsWith("Xóa")) {
                        xacNhanXoaBan(ban);
                    } else if (chon.startsWith("Xem")) {
                        ThaoTacBan.moChiTietOrder(this, ban.getHoaDonId());
                    } else {
                        chonBanDeDoi(ban);
                    }
                })
                .show();
    }

    /** Bàn không có khách chỉ đổi được giữa Trống / Đã đặt / Bảo trì. */
    private void chonTrangThai(Ban ban) {
        int[] trangThai = {Ban.TRONG, Ban.DA_DAT, Ban.BAO_TRI};
        String[] ten = new String[trangThai.length];
        int dangChon = 0;
        for (int i = 0; i < trangThai.length; i++) {
            ten[i] = Ban.tenTrangThai(trangThai[i]);
            if (trangThai[i] == ban.getTrangThai()) dangChon = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("Trạng thái " + ban.getTenBan())
                .setSingleChoiceItems(ten, dangChon, (d, i) -> {
                    d.dismiss();
                    if (trangThai[i] == ban.getTrangThai()) return;
                    ghi(() -> {
                        db.capNhatTrangThaiBan(ban.getId(), trangThai[i]);
                        return null;
                    }, ban.getTenBan() + " → " + ten[i]);
                })
                .show();
    }

    // ---------------- Thêm / sửa / xóa bàn ----------------

    private void hienThiDialogBan(Ban banSua) {
        View view = getLayoutInflater().inflate(R.layout.dialog_ban, null);
        EditText edtTenBan = view.findViewById(R.id.edtTenBan);
        AutoCompleteTextView edtKhuVuc = view.findViewById(R.id.edtKhuVuc);
        EditText edtSoCho = view.findViewById(R.id.edtSoCho);

        edtKhuVuc.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(dsKhuVuc.keySet())));
        edtKhuVuc.setOnClickListener(v -> edtKhuVuc.showDropDown());

        if (banSua != null) {
            edtTenBan.setText(banSua.getTenBan());
            edtKhuVuc.setText(banSua.getKhuVuc());
            edtSoCho.setText(String.valueOf(banSua.getSoCho()));
        } else if (khuVucDangChon != null) {
            edtKhuVuc.setText(khuVucDangChon);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(banSua == null ? "Thêm bàn mới" : "Sửa " + banSua.getTenBan())
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();
        dialog.show();
        // Tự xử lý nút Lưu để không đóng hộp thoại khi dữ liệu sai
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ten = edtTenBan.getText().toString().trim();
            String khuVuc = edtKhuVuc.getText().toString().trim();
            String soChoStr = edtSoCho.getText().toString().trim();
            if (ten.isEmpty()) {
                edtTenBan.setError("Nhập tên bàn");
                edtTenBan.requestFocus();
                return;
            }
            if (khuVuc.isEmpty()) {
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
            ban.setKhuVuc(khuVuc);
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

    /** Nhấn giữ chip khu vực để đổi tên khu vực (áp dụng cho mọi bàn trong khu vực). */
    private void doiTenKhuVuc(String tenCu) {
        EditText edt = new EditText(this);
        edt.setText(tenCu);
        edt.setSelection(tenCu.length());
        int dem = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout khung = new FrameLayout(this);
        khung.setPadding(dem, dem / 2, dem, 0);
        khung.addView(edt);
        new AlertDialog.Builder(this)
                .setTitle("Đổi tên khu vực")
                .setMessage("Khu vực tự mất đi khi không còn bàn nào trong đó.")
                .setView(khung)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    String moi = edt.getText().toString().trim();
                    if (moi.isEmpty() || moi.equals(tenCu)) return;
                    if (tenCu.equals(khuVucDangChon)) khuVucDangChon = moi;
                    ghi(() -> {
                        db.doiTenKhuVuc(tenCu, moi);
                        return null;
                    }, "Đã đổi " + tenCu + " → " + moi);
                })
                .show();
    }

    // ---------------- Đổi bàn / Gộp bàn ----------------

    private ArrayList<Ban> locBan(boolean coKhach, Ban boQua) {
        ArrayList<Ban> kq = new ArrayList<>();
        for (Ban b : tatCaBan) {
            if (boQua != null && b.getId() == boQua.getId()) continue;
            if (coKhach && !b.isTrong()) kq.add(b);
            if (!coKhach && b.isTrong() && b.getTrangThai() != Ban.BAO_TRI) kq.add(b);
        }
        return kq;
    }

    private String[] tenCacBan(ArrayList<Ban> ds) {
        String[] ten = new String[ds.size()];
        for (int i = 0; i < ds.size(); i++) ten[i] = ds.get(i).toString();
        return ten;
    }

    /** banNguon = null thì cho người dùng chọn bàn nguồn trước. */
    private void chonBanDeDoi(Ban banNguon) {
        if (banNguon == null) {
            ArrayList<Ban> dsCoKhach = locBan(true, null);
            if (dsCoKhach.isEmpty()) {
                Toast.makeText(this, "Chưa có bàn nào có khách", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Đổi bàn: chọn bàn hiện tại")
                    .setItems(tenCacBan(dsCoKhach), (d, i) -> chonBanDeDoi(dsCoKhach.get(i)))
                    .show();
            return;
        }
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

    private void chonBanDeGop() {
        ArrayList<Ban> dsCoKhach = locBan(true, null);
        if (dsCoKhach.size() < 2) {
            Toast.makeText(this, "Cần ít nhất 2 bàn có khách để gộp", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Gộp bàn: chọn bàn cần gộp")
                .setItems(tenCacBan(dsCoKhach), (d, i) -> {
                    Ban nguon = dsCoKhach.get(i);
                    ArrayList<Ban> dsDich = locBan(true, nguon);
                    new AlertDialog.Builder(this)
                            .setTitle("Gộp " + nguon.getTenBan() + " vào")
                            .setItems(tenCacBan(dsDich), (d2, j) -> {
                                Ban dich = dsDich.get(j);
                                ghi(() -> {
                                    db.gopBan(nguon.getHoaDonId(), dich.getHoaDonId());
                                    return null;
                                }, "Đã gộp " + nguon.getTenBan() + " vào " + dich.getTenBan());
                            })
                            .show();
                })
                .show();
    }

    private void hienThiThongBao() {
        StringBuilder sb = new StringBuilder();
        for (Ban b : tatCaBan) {
            if (b.getTrangThai() == Ban.CHO_THANH_TOAN) {
                sb.append("• ").append(b.getTenBan()).append(" đang chờ thanh toán\n");
            } else if (b.getTrangThai() == Ban.DA_DAT) {
                sb.append("• ").append(b.getTenBan()).append(" đã được đặt trước\n");
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Thông báo")
                .setMessage(sb.length() == 0 ? "Không có thông báo mới." : sb.toString().trim())
                .setPositiveButton("Đóng", null)
                .show();
    }
}
