package com.jibunnoji.keyboard;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setPadding(50, 100, 50, 50);
        screen.setBackgroundColor(Color.rgb(255, 248, 235));

        TextView title = new TextView(this);
        title.setText("自分の字キーボード");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        screen.addView(title);

        TextView message = new TextView(this);
        message.setText("\nじぶんの字を、キーボードにしよう！\n");
        message.setTextSize(18);
        message.setGravity(Gravity.CENTER);
        screen.addView(message);

        Button startButton = new Button(this);
        startButton.setText("もじをかいてみよう");
        startButton.setTextSize(18);
        screen.addView(startButton);

        setContentView(screen);
    }
}
