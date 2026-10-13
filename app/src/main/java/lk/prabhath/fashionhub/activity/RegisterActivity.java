package lk.prabhath.fashionhub.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import lk.prabhath.fashionhub.R;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etPass;
    private FirebaseFirestore firestore; // Firebase instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Init Firebase
        firestore = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.et_reg_name);
        etEmail = findViewById(R.id.et_reg_email);
        etPhone = findViewById(R.id.et_reg_phone);
        etPass = findViewById(R.id.et_reg_pass);
        MaterialButton btnSignUp = findViewById(R.id.btn_signup);
        MaterialButton btnBackToLogin = findViewById(R.id.btn_back_to_login);

        // Back login
        btnBackToLogin.setOnClickListener(v -> finish());

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Signup click
        btnSignUp.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            } else {
                // Direct Firebase

                // Disable button
                btnSignUp.setEnabled(false);
                btnSignUp.setText("REGISTERING...");

                saveToFirebase(name, email, phone, pass, btnSignUp);
            }
        });
    }

    // Save Firebase
    private void saveToFirebase(String name, String email, String phone, String pass, MaterialButton btnSignUp) {

        // Create map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("password", pass);
        userMap.put("role", "customer");
        userMap.put("imageUrl", ""); // Empty image

        // Save doc
        firestore.collection("users")
                .document(email)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_LONG).show();
                    finish(); // Go login
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    android.util.Log.e("RegisterActivity", "Registration Failed", e);
                    Toast.makeText(this, "Registration Failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

                    // Enable button
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("SIGN UP");
                });
    }
}