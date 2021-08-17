package com.dji.uxsdkdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;
import java.util.Set;

public class ProperTies {
    private static SharedPreferences share;
    private static String configPath = "appSetting";

    // 读取配置文件信息
    public static SharedPreferences getProperties(Context context) {
        try {
            share = context.getSharedPreferences(configPath, Context.MODE_PRIVATE);
        } catch (Exception e) {
            e.printStackTrace();

        }

        return share;
    }

    // 修改配置文件信息
    public static String setPropertiesMap(Context context, Map<String, String> maps) {
        try {
            share = context.getSharedPreferences(configPath, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = share.edit();//取得编辑器
            Set<Map.Entry<String, String>> set = maps.entrySet();
            // 遍历键值对对象的集合，得到每一个键值对对象
            for (Map.Entry<String, String> me : set) {
                // 根据键值对对象获取键和值
                String key = me.getKey();
                String value = me.getValue();
                editor.putString(key, value);//存储配置 参数1 是key 参数2 是值
            }
            editor.commit();//提交刷新数据


        } catch (Exception e) {
            e.printStackTrace();
            Log.e("setPropertiesError", e.toString());
            return "修改配置文件失败!";
        }
        return "设置成功";
    }
}
