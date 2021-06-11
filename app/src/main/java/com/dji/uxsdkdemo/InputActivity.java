package com.dji.uxsdkdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                addview();

            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commit();
            }
        });
    }

    private void addview() {
        TableRow tableRow = new TableRow(this);
        TextView textView = new TextView(this);
        EditText editText1 = new EditText(this);
        EditText editText2 = new EditText(this);

        editText1.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText2.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);

        String ids = String.format(id, i);
        String zb_js = String.format(zb_j, i);
        String zb_ws = String.format(zb_w, i);
        i++;

        textView.setText(ids);
        editText1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

            }
        });
        editText1.setHint(zb_js);
        editText2.setHint(zb_ws);

        tableRow.addView(textView);
        tableRow.addView(editText1);
        tableRow.addView(editText2);

        tableLayout.addView(tableRow);
    }

    private void commit() {
        Intent intent = new Intent();
        JSONObject root = new JSONObject();

        JSONArray zbarray = new JSONArray();
        //JSONObject code = new JSONObject();

        TableRow[] childs = new TableRow[tableLayout.getChildCount()];
        for (int i = 0; i < childs.length; i++) {
            childs[i] = (TableRow) tableLayout.getChildAt(i);
            View[] childss = new View[childs[i].getChildCount()];
            childss[0] = childs[i].getChildAt(1);
            childss[1] = childs[i].getChildAt(2);
            JSONObject jzb = new JSONObject();
            try {
                jzb.put("longitude", ((EditText) childss[0]).getText().toString());
                jzb.put("latitude", ((EditText) childss[1]).getText().toString());
                zbarray.put(jzb);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            root.put("data", zbarray);
            root.put("code", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        intent.putExtra("data", root.toString());
        setResult(2, intent);
        finish();
    }
}