package barber.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.barber.R;

import org.json.JSONObject;

import barber.ApiClient;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPhone, etPassword;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String pass = etPassword.getText().toString();

        tvStatus.setText("Đang gửi OTP qua email...");

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("name", name);
                body.put("email", email);
                body.put("phone", phone);
                body.put("password", pass);

                JSONObject res = ApiClient.INSTANCE.postJson("/auth/register", body);

                System.out.println(res);

                runOnUiThread(() -> {
                    tvStatus.setText(res.optString("message", "Đã gửi OTP"));
                    Intent i = new Intent(this, VerifyActivity.class);
                    i.putExtra("email", email);
                    startActivity(i);
                });
            }  catch (java.net.SocketTimeoutException te) {
                runOnUiThread(() -> tvStatus.setText("Timeout: Server xử lý chậm (có thể do gửi email). Thử lại sau ít phút."));
            } catch (java.io.IOException ioe) {
                runOnUiThread(() -> tvStatus.setText("Lỗi mạng: " + ioe.getMessage()));
            } catch (Exception e) {
                runOnUiThread(() -> tvStatus.setText("Lỗi: " + e.getMessage()));
            }
        }).start();
    }
}
