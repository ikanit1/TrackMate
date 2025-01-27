package com.example.trackmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.trackmate.adapter.SearchUserRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SearchActivity extends AppCompatActivity implements SearchUserRecyclerAdapter.OnImageClick {

    private RecyclerView recyclerView;
    private SearchUserRecyclerAdapter adapter;
    private EditText etFriendPhone;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        auth = FirebaseAuth.getInstance();

        // Обработка нажатий на кнопки навигации
        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, MapActivity.class);
            startActivity(intent);
        });
        ImageButton friendsButton = findViewById(R.id.friendsButton);
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, FriendsActivity.class);
            startActivity(intent);
            finish();
        });
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        recyclerView = findViewById(R.id.recyclerView);
        etFriendPhone = findViewById(R.id.etFriendPhone);
        Button searchButton = findViewById(R.id.searchButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseRecyclerOptions<Users> options =
                new FirebaseRecyclerOptions.Builder<Users>()
                        .setQuery(databaseReference, Users.class)
                        .build();

        adapter = new SearchUserRecyclerAdapter(options, this);
        adapter.setOnImageClick(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.GONE);

        searchButton.setOnClickListener(v -> {
            String phoneNumber = etFriendPhone.getText().toString().trim();
            Log.d("SearchActivity", "Search button clicked with text: " + phoneNumber);
            filter(phoneNumber);

            // Скрыть клавиатуру после поиска
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        });
    }

    // Метод для фильтрации пользователей по номеру телефона
    private void filter(String text) {
        adapter.stopListening();

        databaseReference.orderByChild("phone").equalTo(text).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    Log.d("SearchActivity", "No user found with this phone number");
                    Toast.makeText(SearchActivity.this, "No user found with this phone number", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SearchActivity", "Error fetching user data", databaseError.toException());
            }
        });

        FirebaseRecyclerOptions<Users> options =
                new FirebaseRecyclerOptions.Builder<Users>()
                        .setQuery(databaseReference.orderByChild("phone").equalTo(text), Users.class)
                        .build();

        adapter.updateOptions(options);

        adapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public void onImageClick(String nickName) {
        // Получение телефонного номера пользователя по никнейму
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        usersRef.orderByChild("nickname").equalTo(nickName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Users user = snapshot.getValue(Users.class);
                        String phone = user.getPhone();
                        Log.d("SearchActivity", "Found user phone: " + phone);

                        // Передача данных в UserActivity
                        Intent intent = new Intent(SearchActivity.this, UserActivity.class);
                        intent.putExtra("nickname", nickName);
                        intent.putExtra("phone", phone);
                        startActivity(intent);
                        return; // Выходим после нахождения первого совпадения
                    }
                } else {
                    Log.d("SearchActivity", "No user found with this nickname");
                    Toast.makeText(SearchActivity.this, "No user found with this nickname", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SearchActivity", "Error fetching user data", databaseError.toException());
            }
        });
    }
}
