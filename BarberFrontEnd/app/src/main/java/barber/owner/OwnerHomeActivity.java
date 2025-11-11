package barber.owner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barber.R;

public class OwnerHomeActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_home);

        Button btnSvc   = findViewById(R.id.btnManageServices);
        Button btnBar   = findViewById(R.id.btnManageBarbers);
        Button btnShift = findViewById(R.id.btnManageShifts);
        Button btnOff   = findViewById(R.id.btnManageTimeOffs);

        btnSvc.setOnClickListener(v -> startActivity(new Intent(this, ManageServicesActivity.class)));
        btnBar.setOnClickListener(v -> startActivity(new Intent(this, ManageBarbersActivity.class)));
        btnShift.setOnClickListener(v -> startActivity(new Intent(this, OwnerDayActivity.class)));
        btnOff.setOnClickListener(v -> startActivity(new Intent(this, ManageTimeOffsActivity.class)));
    }
}
