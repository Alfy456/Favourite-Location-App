package com.dev.fa_alfygeorge_c0836170_android.utils;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefHelper {

    private static final String PREF_NAME = "SharedPref";
    private static SharedPrefHelper ourInstance;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final boolean IS_UPDATE = false;
    private final boolean IS_NEW = false;

    private SharedPrefHelper(Context context) {

        prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = prefs.edit();
        editor.apply();
    }

    public static SharedPrefHelper getInstance(Context context) {

        if (ourInstance == null) {
            ourInstance = new SharedPrefHelper(context);
        }
        return ourInstance;
    }
    public boolean getBolIsUpdate() {
        return prefs.getBoolean(String.valueOf(IS_UPDATE), false);
    }

    public void setBolIsUpdate(boolean isAutoStart) {
        editor.putBoolean(String.valueOf(IS_UPDATE), isAutoStart);
        editor.commit();
    }

    public boolean getBolIsNew() {
        return prefs.getBoolean(String.valueOf(IS_NEW), false);
    }

    public void setBolIsNew(boolean isAutoStart) {
        editor.putBoolean(String.valueOf(IS_NEW), isAutoStart);
        editor.commit();
    }

}
