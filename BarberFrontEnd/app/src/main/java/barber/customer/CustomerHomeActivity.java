package barber.customer;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barber.R;

public class CustomerHomeActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_customer_home);

        findViewById(R.id.btnServices).setOnClickListener(v -> go(ServicesListActivity.class));
        findViewById(R.id.btnBarbers).setOnClickListener(v -> go(BarbersListActivity.class));
        findViewById(R.id.btnSchedule).setOnClickListener(v -> go(CustomerScheduleActivity.class));
        findViewById(R.id.btnBook).setOnClickListener(v -> go(CustomerBookingActivity.class));
        findViewById(R.id.btnMyBookings).setOnClickListener(v -> go(MyBookingsActivity.class));
    }

    private void go(Class<?> cls) {
        try { startActivity(new Intent(this, cls)); }
        catch (Exception e){ Toast.makeText(this, "Màn hình chưa triển khai", Toast.LENGTH_SHORT).show(); }
    }
}

