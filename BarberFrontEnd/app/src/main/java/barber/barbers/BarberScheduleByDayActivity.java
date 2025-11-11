package barber.barbers;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barber.R;

import barber.Session;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BarberScheduleByDayActivity extends AppCompatActivity {

    private static final String BASE = "http://10.0.2.2:8080/api";
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private EditText etDate;
    private ArrayAdapter<String> adShifts, adOffs, adBookings;
    private final ArrayList<String> rowsShifts   = new ArrayList<>();
    private final ArrayList<String> rowsOffs     = new ArrayList<>();
    private final ArrayList<String> rowsBookings = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_barber_schedule_by_day);

        etDate = findViewById(R.id.etDate);
        ListView lvShifts   = findViewById(R.id.listShifts);
        ListView lvOffs     = findViewById(R.id.listOffs);
        ListView lvBookings = findViewById(R.id.listBookings);
        Button btnPick = findViewById(R.id.btnPick);
        Button btnLoad = findViewById(R.id.btnLoad);

        adShifts   = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsShifts);
        adOffs     = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsOffs);
        adBookings = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rowsBookings);
        lvShifts.setAdapter(adShifts);
        lvOffs.setAdapter(adOffs);
        lvBookings.setAdapter(adBookings);

        // mặc định hôm nay
        etDate.setText(LocalDate.now().toString());

        btnPick.setOnClickListener(v -> openDatePicker());
        btnLoad.setOnClickListener(v -> load());

        load(); // tải lần đầu
    }

    private void openDatePicker() {
        LocalDate cur = LocalDate.now();
        try { cur = LocalDate.parse(etDate.getText().toString().trim()); } catch (Exception ignored) {}
        DatePickerDialog d = new DatePickerDialog(
                this,
                (view, y, m, day) -> etDate.setText(String.format("%04d-%02d-%02d", y, m+1, day)),
                cur.getYear(), cur.getMonthValue()-1, cur.getDayOfMonth()
        );
        d.show();
    }

    private void load() {
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Bạn cần đăng nhập BARBER"); return; }

        String date = etDate.getText().toString().trim();
        if (TextUtils.isEmpty(date)) { toast("Nhập ngày (yyyy-MM-dd)"); return; }

        new Thread(() -> {
            try {
                HttpUrl url = HttpUrl.parse(BASE + "/barber/day")
                        .newBuilder().addQueryParameter("date", date).build();
                Request req = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + token)
                        .get().build();

                try (Response r = http.newCall(req).execute()) {
                    String t = r.body()!=null ? r.body().string() : "";
                    if (!r.isSuccessful()) throw new IOException("HTTP "+r.code()+": "+t);

                    JSONObject o = new JSONObject(t);
                    fillShifts(o.optJSONArray("shifts"));
                    fillOffs(o.optJSONArray("timeOffs"));
                    fillBookings(o.optJSONArray("bookings"));

                    runOnUiThread(() -> {
                        adShifts.notifyDataSetChanged();
                        adOffs.notifyDataSetChanged();
                        adBookings.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) { toast("Lỗi tải: " + e.getMessage()); }
        }).start();
    }

    private void fillShifts(JSONArray arr){
        rowsShifts.clear();
        rowsShifts.add("[Ca làm]");
        if (arr == null) return;
        for (int i=0;i<arr.length();i++){
            JSONObject s = arr.optJSONObject(i);
            String line = s.optString("start")+"–"+s.optString("end")+" • "+s.optString("status");
            rowsShifts.add(line);
        }
    }

    private void fillOffs(JSONArray arr){
        rowsOffs.clear();
        rowsOffs.add("[Time-off]");
        if (arr == null) return;
        for (int i=0;i<arr.length();i++){
            JSONObject o = arr.optJSONObject(i);
            boolean all = o.optBoolean("allDay");
            String time = all ? "CẢ NGÀY" : (o.optString("start")+"–"+o.optString("end"));
            String reason = o.optString("reason","");
            rowsOffs.add(time + (reason.isEmpty() ? "" : " • " + reason));
        }
    }

    private void fillBookings(JSONArray arr){
        rowsBookings.clear();
        rowsBookings.add("[Khách đặt]");
        if (arr == null) return;
        for (int i=0;i<arr.length();i++){
            JSONObject b = arr.optJSONObject(i);
            String start = b.optString("start");
            String end   = b.optString("end");
            String svc   = b.optString("service");
            int price    = b.optInt("price");
            String stt   = b.optString("status");
            String pay   = b.optString("payMethod");
            String cus   = b.optString("customerName");
            String ph    = b.optString("customerPhone");
            rowsBookings.add(pretty(start, end)+" • "+svc+" • "+price+"đ • "+stt+"("+pay+") • "+cus+(ph.isEmpty()?"":" • "+ph));
        }
    }

    private String pretty(String s, String e){
        try { return s.substring(11,16) + "–" + e.substring(11,16); }
        catch (Exception ex){ return s + "–" + e; }
    }

    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
