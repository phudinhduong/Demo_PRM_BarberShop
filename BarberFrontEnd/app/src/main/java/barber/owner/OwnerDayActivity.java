package barber.owner;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barber.R;

import barber.ApiClient;
import barber.Session;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class OwnerDayActivity extends AppCompatActivity {

    private static final String BASE = "http://10.0.2.2:8080/api";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private EditText etDate;
    private ArrayAdapter<String> adMorning, adAfternoon, adEvening;
    private final ArrayList<String> rowsMorning = new ArrayList<>();
    private final ArrayList<String> rowsAfternoon = new ArrayList<>();
    private final ArrayList<String> rowsEvening = new ArrayList<>();
    // Nếu backend trả kèm regId trong mỗi barber, lưu để huỷ
    private final ArrayList<Long> idsMorning = new ArrayList<>();
    private final ArrayList<Long> idsAfternoon = new ArrayList<>();
    private final ArrayList<Long> idsEvening = new ArrayList<>();

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_owner_day);

        etDate = findViewById(R.id.etDate);

        ListView lvMorning  = findViewById(R.id.listMorning);
        ListView lvAfternoon= findViewById(R.id.listAfternoon);
        ListView lvEvening  = findViewById(R.id.listEvening);

        adMorning  = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsMorning);
        adAfternoon= new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsAfternoon);
        adEvening  = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsEvening);

        lvMorning.setAdapter(adMorning);
        lvAfternoon.setAdapter(adAfternoon);
        lvEvening.setAdapter(adEvening);

        findViewById(R.id.btnLoad).setOnClickListener(v -> loadDay());
        findViewById(R.id.btnRegister).setOnClickListener(v -> openRegisterDialog());

        // long-press để huỷ (nếu có regId từ backend)
        lvMorning.setOnItemLongClickListener((p,v,pos,id)-> { tryCancel(idsMorning, pos); return true; });
        lvAfternoon.setOnItemLongClickListener((p,v,pos,id)-> { tryCancel(idsAfternoon, pos); return true; });
        lvEvening.setOnItemLongClickListener((p,v,pos,id)-> { tryCancel(idsEvening, pos); return true; });
    }

    private void loadDay() {
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Chưa đăng nhập Owner"); return; }

        String date = etDate.getText().toString().trim();
        if (TextUtils.isEmpty(date)) { toast("Nhập ngày dạng yyyy-MM-dd"); return; }

        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(BASE + "/owner/shifts/day?date=" + date)
                        .addHeader("Authorization", "Bearer " + token)
                        .get().build();
                try (Response r = http.newCall(req).execute()) {
                    String t = r.body() != null ? r.body().string() : "";
                    if (!r.isSuccessful()) throw new IOException("HTTP "+r.code()+": "+t);

                    JSONObject o = new JSONObject(t);
                    fillBucket(o.optJSONObject("morning"), rowsMorning, idsMorning, "Sáng");
                    fillBucket(o.optJSONObject("afternoon"), rowsAfternoon, idsAfternoon, "Chiều");
                    fillBucket(o.optJSONObject("evening"), rowsEvening, idsEvening, "Tối");

                    runOnUiThread(() -> {
                        adMorning.notifyDataSetChanged();
                        adAfternoon.notifyDataSetChanged();
                        adEvening.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) { toast("Lỗi load: " + e.getMessage()); }
        }).start();
    }

    private void fillBucket(JSONObject bucket, ArrayList<String> rows, ArrayList<Long> ids, String label){
        rows.clear(); ids.clear();
        if (bucket == null) return;
        String start = bucket.optString("start");
        String end   = bucket.optString("end");
        rows.add("[" + label + " " + start + "–" + end + "]"); ids.add(-1L);

        JSONArray bs = bucket.optJSONArray("barbers");
        if (bs == null) return;
        for (int i=0;i<bs.length();i++){
            JSONObject b = bs.optJSONObject(i);
            long regId = b != null ? b.optLong("regId", -1) : -1; // nếu backend có trả regId
            String name = b != null ? b.optString("name") : "";
            String phone= b != null ? b.optString("phone") : "";
            rows.add(name + (phone.isEmpty() ? "" : " • " + phone));
            ids.add(regId);
        }
    }

    private void openRegisterDialog() {
        final EditText etDateDlg = new EditText(this); etDateDlg.setHint("Ngày (yyyy-MM-dd)");
        etDateDlg.setText(etDate.getText().toString().trim());

        final RadioGroup rg = new RadioGroup(this);
        RadioButton rbM = new RadioButton(this); rbM.setText("MORNING");
        RadioButton rbA = new RadioButton(this); rbA.setText("AFTERNOON");
        RadioButton rbE = new RadioButton(this); rbE.setText("EVENING");
        rg.addView(rbM); rg.addView(rbA); rg.addView(rbE); rbM.setChecked(true);

        LinearLayout boxTop = new LinearLayout(this);
        boxTop.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        boxTop.setPadding(pad,pad,pad,pad);
        boxTop.addView(etDateDlg);
        boxTop.addView(rg);

        // tải danh sách thợ active để chọn nhiều
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
                    if (u != null && !u.optString("phone").isEmpty()) {
                        label += " • " + u.optString("phone");
                    }
                    names[i] = label;
                    checks[i] = false;
                }

                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Đăng ký ca (chọn ngày, khung giờ, thợ)")
                            .setView(boxTop)
                            .setMultiChoiceItems(names, checks, (d, which, isChecked) -> checks[which] = isChecked)
                            .setPositiveButton("Lưu", (d,w)-> {
                                String date = etDateDlg.getText().toString().trim();
                                if (TextUtils.isEmpty(date)) { toast("Thiếu ngày"); return; }
                                int checkedId = rg.getCheckedRadioButtonId();
                                String slot = ((RadioButton)rg.findViewById(checkedId)).getText().toString();

                                // gom barberIds
                                ArrayList<Long> pick = new ArrayList<>();
                                for (int i=0;i<checks.length;i++) if (checks[i]) pick.add(ids[i]);
                                if (pick.isEmpty()) { toast("Chọn ít nhất 1 thợ"); return; }

                                doRegister(date, slot, pick);
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            } catch (Exception e) { toast("Lỗi tải thợ: " + e.getMessage()); }
        }).start();
    }

    private void doRegister(String date, String slot, ArrayList<Long> barberIds) {
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Chưa đăng nhập Owner"); return; }

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("date", date);
                body.put("slot", slot);
                JSONArray arr = new JSONArray();
                for (Long id : barberIds) arr.put(id);
                body.put("barberIds", arr);

                JSONObject res = ApiClient.postAuth("/owner/shifts/register", body, token);
                // có thể đọc createdIds/ skipped để hiển thị
                runOnUiThread(this::loadDay);
            } catch (Exception e) { toast("Đăng ký lỗi: " + e.getMessage()); }
        }).start();
    }

    private void tryCancel(ArrayList<Long> ids, int pos){
        long regId = (pos >=0 && pos < ids.size()) ? ids.get(pos) : -1L;
        if (regId <= 0) { toast("Backend chưa trả regId trong bucket, chưa huỷ được trực tiếp."); return; }

        final EditText etReason = new EditText(this); etReason.setHint("Lý do huỷ");
        new AlertDialog.Builder(this)
                .setTitle("Huỷ ca #" + regId)
                .setView(etReason)
                .setPositiveButton("Huỷ", (d,w)-> doCancel(regId, etReason.getText().toString().trim()))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void doCancel(long id, String reason){
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Chưa đăng nhập Owner"); return; }

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                if (!TextUtils.isEmpty(reason)) body.put("reason", reason);
                RequestBody rb = RequestBody.create(body.toString(), JSON);
                Request req = new Request.Builder()
                        .url(BASE + "/owner/shifts/" + id + "/cancel")
                        .addHeader("Authorization", "Bearer " + token)
                        .patch(rb).build();
                try (Response r = http.newCall(req).execute()) {
                    if (!r.isSuccessful())
                        throw new IOException("HTTP "+r.code()+": "+(r.body()!=null?r.body().string():""));
                }
                runOnUiThread(this::loadDay);
            } catch (Exception e){ toast("Huỷ lỗi: " + e.getMessage()); }
        }).start();
    }

    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}

