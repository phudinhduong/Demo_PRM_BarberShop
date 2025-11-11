package barber.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barber.R;

import org.json.JSONObject;

import barber.ApiClient;

public class VerifyActivity extends AppCompatActivity {
    private String email;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_verify);

        email = getIntent().getStringExtra("email");
        TextView tvEmail = findViewById(R.id.tvEmail);
        tvStatus = findViewById(R.id.tvStatus);
        EditText etCode = findViewById(R.id.etCode);
        Button btnVerify = findViewById(R.id.btnVerify);
        Button btnResend = findViewById(R.id.btnResend);
        Button btnToLogin = findViewById(R.id.btnToLogin);

        tvEmail.setText("Email: " + email);

        btnVerify.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            tvStatus.setText("Đang xác thực...");
            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("email", email);
                    body.put("code", code);
                    JSONObject res = ApiClient.INSTANCE.postJson("/auth/verify-email", body);
                    runOnUiThread(() -> tvStatus.setText(res.optString("message", "Xác thực thành công!")));
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("Lỗi: " + e.getMessage()));
                }
            }).start();
        });

        btnResend.setOnClickListener(v -> {
            tvStatus.setText("Đang gửi lại OTP...");
            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("email", email);
                    JSONObject res = ApiClient.INSTANCE.postJson("/auth/resend-code", body);
                    runOnUiThread(() -> tvStatus.setText(res.optString("message", "Đã gửi lại OTP")));
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("Lỗi: " + e.getMessage()));
                }
            }).start();
        });

        btnToLogin.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("email", email);
            startActivity(i);
        });
    }
}