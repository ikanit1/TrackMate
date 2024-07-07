package com.example.trackmate;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private FirebaseAuth auth;
    private EditText etNickname, etPhone, etREmail, etRPassword;
    private ImageView ivProfilePicture;
    private Button btnRegister, btnBack, btnChoosePicture;
    private DatabaseReference usersRef;
    private StorageReference storageRef;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        storageRef = FirebaseStorage.getInstance().getReference();

        // Инициализация элементов UI
        etNickname = findViewById(R.id.etNickname);
        etPhone = findViewById(R.id.etPhone);
        etREmail = findViewById(R.id.etREmail);
        etRPassword = findViewById(R.id.etRPassword);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
        btnChoosePicture = findViewById(R.id.btnChoosePicture);

        // Кнопка назад
        btnBack.setOnClickListener(v -> finish());

        // Кнопка для выбора фотографии
        btnChoosePicture.setOnClickListener(v -> openFileChooser());

        // Кнопка для регистрации
        btnRegister.setOnClickListener(v -> {
            String email = etREmail.getText().toString().trim();
            String pass = etRPassword.getText().toString().trim();
            String nickname = etNickname.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            ArrayList<String> friends = new ArrayList<>();

            // Проверка полей на заполненность
            if (email.isEmpty()) {
                etREmail.setError("Email не может быть пустым");
                return;
            }
            if (pass.isEmpty()) {
                etRPassword.setError("Пароль не может быть пустым");
                return;
            }
            if (nickname.isEmpty()) {
                etNickname.setError("Никнейм не может быть пустым");
                return;
            }
            if (phone.isEmpty()) {
                etPhone.setError("Номер телефона не может быть пустым");
                return;
            }

            // Загрузка изображения и регистрация
            uploadImageAndRegister(email, pass, nickname, phone, friends);
        });
    }

    // Открытие файлового диалога для выбора изображения
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivProfilePicture.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Загрузка изображения и регистрация пользователя
    private void uploadImageAndRegister(String email, String password, String nickname, String phone, ArrayList<String> friends) {
        if (imageUri != null) {
            // Регистрация пользователя в Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    // Загрузка изображения в Firebase Storage
                                    StorageReference fileReference = storageRef.child("profile_pictures/" + user.getUid() + ".jpg");
                                    fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                                            fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                                String profilePictureUrl = uri.toString();
                                                // Регистрация пользователя и сохранение данных
                                                registerUser(email, password, nickname, phone, friends, profilePictureUrl);
                                            })
                                    ).addOnFailureListener(e -> {
                                        Toast.makeText(RegisterActivity.this, "Image upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } else {
                                Toast.makeText(RegisterActivity.this, "Registration error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(this, "The photo is not selected", Toast.LENGTH_SHORT).show();
        }
    }

    // Сохранение данных пользователя в Firebase Realtime Database
    private void registerUser(String email, String password, String nickname, String phone, ArrayList<String> friends, String profilePictureUrl) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            getFCMToken(token -> {
                Users user = new Users(phone, email, password, nickname, friends, token, profilePictureUrl);
                usersRef.child(firebaseUser.getUid()).setValue(user)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // Очистка полей и переход на главную страницу
                                    etNickname.setText("");
                                    etREmail.setText("");
                                    etPhone.setText("");
                                    etRPassword.setText("");
                                    ivProfilePicture.setImageResource(R.drawable.user_pic);
                                    Toast.makeText(RegisterActivity.this, "Registration is successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Error saving user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            });
        }
    }

    // Получение FCM токена
    private void getFCMToken(TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String token = task.getResult();
                            callback.onTokenReceived(token);
                        } else {
                            Log.e("FCM Token", "Failed to get a token: " + task.getException().getMessage());
                            callback.onTokenReceived(null);
                        }
                    }
                });
    }

    // Интерфейс для получения FCM токена
    private interface TokenCallback {
        void onTokenReceived(String token);
    }
}
