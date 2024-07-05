package com.example.trackmate;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView ivProfilePicture;
    private Uri imageUri;
    private EditText etNickname, etCurrentPassword, etNewPassword;
    private TextView tvUserName, tvPhoneNumber;
    private FirebaseAuth auth;
    private DatabaseReference refMe;
    private DatabaseReference refLocations;
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
        refLocations = FirebaseDatabase.getInstance().getReference("Locations");

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

        btnChangePicture.setOnClickListener(v -> openFileChooser());

        btnSaveChanges.setOnClickListener(v -> updateProfile());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            finish();
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
                long fileSizeInBytes = getContentResolver().openInputStream(imageUri).available();
                long fileSizeInMB = fileSizeInBytes / 1048576;

                if (fileSizeInMB > 2) {
                    Toast.makeText(this, "Please select an image smaller than 2 MB", Toast.LENGTH_SHORT).show();
                    return;
                }

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivProfilePicture.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateProfile() {
        String newNickname = etNickname.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        FirebaseUser user = auth.getCurrentUser();

        if (imageUri != null) {
            StorageReference fileReference = storageRef.child("profile_pictures/" + user.getUid() + ".jpg");
            fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String profilePictureUrl = uri.toString();
                        refMe.child("profilePictureUrl").setValue(profilePictureUrl);
                        Toast.makeText(SettingsActivity.this, "Profile picture updated", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent("com.example.trackmate.PROFILE_PICTURE_UPDATED");
                        intent.putExtra("profilePictureUrl", profilePictureUrl);
                        sendBroadcast(intent);
                    })
            ).addOnFailureListener(e ->
                    Toast.makeText(SettingsActivity.this, "Failed to upload picture", Toast.LENGTH_SHORT).show()
            );
        }

        if (!newNickname.isEmpty()) {
            AtomicReference<String> oldNickname = new AtomicReference<>("");

            refMe.child("nickname").get().addOnSuccessListener(dataSnapshot -> {
                oldNickname.set(dataSnapshot.getValue(String.class));

                // Update nickname in Users
                refMe.child("nickname").setValue(newNickname).addOnSuccessListener(aVoid -> {
                    Toast.makeText(SettingsActivity.this, "Nickname updated", Toast.LENGTH_SHORT).show();

                    // Update keys in Locations
                    refLocations.child(oldNickname.get()).get().addOnSuccessListener(dataSnapshot1 -> {
                        if (dataSnapshot1.exists()) {
                            Map<String, Object> locationData = (Map<String, Object>) dataSnapshot1.getValue();

                            refLocations.child(oldNickname.get()).removeValue().addOnSuccessListener(aVoid1 ->
                                    refLocations.child(newNickname).setValue(locationData).addOnSuccessListener(aVoid2 ->
                                            Toast.makeText(SettingsActivity.this, "Location updated", Toast.LENGTH_SHORT).show()
                                    ).addOnFailureListener(e ->
                                            Toast.makeText(SettingsActivity.this, "Failed to update location", Toast.LENGTH_SHORT).show()
                                    )
                            ).addOnFailureListener(e ->
                                    Toast.makeText(SettingsActivity.this, "Failed to remove old location", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });

                    // Update friends' nicknames
                    FirebaseDatabase.getInstance().getReference("Users").get().addOnSuccessListener(dataSnapshot2 -> {
                        for (DataSnapshot snapshot : dataSnapshot2.getChildren()) {
                            DatabaseReference userRef = snapshot.getRef();
                            userRef.child("friends").get().addOnSuccessListener(friendsSnapshot -> {
                                for (DataSnapshot friendSnapshot : friendsSnapshot.getChildren()) {
                                    if (friendSnapshot.getValue(String.class).equals(oldNickname.get())) {
                                        friendSnapshot.getRef().setValue(newNickname);
                                    }
                                }
                            });
                        }
                    });

                }).addOnFailureListener(e ->
                        Toast.makeText(SettingsActivity.this, "Failed to update nickname", Toast.LENGTH_SHORT).show()
                );

            }).addOnFailureListener(e ->
                    Toast.makeText(SettingsActivity.this, "Failed to get old nickname", Toast.LENGTH_SHORT).show()
            );
        }

        if (!currentPassword.isEmpty() && !newPassword.isEmpty()) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnSuccessListener(aVoid ->
                    user.updatePassword(newPassword).addOnSuccessListener(aVoid1 ->
                            Toast.makeText(SettingsActivity.this, "Password updated", Toast.LENGTH_SHORT).show()
                    ).addOnFailureListener(e ->
                            Toast.makeText(SettingsActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show()
                    )
            ).addOnFailureListener(e ->
                    Toast.makeText(SettingsActivity.this, "Re-authentication failed", Toast.LENGTH_SHORT).show()
            );
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(SettingsActivity.this, MapActivity.class);
        startActivity(intent);
        finish();
    }
}
