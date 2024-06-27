package com.example.trackmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etNickname, etPhone, etREmail, etRPassword;
    private Button btnRegister, btnBack;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        etNickname = findViewById(R.id.etNickname);
        etPhone = findViewById(R.id.etPhone);
        etREmail = findViewById(R.id.etREmail);
        etRPassword = findViewById(R.id.etRPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etREmail.getText().toString().trim();
                String pass = etRPassword.getText().toString().trim();
                String nickname = etNickname.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                ArrayList<String> friends = new ArrayList<>();

                if (email.isEmpty()) {
                    etREmail.setError("Email cannot be empty");
                    return;
                }
                if (pass.isEmpty()) {
                    etRPassword.setError("Password cannot be empty");
                    return;
                }
                if (nickname.isEmpty()) {
                    etNickname.setError("Nickname cannot be empty");
                    return;
                }
                if (phone.isEmpty()) {
                    etPhone.setError("Phone Number cannot be empty");
                    return;
                }

                // Создаем пользователя в Firebase Authentication
                auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Успешно зарегистрирован
                                    FirebaseUser firebaseUser = auth.getCurrentUser();
                                    if (firebaseUser != null) {
                                        // Получаем токен FCM
                                        getFCMToken(new TokenCallback() {
                                            @Override
                                            public void onTokenReceived(String token) {
                                                // Сохраняем пользователя в базу данных
                                                Users user = new Users(phone, email, pass, nickname, friends, token);
                                                usersRef.child(firebaseUser.getUid()).setValue(user)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    // Очищаем поля ввода
                                                                    etNickname.setText("");
                                                                    etREmail.setText("");
                                                                    etPhone.setText("");
                                                                    etRPassword.setText("");
                                                                    Toast.makeText(RegisterActivity.this, "SignUp Successful", Toast.LENGTH_SHORT).show();
                                                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                                } else {
                                                                    Toast.makeText(RegisterActivity.this, "Failed to save user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                                    }
                                } else {
                                    // Ошибка при регистрации
                                    Toast.makeText(RegisterActivity.this, "SignUp Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }


    private void getFCMToken(TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String token = task.getResult();
                            callback.onTokenReceived(token);
                        } else {
                            Log.e("FCM Token", "Failed to get token: " + task.getException().getMessage());
                            callback.onTokenReceived(null);
                        }
                    }
                });
    }


    private interface TokenCallback {
        void onTokenReceived(String token);
    }
}
