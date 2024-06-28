package com.example.trackmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView ivProfilePicture;
    private Uri imageUri;
    private EditText etNickname, etCurrentPassword, etNewPassword;
    private TextView tvUserName, tvPhoneNumber;
    private FirebaseAuth auth;
    private DatabaseReference refMe;
    private DatabaseReference refUserLocation;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        etNickname = findViewById(R.id.etNickname);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        tvUserName = findViewById(R.id.tvUserName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        Button btnChangePicture = findViewById(R.id.btnChangePicture);
        Button btnSaveChanges = findViewById(R.id.btnSaveChanges);
        Button btnLogout = findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        FirebaseUser currentUser = auth.getCurrentUser();
        refMe = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        refUserLocation = FirebaseDatabase.getInstance().getReference("UserLocations").child(currentUser.getUid());

        if (currentUser != null) {
            tvUserName.setText(currentUser.getDisplayName());
            tvPhoneNumber.setText(currentUser.getPhoneNumber());

            // Load the profile picture from Firebase Storage
            refMe.child("profilePictureUrl").get().addOnSuccessListener(dataSnapshot -> {
                String profilePictureUrl = dataSnapshot.getValue(String.class);
                if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                    Picasso.get().load(profilePictureUrl).into(ivProfilePicture);
                }
            });
        }

        btnChangePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        btnSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth.signOut();
                startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            // Check the file size before proceeding
            try {
                // Get the size of the file in bytes
                long fileSizeInBytes = getContentResolver().openInputStream(imageUri).available();
                // Convert bytes to megabytes (1 MB = 1048576 bytes)
                long fileSizeInMB = fileSizeInBytes / 1048576;

                // Check if file size exceeds 2 MB
                if (fileSizeInMB > 2) {
                    Toast.makeText(this, "Please select an image smaller than 2 MB", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Load the image into ImageView
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivProfilePicture.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateProfile() {
        String nickname = etNickname.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        FirebaseUser user = auth.getCurrentUser();

        if (imageUri != null) {
            StorageReference fileReference = storageRef.child("profile_pictures/" + user.getUid() + ".jpg");
            fileReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String profilePictureUrl = uri.toString();
                            refMe.child("profilePictureUrl").setValue(profilePictureUrl);
                            Toast.makeText(SettingsActivity.this, "Profile picture updated", Toast.LENGTH_SHORT).show();

                            // Broadcast the updated profile picture URL
                            Intent intent = new Intent("com.example.trackmate.PROFILE_PICTURE_UPDATED");
                            intent.putExtra("profilePictureUrl", profilePictureUrl);
                            sendBroadcast(intent);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SettingsActivity.this, "Failed to upload picture", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (!nickname.isEmpty()) {
            refMe.child("nickname").setValue(nickname).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(SettingsActivity.this, "Nickname updated", Toast.LENGTH_SHORT).show();
                    refUserLocation.child("NickName").setValue(nickname).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(SettingsActivity.this, "User location nickname updated", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(SettingsActivity.this, "Failed to update user location nickname", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SettingsActivity.this, "Failed to update nickname", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (!currentPassword.isEmpty() && !newPassword.isEmpty()) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    user.updatePassword(newPassword).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(SettingsActivity.this, "Password updated", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(SettingsActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SettingsActivity.this, "Re-authentication failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(SettingsActivity.this, MapActivity.class);
        startActivity(intent);
        finish(); // This will close the current activity
    }
}
