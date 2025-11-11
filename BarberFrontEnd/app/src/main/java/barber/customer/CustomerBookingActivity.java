package barber.customer;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barber.R;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import barber.ApiClient;
import barber.Session;

public class CustomerBookingActivity extends AppCompatActivity {

    private EditText etDate, etTime;
    private Spinner spService, spBarber;
    private RadioGroup rgMethod;
    private Button btnBook;

    // dữ liệu dropdown
    private final ArrayList<Long> serviceIds = new ArrayList<>();
    private final ArrayList<String> serviceLabels = new ArrayList<>();
    private final ArrayList<Long> barberIds = new ArrayList<>();
    private final ArrayList<String> barberLabels = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_customer_booking);

        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        spService = findViewById(R.id.spService);
        spBarber = findViewById(R.id.spBarber);
        rgMethod = findViewById(R.id.rgMethod);
        btnBook  = findViewById(R.id.btnBook);

        btnBook.setOnClickListener(v -> createBooking());

        loadServices();
        loadBarbers();
    }

    private void loadServices() {
        new Thread(() -> {
            try {
                JSONArray arr = ApiClient.getArray("/services");
                serviceIds.clear(); serviceLabels.clear();
                for (int i=0;i<arr.length();i++){
                    JSONObject o = arr.getJSONObject(i);
                    if (!o.optBoolean("isActive", true)) continue;
                    long id = o.optLong("id");
                    String name = o.optString("name");
                    int dur = o.optInt("durationMin");
                    int price = o.optInt("price");
                    serviceIds.add(id);
                    serviceLabels.add(name + " • " + dur + "p • " + price + "đ");
                }
                runOnUiThread(() -> {
                    spService.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, serviceLabels));
                });
            } catch (Exception e) { toast("Lỗi services: " + e.getMessage()); }
        }).start();
    }

    private void loadBarbers() {
        new Thread(() -> {
            try {
                JSONArray arr = ApiClient.getArray("/barbers");
                barberIds.clear(); barberLabels.clear();
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
                runOnUiThread(() -> {
                    spBarber.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, barberLabels));
                });
            } catch (Exception e) { toast("Lỗi barbers: " + e.getMessage()); }
        }).start();
    }

    private void createBooking() {
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Hãy đăng nhập tài khoản CUSTOMER"); return; }

        String date = etDate.getText().toString().trim();     // yyyy-MM-dd
        String time = etTime.getText().toString().trim();     // HH:mm (vd 09:00)
        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            toast("Nhập ngày (yyyy-MM-dd) và giờ (HH:mm)"); return;
        }
        if (serviceIds.isEmpty() || barberIds.isEmpty()) { toast("Chưa tải xong dữ liệu"); return; }

        int sIdx = spService.getSelectedItemPosition();
        int bIdx = spBarber.getSelectedItemPosition();
        if (sIdx < 0 || bIdx < 0) { toast("Chọn dịch vụ và thợ"); return; }

        String method = (rgMethod.getCheckedRadioButtonId() == R.id.rbVnpay) ? "VNPAY" : "CASH";

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("date", date);
                body.put("startTime", time);
                body.put("serviceId", serviceIds.get(sIdx));
                body.put("barberId",  barberIds.get(bIdx));
                body.put("method", method);

                JSONObject res = ApiClient.postAuth("/customer/bookings", body, token);
                long bookingId = res.optLong("bookingId", -1);
                String payUrl  = res.optString("payUrl", null);

                runOnUiThread(() -> {
                    if (bookingId <= 0) { toast("Tạo lịch thất bại"); return; }
                    if ("VNPAY".equals(method) && payUrl != null && !payUrl.isEmpty()) {
                        // mở trang VNPay
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
                        startActivity(i);
                    } else {
                        toast("Đã tạo lịch #" + bookingId + " (CASH)");
                    }
                });
            } catch (Exception e) { toast("Lỗi đặt lịch: " + e.getMessage()); }
        }).start();
    }

    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
