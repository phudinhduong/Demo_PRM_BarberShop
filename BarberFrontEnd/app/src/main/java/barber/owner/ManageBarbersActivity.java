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

public class ManageBarbersActivity extends AppCompatActivity {
    private final ArrayList<JSONObject> data = new ArrayList<>();
    private final ArrayList<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_manage_barbers);

        ListView list = findViewById(R.id.listBarbers);
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
                JSONArray arr = ApiClient.getArrayAuth("/owner/barbers", token);
                data.clear(); rows.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    data.add(o);
                    String status = o.optBoolean("isActive") ? "● Hoạt động" : "○ Tạm tắt";
                    JSONObject u = o.optJSONObject("user");
                    String userStr = (u != null) ? (" | user#" + u.optLong("id") + " • " + u.optString("email")) : "";
                    rows.add("#"+o.optLong("id")+" • "+o.optString("name")+userStr+" • "+status);
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) { toast("Lỗi: " + e.getMessage()); }
        }).start();
    }

    private void showCreateDialog() {
        // --- Khối USER ---
        final EditText etUserId  = new EditText(this);
        etUserId.setHint("userId (nếu đã có)");
        etUserId.setInputType(InputType.TYPE_CLASS_NUMBER);

        final TextView tvOr = new TextView(this);
        tvOr.setText("hoặc tạo User mới:");

        final EditText etUserEmail = new EditText(this);
        etUserEmail.setHint("userEmail");
        etUserEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        final EditText etUserPass  = new EditText(this);
        etUserPass.setHint("userPassword");
        etUserPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final EditText etUserName  = new EditText(this);
        etUserName.setHint("userName (tuỳ chọn)");

        // --- Khối BARBER ---
        final EditText etName = new EditText(this); etName.setHint("Tên thợ (barber.name)");
        final EditText etBio  = new EditText(this);  etBio.setHint("Bio (tuỳ chọn)");
//        final EditText etAvt  = new EditText(this);  etAvt.setHint("Avatar URL (tuỳ chọn)");
//        etAvt.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        final CheckBox cbAct  = new CheckBox(this);  cbAct.setText("Đang hoạt động"); cbAct.setChecked(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad,pad,pad);
        box.addView(etUserId);
        box.addView(tvOr);
        box.addView(etUserEmail);
        box.addView(etUserPass);
        box.addView(etUserName);
        box.addView(etName);
        box.addView(etBio);
//        box.addView(etAvt);
        box.addView(cbAct);

        new AlertDialog.Builder(this)
                .setTitle("Tạo thợ (chọn userId HOẶC nhập email+password)")
                .setView(box)
                .setPositiveButton("Lưu", (d,w)-> {
                    String token = Session.getToken(this);
                    new Thread(() -> {
                        try {
                            JSONObject body = new JSONObject();

                            // Nếu có userId -> dùng userId; ngược lại yêu cầu email+password
                            String userIdStr = etUserId.getText().toString().trim();
                            if (!userIdStr.isEmpty()) {
                                body.put("userId", Long.parseLong(userIdStr));
                            } else {
                                String email = etUserEmail.getText().toString().trim();
                                String pass  = etUserPass.getText().toString().trim();
                                if (email.isEmpty() || pass.isEmpty())
                                    throw new IllegalArgumentException("Thiếu userEmail hoặc userPassword");
                                body.put("userEmail", email);
                                body.put("userPassword", pass);
                                String uname = etUserName.getText().toString().trim();
                                if (!uname.isEmpty()) body.put("userName", uname);
                            }

                            // Thuộc tính Barber
                            body.put("name", etName.getText().toString().trim());
                            body.put("bio", etBio.getText().toString().trim());
//                            body.put("avatarUrl", etAvt.getText().toString().trim());
                            body.put("isActive", cbAct.isChecked());

                            ApiClient.postAuth("/owner/barbers", body, token);
                            runOnUiThread(this::load);
                        } catch (Exception e) { toast("Lỗi: " + e.getMessage()); }
                    }).start();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditDialog(JSONObject o) {
        long id = o.optLong("id");

        final EditText etName = new EditText(this); etName.setHint("Tên"); etName.setText(o.optString("name"));
        final EditText etBio  = new EditText(this); etBio.setHint("Bio");  etBio.setText(o.optString("bio"));
//        final EditText etAvt  = new EditText(this); etAvt.setHint("Avatar URL");
//        etAvt.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
//        etAvt.setText(o.optString("avatarUrl"));
        final CheckBox cbAct  = new CheckBox(this); cbAct.setText("Đang hoạt động"); cbAct.setChecked(o.optBoolean("isActive"));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad,pad,pad);
        box.addView(etName); box.addView(etBio);
//        box.addView(etAvt);
        box.addView(cbAct);

        new AlertDialog.Builder(this)
                .setTitle("Sửa thợ #"+id+" (không đổi user tại đây)")
                .setView(box)
                .setPositiveButton("Cập nhật", (d,w)-> {
                    String token = Session.getToken(this);
                    new Thread(() -> {
                        try {
                            JSONObject body = new JSONObject();
                            body.put("name", etName.getText().toString().trim());
                            body.put("bio", etBio.getText().toString().trim());
//                            body.put("avatarUrl", etAvt.getText().toString().trim());
                            body.put("isActive", cbAct.isChecked());
                            ApiClient.putAuth("/owner/barbers/"+id, body, token);
                            runOnUiThread(this::load);
                        } catch (Exception e) { toast("Lỗi: " + e.getMessage()); }
                    }).start();
                })
                .setNeutralButton("Xóa", (d,w)-> confirmDelete(o))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void confirmDelete(JSONObject o) {
        long id = o.optLong("id");
        new AlertDialog.Builder(this)
                .setMessage("Xóa thợ #"+id+" ?")
                .setPositiveButton("Xóa", (d,w)-> {
                    String token = Session.getToken(this);
                    new Thread(() -> {
                        try {
                            ApiClient.deleteAuth("/owner/barbers/"+id, token);
                            runOnUiThread(this::load);
                        } catch (Exception e) { toast("Lỗi: " + e.getMessage()); }
                    }).start();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
