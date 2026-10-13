package lk.prabhath.fashionhub.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import lk.prabhath.fashionhub.R;


public class LoginActivity extends AppCompatActivity {


    private static final String SENDER_EMAIL = "prabathalgevatta@gmail.com";
    private static final String APP_PASSWORD = "nsfc oemi cbxb abhl";

    private String generatedOTP = "";
    private String resettingEmail = "";

    private TextInputEditText etEmail, etPass;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        db = FirebaseFirestore.getInstance();


        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String cachedEmail = prefs.getString("email", "");

        if (isLoggedIn) {
            if (!cachedEmail.isEmpty()) {
                db.collection("users").document(cachedEmail).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String status = documentSnapshot.getString("status");
                                if (status != null && "Suspended".equalsIgnoreCase(status.trim())) {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.clear();
                                    editor.apply();
                                    new AlertDialog.Builder(LoginActivity.this)
                                            .setTitle("Account Suspended")
                                            .setMessage("Your FashionHub account has been suspended by the administrator. You have been logged out.")
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton("OK", null)
                                            .show();
                                    return;
                                }
                            }
                            goToMainActivity();
                        })
                        .addOnFailureListener(e -> goToMainActivity());
                return;
            } else {
                goToMainActivity();
                return;
            }
        }

        etEmail = findViewById(R.id.et_login_user);
        etPass = findViewById(R.id.et_login_pass);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        MaterialButton btnRegister = findViewById(R.id.btn_register_new);
        MaterialButton btnForgotPassword = findViewById(R.id.btn_forgot_password);


        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });


        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUserOnline(email, pass);
            }
        });


        btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }


    private void loginUserOnline(String email, String pass) {
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();


        db.collection("users").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        String status = documentSnapshot.getString("status");
                        if (status != null && "Suspended".equalsIgnoreCase(status.trim())) {
                            new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle("Account Suspended")
                                    .setMessage("Your FashionHub account has been suspended by the administrator. Access is denied.\n\nPlease contact customer support for further assistance.")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }

                        String dbPassword = documentSnapshot.getString("password");
                        String userName = documentSnapshot.getString("name");

                        if (pass.equals(dbPassword)) {

                            SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("email", email);
                            editor.putString("username", userName);
                            editor.apply();

                            Toast.makeText(this, "Welcome " + userName + "!", Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        } else {
                            Toast.makeText(this, "Incorrect Password!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Login failed. Check internet connection.", Toast.LENGTH_SHORT).show());
    }

    private void goToMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }


    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your registered email address to receive an OTP.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Email Address");

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Send OTP", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) checkEmailAndSendOTP(email);
            else Toast.makeText(LoginActivity.this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void checkEmailAndSendOTP(String email) {
        Toast.makeText(this, "Checking email...", Toast.LENGTH_SHORT).show();
        db.collection("users").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        generatedOTP = String.format("%04d", new Random().nextInt(10000));
                        resettingEmail = email;
                        sendOTPEmail(email, generatedOTP);
                    } else {
                        Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Database error.", Toast.LENGTH_SHORT).show());
    }

    private void sendOTPEmail(String receiverEmail, String otp) {
        Toast.makeText(this, "Sending OTP to your email... Please wait.", Toast.LENGTH_LONG).show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiverEmail));
                message.setSubject("FashionHub - Your Password Reset OTP");
                message.setText("Hello,\n\nYour OTP for resetting the password is: " + otp + "\n\nPlease do not share this code with anyone.\n\nThank you,\nFashionHub Team");

                Transport.send(message);

                runOnUiThread(() -> {
                    Toast.makeText(this, "OTP Sent Successfully!", Toast.LENGTH_SHORT).show();
                    showOTPInputDialog();
                });

            } catch (MessagingException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to send email. Check your connection.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showOTPInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter OTP");
        builder.setMessage("Please enter the 4-digit OTP sent to " + resettingEmail);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("4-digit OTP");

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String enteredOTP = input.getText().toString().trim();
            if (enteredOTP.equals(generatedOTP)) showNewPasswordDialog();
            else Toast.makeText(LoginActivity.this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showNewPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set New Password");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("New Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPassword = input.getText().toString().trim();
            if (newPassword.length() >= 6) {
                db.collection("users").document(resettingEmail)
                        .update("password", newPassword)
                        .addOnSuccessListener(aVoid -> Toast.makeText(LoginActivity.this, "Password reset successful! You can login now.", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Failed to update password.", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(LoginActivity.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }
}