package com.example.trackmate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;

public class StartActivity extends AppCompatActivity {
    int i = 0;
    final int N = 19;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ImageView imageView = findViewById(R.id.ivIcon);
        int[] drawables = new int[N];
        drawables[0] = R.drawable.icon1;
        drawables[1] = R.drawable.icon2;
        drawables[2] = R.drawable.icon3;
        drawables[3] = R.drawable.icon4;
        drawables[4] = R.drawable.icon5;
        drawables[5] = R.drawable.icon6;
        drawables[6] = R.drawable.icon7;
        drawables[7] = R.drawable.icon8;
        drawables[8] = R.drawable.icon9;
        drawables[9] = R.drawable.icon10;
        drawables[10] = R.drawable.icon11;
        drawables[11] = R.drawable.icon12;
        drawables[12] = R.drawable.icon13;
        drawables[13] = R.drawable.icon14;
        drawables[14] = R.drawable.icon15;
        drawables[15] = R.drawable.icon16;
        drawables[16] = R.drawable.icon17;
        drawables[17] = R.drawable.icon18;
        drawables[18] = R.drawable.icon19;

        CountDownTimer countDownTimer = new CountDownTimer(5000,200) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (i < drawables.length) {
                    imageView.setImageResource(drawables[i]);
                    i++;
                }
            }
            @Override
            public void onFinish() {
                Intent go = new Intent(StartActivity.this, MainActivity.class);
                startActivity(go);
                finish();
            }
        };
        countDownTimer.start();
    }
}