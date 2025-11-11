package barber.barbers;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barber.R;

import java.time.LocalDate;

import barber.Session;

public class BarberHomeActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_barber_home);

        TextView tvRole = findViewById(R.id.tvRole);
        tvRole.setText("Xin chào, BARBER");

        // kiểm tra quyền
        String role = Session.getRole(this);
        if (!"BARBER".equalsIgnoreCase(role)) {
            Toast.makeText(this, "Bạn không phải BARBER", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Button btnToday = findViewById(R.id.btnToday);
        Button btnByDay = findViewById(R.id.btnByDay);

        btnToday.setOnClickListener(v -> {
            Intent i = new Intent(this, BarberScheduleActivity.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                i.putExtra("date", LocalDate.now().toString()); // yyyy-MM-dd
            }
            startActivity(i);
        });

        btnByDay.setOnClickListener(v -> {
            startActivity(new Intent(this, BarberScheduleByDayActivity.class));
        });
    }
}
