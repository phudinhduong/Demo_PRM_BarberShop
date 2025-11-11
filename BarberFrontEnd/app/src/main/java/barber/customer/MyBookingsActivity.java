package barber.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barber.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

import barber.ApiClient;
import barber.Session;

public class MyBookingsActivity extends AppCompatActivity {
    private final ArrayList<JSONObject> data = new ArrayList<>();
    private final ArrayList<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_my_bookings);

        ListView list = findViewById(R.id.listMyBookings);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);

        findViewById(R.id.btnReload).setOnClickListener(v -> load());

        list.setOnItemClickListener((p,v,pos,id) -> showActions(pos));

        load();
    }

    private void load() {
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Hãy đăng nhập CUSTOMER"); return; }

        new Thread(() -> {
            try {
                JSONArray arr = ApiClient.getArrayAuth("/customer/bookings/my", token);
                data.clear(); rows.clear();
                for (int i=0;i<arr.length();i++){
                    JSONObject o = arr.getJSONObject(i);
                    data.add(o);
                    long id = o.optLong("id");
                    String startDt = o.optString("startDt"); // ISO: yyyy-MM-ddTHH:mm:ss
                    String when = pretty(startDt);
                    int price = o.optInt("price");
                    String status = o.optString("status");
                    String method = o.optString("payMethod");
                    String svc = safeName(o.optJSONObject("service"));
                    String barber = safeName(o.optJSONObject("barber"));

                    String line = "#"+id+" • "+when+" • "+svc+" • "+barber
                            +" • "+price+"đ • "+status+"("+method+")";
                    rows.add(line);
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) { toast("Lỗi tải: "+e.getMessage()); }
        }).start();
    }

    private void showActions(int pos){
        if (pos < 0 || pos >= data.size()) return;
        JSONObject o = data.get(pos);
        long id = o.optLong("id");
        String status = o.optString("status");
        String method = o.optString("payMethod");

        ArrayList<String> opts = new ArrayList<>();
        if ("PENDING".equals(status)) {
            opts.add("Thanh toán VNPay");
        }
        opts.add("Đóng");

        new AlertDialog.Builder(this)
                .setTitle("Booking #"+id)
                .setItems(opts.toArray(new String[0]), (d,which) -> {
                    String pick = opts.get(which);
                    if (pick.startsWith("Thanh toán")) payVnPay(id);
                }).show();
    }

    private void payVnPay(long id){
        String token = Session.getToken(this);
        if (token.isEmpty()) { toast("Hãy đăng nhập"); return; }

        new Thread(() -> {
            try {
                // gọi phát sinh link VNPay
                JSONObject res = ApiClient.postAuth("/customer/bookings/"+id+"/pay/vnpay",
                        new JSONObject(), token);
                String url = res.optString("payUrl", "");
                if (url.isEmpty()) { toast("Không tạo được link thanh toán"); return; }

                runOnUiThread(() -> {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                });
            } catch (Exception e) { toast("Lỗi thanh toán: "+e.getMessage()); }
        }).start();
    }

    // helpers
    private String safeName(JSONObject x){
        if (x == null) return "";
        String n = x.optString("name");
        if (!n.isEmpty()) return n;
        long id = x.optLong("id", -1);
        return id > 0 ? ("#"+id) : "";
    }
    private String pretty(String iso){
        // "yyyy-MM-ddTHH:mm:ss" -> "dd/MM HH:mm"
        try {
            String[] p = iso.split("T");
            String[] d = p[0].split("-");
            String hhmm = p[1].substring(0,5);
            return d[2]+"/"+d[1]+" "+hhmm;
        } catch (Exception e){ return iso; }
    }
    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
