package barber.owner;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barber.R;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

import barber.ApiClient;
import barber.Session;

public class ManageServicesActivity extends AppCompatActivity {
    private ArrayList<JSONObject> data = new ArrayList<>();
    private ArrayList<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_manage_services);

        ListView list = findViewById(R.id.listServices);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);

        findViewById(R.id.btnRefresh).setOnClickListener(v -> load());
        findViewById(R.id.btnCreate).setOnClickListener(v -> showCreateDialog());

        list.setOnItemClickListener((p, v, pos, id) -> showEditDialog(data.get(pos)));
        list.setOnItemLongClickListener((p, v, pos, id) -> { confirmDelete(data.get(pos)); return true; });

        load();
    }

    private void load() {
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Chưa đăng nhập Owner"); return; }

        new Thread(() -> {
            try {
                JSONArray arr = ApiClient.getArrayAuth("/owner/services", token);
                data.clear(); rows.clear();
                for (int i=0;i<arr.length();i++){
                    JSONObject o = arr.getJSONObject(i);
                    data.add(o);
                    String status = o.optBoolean("isActive") ? "● Hoạt động" : "○ Tạm tắt";
                    String result = "#"+o.optLong("id")+" • "
                            +o.optString("name")+" • "
                            +o.optInt("durationMin")+"phút • "
                            +o.optInt("price")+"đ "
                            +status;
                    rows.add(result);
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) { toast("Lỗi: "+e.getMessage()); }
        }).start();
    }

    private void showCreateDialog() {
        final EditText etName = new EditText(this); etName.setHint("Tên dịch vụ");
        final EditText etDur  = new EditText(this); etDur.setHint("Thời lượng (phút)"); etDur.setInputType(InputType.TYPE_CLASS_NUMBER);
        final EditText etPrice= new EditText(this); etPrice.setHint("Giá (VND)"); etPrice.setInputType(InputType.TYPE_CLASS_NUMBER);
        final CheckBox cbAct  = new CheckBox(this); cbAct.setText("Đang bán"); cbAct.setChecked(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad,pad,pad);
        box.addView(etName); box.addView(etDur); box.addView(etPrice); box.addView(cbAct);

        new AlertDialog.Builder(this)
                .setTitle("Tạo dịch vụ")
                .setView(box)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        String token = Session.getToken(this);
                        JSONObject body = new JSONObject();
                        body.put("name", etName.getText().toString().trim());
                        body.put("durationMin", Integer.parseInt(etDur.getText().toString().trim()));
                        body.put("price", Integer.parseInt(etPrice.getText().toString().trim()));
                        body.put("isActive", cbAct.isChecked());

                        new Thread(() -> {
                            try { ApiClient.postAuth("/owner/services", body, token); runOnUiThread(this::load); }
                            catch (Exception e){ toast("Lỗi: "+e.getMessage()); }
                        }).start();
                    } catch (Exception ex) { toast("Dữ liệu không hợp lệ"); }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditDialog(JSONObject o) {
        long id = o.optLong("id");
        final EditText etName = new EditText(this); etName.setHint("Tên"); etName.setText(o.optString("name"));
        final EditText etDur  = new EditText(this); etDur.setHint("Thời lượng"); etDur.setInputType(InputType.TYPE_CLASS_NUMBER); etDur.setText(String.valueOf(o.optInt("durationMin")));
        final EditText etPrice= new EditText(this); etPrice.setHint("Giá"); etPrice.setInputType(InputType.TYPE_CLASS_NUMBER); etPrice.setText(String.valueOf(o.optInt("price")));
        final CheckBox cbAct  = new CheckBox(this); cbAct.setText("Đang bán"); cbAct.setChecked(o.optBoolean("isActive"));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad,pad,pad);
        box.addView(etName); box.addView(etDur); box.addView(etPrice); box.addView(cbAct);

        new AlertDialog.Builder(this)
                .setTitle("Sửa dịch vụ #"+id)
                .setView(box)
                .setPositiveButton("Cập nhật", (d, w) -> {
                    try {
                        String token = Session.getToken(this);
                        JSONObject body = new JSONObject();
                        body.put("name", etName.getText().toString().trim());
                        body.put("durationMin", Integer.parseInt(etDur.getText().toString().trim()));
                        body.put("price", Integer.parseInt(etPrice.getText().toString().trim()));
                        body.put("isActive", cbAct.isChecked());

                        new Thread(() -> {
                            try { ApiClient.putAuth("/owner/services/"+id, body, token); runOnUiThread(this::load); }
                            catch (Exception e){ toast("Lỗi: "+e.getMessage()); }
                        }).start();
                    } catch (Exception ex) { toast("Dữ liệu không hợp lệ"); }
                })
                .setNeutralButton("Xóa", (d, w) -> confirmDelete(o))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void confirmDelete(JSONObject o){
        long id = o.optLong("id");
        new AlertDialog.Builder(this)
                .setMessage("Xóa dịch vụ #"+id+" ?")
                .setPositiveButton("Xóa", (d,w)-> {
                    String token = Session.getToken(this);
                    new Thread(() -> {
                        try { ApiClient.deleteAuth("/owner/services/"+id, token); runOnUiThread(this::load); }
                        catch (Exception e){ toast("Lỗi: "+e.getMessage()); }
                    }).start();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
