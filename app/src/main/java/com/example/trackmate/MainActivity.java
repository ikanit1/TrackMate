package com.example.trackmate;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private EditText etEmail, etPassword;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private DatabaseReference refMe, refUsers, refLocations;
    boolean uF = false, lF = false, meF = false;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if(uF && lF && meF){
                progressBar.setVisibility(View.GONE);
                timerHandler.removeCallbacks(timerRunnable);
                Quit();
            }
            else {
                timerHandler.postDelayed(this, 500);
            }
        }
    };

    private void Quit(){
        ArrayList<String> friends = Global.me.getFriends();
        if (friends != null) {
            Global.myFriendsLocation = new ArrayList<>();
            for (String friend : friends) {
                for (UserLocation location : Global.allLocations) {
                    if (location.getNickName().equals(friend)) {
                        Global.myFriendsLocation.add(location);
                        break;
                    }
                }
            }
        }
        Toast.makeText(MainActivity.this, "Login Successful" + Global.myFriendsLocation.size(), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, MapActivity.class));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        Button btnRegister = findViewById(R.id.btnRegister);
        auth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString();
                String pass = etPassword.getText().toString();

                if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (!pass.isEmpty()) {
                        auth.signInWithEmailAndPassword(email, pass)
                                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                    @Override
                                    public void onSuccess(AuthResult authResult) {
                                        retrieve(auth.getUid());
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        etPassword.setError("Empty fields are not allowed");
                    }
                } else if (email.isEmpty()) {
                    etEmail.setError("Empty fields are not allowed");
                } else {
                    etEmail.setError("Please enter correct email");
                }
            }
        });

        // Check if the user is already logged in
        if (auth.getCurrentUser() != null) {
            retrieve(auth.getCurrentUser().getUid());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void retrieve(String uid) {
        uF = lF = meF = false;
        progressBar.setVisibility(View.VISIBLE);
        timerHandler.postDelayed(timerRunnable, 0);
        refMe = FirebaseDatabase.getInstance().getReference("Users/" + uid);
        refMe.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users me = snapshot.getValue(Users.class);
                Global.me = me;
                meF = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        refUsers = FirebaseDatabase.getInstance().getReference("Users");
        refUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot: snapshot.getChildren()) {
                    Global.allUsers.add(postSnapshot.getValue(Users.class));
                }
                uF = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        refLocations = FirebaseDatabase.getInstance().getReference("Locations");
        refLocations.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot: snapshot.getChildren()) {
                    UserLocation c = postSnapshot.getValue(UserLocation.class);
                    if (c.getNickName() != null){
                        Global.allLocations.add(c);
                    }
                }
                lF = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}
