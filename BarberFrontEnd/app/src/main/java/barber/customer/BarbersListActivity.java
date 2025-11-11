package barber.customer;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barber.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

import barber.ApiClient;

public class BarbersListActivity extends AppCompatActivity {
    private final ArrayList<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_barbers_list);

        ListView list = findViewById(R.id.listBarbers);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(adapter);

        list.setOnItemClickListener((p,v,pos,id) -> Toast.makeText(this, rows.get(pos), Toast.LENGTH_SHORT).show());

        load();
    }

    private void load() {
        new Thread(() -> {
            try {
                JSONArray arr = ApiClient.getArray("/barbers");
                rows.clear();
                for (int i=0;i<arr.length();i++){
                    JSONObject b = arr.getJSONObject(i);
                    if (!b.optBoolean("isActive", true)) continue;
                    long id = b.optLong("id");
                    String name = b.optString("name");
                    String phone = "";
                    JSONObject u = b.optJSONObject("user");
                    if (u != null) phone = u.optString("phone", "");
                    String line = "#" + id + " • " + name + (phone.isEmpty() ? "" : " • " + phone);
                    rows.add(line);
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) {
                toast("Lỗi load thợ: " + e.getMessage());
            }
        }).start();
    }

    private void toast(String m){ runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show()); }
}
