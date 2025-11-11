package barber.owner;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barber.R;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

import barber.ApiClient;
import barber.Session;

public class ManageTimeOffsActivity extends AppCompatActivity {
    private EditText etFrom, etTo;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> rows = new ArrayList<>();
    private final ArrayList<Long> ids = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_manage_timeoffs);

        etFrom = findViewById(R.id.etFrom);
        etTo   = findViewById(R.id.etTo);
        ListView list = findViewById(R.id.listTimeoffs);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);

        findViewById(R.id.btnLoad).setOnClickListener(v -> load());     // lọc theo nhập tay
        findViewById(R.id.btnAdd).setOnClickListener(v -> openAddDialog());

        list.setOnItemLongClickListener((p,v,pos,id) -> { tryDelete(pos); return true; });

        // ➜ mở màn hình tự load tất cả
        load();
    }

    private void load() {
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Chưa đăng nhập Owner"); return; }

        // nếu để trống -> lấy mặc định siêu rộng
        String from = etFrom.getText().toString().trim();
        String to   = etTo.getText().toString().trim();
        if (TextUtils.isEmpty(from)) from = "2020-01-01";
        if (TextUtils.isEmpty(to))   to   = "2027-12-31";

        final String q = "/owner/timeoffs?from=" + from + "&to=" + to;

        new Thread(() -> {
            try {
                JSONArray arr = ApiClient.getArrayAuth(q, token);
                rows.clear(); ids.clear();
                for (int i=0;i<arr.length();i++){
                    JSONObject g = arr.getJSONObject(i);
                    String date = g.optString("date");
                    rows.add("["+date+"]"); ids.add(-1L);

                    JSONArray items = g.optJSONArray("items");
                    if (items == null || items.length()==0) continue;
                    for (int j=0;j<items.length();j++){
                        JSONObject it = items.getJSONObject(j);
                        long tid = it.optLong("id");
                        String type = it.optString("type"); // ALL_DAY | SLOT
                        String start = it.optString("start", "");
                        String end   = it.optString("end", "");
                        String reason= it.optString("reason", "");
                        JSONObject b = it.optJSONObject("barber");
                        String barber = (b!=null) ? ("#"+b.optLong("id")+" "+b.optString("name")) : "";
                        String phone  = (b!=null) ? b.optString("phone","") : "";

                        String line = (type.equals("ALL_DAY")? "CẢ NGÀY" : (start+"–"+end))
                                +" • "+barber + (phone.isEmpty()?"":" • "+phone)
                                + (reason.isEmpty()? "":" • "+reason);
                        rows.add(line); ids.add(tid);
                    }
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) { toast("Lỗi load: "+e.getMessage()); }
        }).start();
    }

    private void openAddDialog() {
        final EditText etDate   = new EditText(this); etDate.setHint("Ngày (yyyy-MM-dd)");
        final CheckBox cbAllDay = new CheckBox(this); cbAllDay.setText("Nghỉ cả ngày"); cbAllDay.setChecked(true);

        final EditText s1 = new EditText(this); s1.setHint("Start 1 (8 | 830 | 8:30)");
        final EditText e1 = new EditText(this); e1.setHint("End 1   (10 | 1030 | 10:30)");
        final EditText s2 = new EditText(this); s2.setHint("Start 2 (tuỳ chọn)");
        final EditText e2 = new EditText(this); e2.setHint("End 2");
        final EditText s3 = new EditText(this); s3.setHint("Start 3 (tuỳ chọn)");
        final EditText e3 = new EditText(this); e3.setHint("End 3");
        final LinearLayout slotsBox = vertical(s1,e1,s2,e2,s3,e3);

        cbAllDay.setOnCheckedChangeListener((b,checked)-> {
            slotsBox.setEnabled(!checked);
            for (int i=0;i<slotsBox.getChildCount();i++) slotsBox.getChildAt(i).setEnabled(!checked);
        });

        final EditText etReason = new EditText(this); etReason.setHint("Lý do (tuỳ chọn)");

        LinearLayout top = vertical(etDate, cbAllDay,
                label("Các khung giờ (khi KHÔNG chọn cả ngày)"), slotsBox,
                etReason);

        // tải thợ active
        new Thread(() -> {
            try {
                JSONArray arr = ApiClient.getArray("/barbers");
                String[] names = new String[arr.length()];
                long[] ids = new long[arr.length()];
                boolean[] checks = new boolean[arr.length()];
                for (int i=0;i<arr.length();i++){
                    JSONObject b = arr.getJSONObject(i);
                    ids[i] = b.optLong("id");
                    String label = b.optString("name");
                    JSONObject u = b.optJSONObject("user");
                    if (u != null && !u.optString("phone").isEmpty()) label += " • " + u.optString("phone");
                    names[i] = label;
                    checks[i] = false;
                }

                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Thêm Time-off")
                            .setView(top)
                            .setMultiChoiceItems(names, checks, (d,which,checked)-> checks[which]=checked)
                            .setPositiveButton("Lưu",(d,w)-> doCreate(
                                    etDate.getText().toString().trim(),
                                    cbAllDay.isChecked(),
                                    new String[]{s1.getText().toString().trim(), e1.getText().toString().trim(),
                                            s2.getText().toString().trim(), e2.getText().toString().trim(),
                                            s3.getText().toString().trim(), e3.getText().toString().trim()},
                                    etReason.getText().toString().trim(),
                                    ids, checks))
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            } catch (Exception e) { toast("Lỗi tải thợ: "+e.getMessage()); }
        }).start();
    }

    private void doCreate(String date, boolean allDay, String[] slot6, String reason, long[] idsAll, boolean[] checks) {
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Chưa đăng nhập Owner"); return; }
        if (TextUtils.isEmpty(date)) { toast("Thiếu ngày"); return; }

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("date", date);
                body.put("allDay", allDay);
                body.put("reason", reason);

                JSONArray barberIds = new JSONArray();
                for (int i=0;i<checks.length;i++) if (checks[i]) barberIds.put(idsAll[i]);
                if (barberIds.length()==0) { runOnUiThread(() -> toast("Chọn ít nhất 1 thợ")); return; }
                body.put("barberIds", barberIds);

                JSONArray slots = new JSONArray();
                if (!allDay) {
                    for (int i=0;i<slot6.length; i+=2) {
                        String st = slot6[i], en = slot6[i+1];
                        if (!TextUtils.isEmpty(st) && !TextUtils.isEmpty(en)) {
                            JSONObject s = new JSONObject();
                            s.put("start", st); s.put("end", en);
                            slots.put(s);
                        }
                    }
                    if (slots.length()==0) { runOnUiThread(() -> toast("Thêm ít nhất 1 khung giờ")); return; }
                }
                body.put("slots", slots);

                ApiClient.postAuth("/owner/timeoffs", body, token);
                runOnUiThread(this::load);
            } catch (Exception e) { toast("Lỗi tạo: "+e.getMessage()); }
        }).start();
    }

    private void tryDelete(int pos){
        long id = (pos>=0 && pos<ids.size()) ? ids.get(pos) : -1L;
        if (id <= 0) return; // header hoặc invalid
        new AlertDialog.Builder(this)
                .setMessage("Xoá time-off #"+id+" ?")
                .setPositiveButton("Xoá",(d,w)-> doDelete(id))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void doDelete(long id){
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Chưa đăng nhập Owner"); return; }
        new Thread(() -> {
            try { ApiClient.deleteAuth("/owner/timeoffs/"+id, token); runOnUiThread(this::load); }
            catch (Exception e){ toast("Lỗi xoá: "+e.getMessage()); }
        }).start();
    }

    // helpers UI
    private LinearLayout vertical(android.view.View... vs){
        LinearLayout box = new LinearLayout(this);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad,pad,pad,pad);
        for (var v: vs) box.addView(v);
        return box;
    }
    private TextView label(String t){ TextView tv = new TextView(this); tv.setText(t); return tv; }
    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
