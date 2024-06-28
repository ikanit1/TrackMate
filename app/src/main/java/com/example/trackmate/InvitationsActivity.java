package com.example.trackmate;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class InvitationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        TextView textView = findViewById(R.id.textView);
        textView.setText("This feature is under development. Check back soon for updates!");

        // Загрузка анимации из ресурсов
        Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink_animation);

        // Применение анимации к TextView
        textView.startAnimation(blinkAnimation);
    }
}
