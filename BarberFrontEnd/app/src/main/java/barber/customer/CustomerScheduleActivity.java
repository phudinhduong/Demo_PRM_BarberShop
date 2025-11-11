package barber.customer;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barber.R;

import barber.ApiClient;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class CustomerScheduleActivity extends AppCompatActivity {

    private static final String BASE = "http://10.0.2.2:8080/api";
    private static final MediaType JSON_MT = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private EditText etDate;
    private Spinner spBarber;
    private ArrayAdapter<String> adMorning, adAfternoon, adEvening;
    private final ArrayList<String> rowsM = new ArrayList<>();
    private final ArrayList<String> rowsA = new ArrayList<>();
    private final ArrayList<String> rowsE = new ArrayList<>();

    private final ArrayList<Long> barberIds = new ArrayList<>();
    private final ArrayList<String> barberLabels = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_customer_schedule);

        etDate = findViewById(R.id.etDate);
        spBarber = findViewById(R.id.spBarber);

        ListView lvM = findViewById(R.id.listMorning);
        ListView lvA = findViewById(R.id.listAfternoon);
        ListView lvE = findViewById(R.id.listEvening);

        adMorning  = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsM);
        adAfternoon= new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsA);
        adEvening  = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsE);
        lvM.setAdapter(adMorning); lvA.setAdapter(adAfternoon); lvE.setAdapter(adEvening);

        findViewById(R.id.btnLoad).setOnClickListener(v -> loadDay());

        // 1) Set mặc định hôm nay (yyyy-MM-dd)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            etDate.setText(LocalDate.now().toString());
        }

        // 2) Tải danh sách thợ cho spinner (không chặn việc load lịch)
        loadBarbers();

        // 3) Tự load lịch ngày hôm nay ngay khi mở màn hình
        loadDay();
    }

    private void loadBarbers() {
        new Thread(() -> {
            try {
                JSONArray arr = ApiClient.getArray("/barbers");
                barberIds.clear(); barberLabels.clear();
                // All
                barberIds.add(0L);
                barberLabels.add("Tất cả thợ");
                for (int i=0;i<arr.length();i++){
                    JSONObject b = arr.getJSONObject(i);
                    if (!b.optBoolean("isActive", true)) continue;
                    long id = b.optLong("id");
                    String name = b.optString("name");
                    String phone = "";
                    JSONObject u = b.optJSONObject("user");
                    if (u != null) phone = u.optString("phone", "");
                    barberIds.add(id);
                    barberLabels.add("#"+id+" "+name + (phone.isEmpty()?"":" • "+phone));
                }
                runOnUiThread(() ->
                        spBarber.setAdapter(new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_dropdown_item, barberLabels))
                );
            } catch (Exception e) { toast("Lỗi tải thợ: "+e.getMessage()); }
        }).start();
    }

    private void loadDay() {
        String date = etDate.getText().toString().trim();
        if (TextUtils.isEmpty(date)) { toast("Nhập ngày (yyyy-MM-dd)"); return; }

        Long barberId;
        int idx = spBarber.getSelectedItemPosition();
        if (idx >= 0 && idx < barberIds.size()) {
            long v = barberIds.get(idx);
            if (v != 0L) barberId = v; // 0 = tất cả
            else {
                barberId = null;
            }
        } else {
            barberId = null;
        }

        new Thread(() -> {
            try {
                HttpUrl.Builder url = HttpUrl.parse(BASE + "/shifts/day").newBuilder()
                        .addQueryParameter("date", date);
                if (barberId != null) url.addQueryParameter("barberId", String.valueOf(barberId));

                Request req = new Request.Builder().url(url.build()).get().build();
                try (Response r = http.newCall(req).execute()) {
                    String t = r.body() != null ? r.body().string() : "";
                    if (!r.isSuccessful()) throw new IOException("HTTP "+r.code()+": "+t);

                    JSONObject o = new JSONObject(t);
                    fillBucket(o.optJSONObject("morning"), rowsM, "Sáng");
                    fillBucket(o.optJSONObject("afternoon"), rowsA, "Chiều");
                    fillBucket(o.optJSONObject("evening"), rowsE, "Tối");

                    runOnUiThread(() -> {
                        adMorning.notifyDataSetChanged();
                        adAfternoon.notifyDataSetChanged();
                        adEvening.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) { toast("Lỗi load: " + e.getMessage()); }
        }).start();
    }

    private void fillBucket(JSONObject bucket, ArrayList<String> rows, String label){
        rows.clear();
        if (bucket == null) return;
        String start = bucket.optString("start");
        String end   = bucket.optString("end");
        rows.add("[" + label + " " + start + "–" + end + "]");
        JSONArray bs = bucket.optJSONArray("barbers");
        if (bs == null) return;
        for (int i=0;i<bs.length();i++){
            JSONObject b = bs.optJSONObject(i);
            String name = b != null ? b.optString("name") : "";
            String phone= b != null ? b.optString("phone") : "";
            rows.add(name + (phone.isEmpty() ? "" : " • " + phone));
        }
    }

    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
