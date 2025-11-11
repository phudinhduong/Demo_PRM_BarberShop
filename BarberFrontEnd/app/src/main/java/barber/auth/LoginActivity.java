package barber.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barber.R;
import org.json.JSONObject;

import barber.ApiClient;
import barber.Session;
import barber.barbers.BarberHomeActivity;
import barber.customer.CustomerHomeActivity;
import barber.owner.OwnerHomeActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        String email = getIntent().getStringExtra("email");
        if (email != null) etEmail.setText(email);

        btnLogin.setOnClickListener(v -> doLogin());

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void doLogin() {
        final String email = etEmail.getText().toString().trim();
        final String pass  = etPassword.getText().toString();
        tvStatus.setText("Đang đăng nhập...");

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", pass);

                JSONObject res = ApiClient.postJson("/auth/login", body);

                // lưu token & role
                Session.applyLoginResponse(this, res);
                String token = Session.getToken(this);

                runOnUiThread(() -> {
                    if (token.isEmpty()) {
                        tvStatus.setText("Đăng nhập thất bại");
                        return;
                    }
                    tvStatus.setText("Đăng nhập thành công");

                    String role = Session.getRole(this); // trả "OWNER" | "BARBER" | "CUSTOMER"
                    Class<?> target;
                    if ("OWNER".equalsIgnoreCase(role)) {
                        target = OwnerHomeActivity.class;
                    } else if ("BARBER".equalsIgnoreCase(role)) {
                        target = BarberHomeActivity.class; // tạo màn này nếu chưa có
                    } else {
                        target = CustomerHomeActivity.class;
                    }

                    Intent i = new Intent(this, target);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> tvStatus.setText("Lỗi: " + e.getMessage()));
            }
        }).start();
    }
}
