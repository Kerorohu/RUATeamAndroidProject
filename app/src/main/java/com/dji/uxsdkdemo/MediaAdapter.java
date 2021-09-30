/*
 * Copyright (c) 2021. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.dji.uxsdkdemo;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatCheckBox;

import java.util.ArrayList;
import java.util.List;

public class MediaAdapter extends BaseAdapter {
    List<String> data = new ArrayList<>();
    private Context mContext;
    ViewHolder holder;
    private boolean isShowCheckBox = true;//表示当前是否是多选状态。
    private SparseBooleanArray stateCheckedMap = new SparseBooleanArray();//用来存放CheckBox的选中状态，true为选中,false为没有选中

    public MediaAdapter(List<String> data, Context mContext, SparseBooleanArray stateCheckedMap) {
        this.data = data;
        this.mContext = mContext;
        this.stateCheckedMap = stateCheckedMap;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.pic_download_item, null);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.checkBox = (CheckBox) convertView.findViewById(R.id.chb_select_way_point);
        holder.mTvData = convertView.findViewById(R.id.tv_data);
        showAndHideCheckBox();//控制CheckBox的那个的框显示与隐藏

        holder.mTvData.setText(data.get(position));
        holder.checkBox.setChecked(stateCheckedMap.get(position));//设置CheckBox是否选中
        return convertView;
    }

    public class ViewHolder {
        public TextView mTvData;
        public CheckBox checkBox;
    }

    private void showAndHideCheckBox() {
        if (isShowCheckBox) {
            holder.checkBox.setVisibility(View.VISIBLE);
        } else {
            holder.checkBox.setVisibility(View.GONE);
        }
    }

    public boolean isShowCheckBox() {
        return isShowCheckBox;
    }

    public void setShowCheckBox(boolean showCheckBox) {
        isShowCheckBox = showCheckBox;
    }
}
