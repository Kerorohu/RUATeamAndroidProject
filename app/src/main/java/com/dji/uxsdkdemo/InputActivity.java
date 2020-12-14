package com.dji.uxsdkdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class InputActivity extends AppCompatActivity {
    private ConstraintLayout layout;
    private TableLayout tableLayout;
    //private TableRow tableRow;
    private String id, zb_j, zb_w;
    private int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);
        layout = findViewById(R.id.root_view);
        tableLayout = findViewById(R.id.table);
        id = getResources().getString(R.string.zb);
        zb_j = getResources().getString(R.string.zb_j);
        zb_w = getResources().getString(R.string.zb_w);
        i = 2;

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commit();
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addview();
            }
        });
    }

    private void addview() {
        TableRow tableRow = new TableRow(this);
        TextView textView = new TextView(this);
        EditText editText1 = new EditText(this);
        EditText editText2 = new EditText(this);

        String.format(id, i);
        String.format(zb_j, i);
        String.format(zb_w, i);
        i++;

        textView.setText(id);
        editText1.setText(zb_j);
        editText2.setText(zb_w);

        tableRow.addView(textView);
        tableRow.addView(editText1);
        tableRow.addView(editText2);

        tableLayout.addView(tableRow);
    }

    private void commit() {
        Intent intent = new Intent("android.intent.action.MAIN");
        startActivity(intent);
    }
}