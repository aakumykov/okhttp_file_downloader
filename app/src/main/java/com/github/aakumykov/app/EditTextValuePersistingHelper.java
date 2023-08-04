package com.github.aakumykov.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EditTextValuePersistingHelper {

    private final SharedPreferences mSharedPreferences;
    private final Map<EditText, TextChangeWatcher> mKeyToInputMap = new HashMap<>();

    public EditTextValuePersistingHelper(@NonNull Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public void addFieldToPersistText(@NonNull final String key, final @NonNull EditText editText) {

        if (mKeyToInputMap.containsKey(editText))
            throw new IllegalStateException("This EditText is already served by this class.");

        TextChangeWatcher textChangeWatcher = new TextChangeWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mSharedPreferences.edit().putString(key, s.toString()).apply();
            }
        };

        mKeyToInputMap.put(editText, textChangeWatcher);
    }


    public void removeFieldFromTextPersisting(final @NonNull EditText editText, @NonNull final String key) {
        if (mKeyToInputMap.containsKey(editText)) {
            editText.removeTextChangedListener(mKeyToInputMap.get(editText));
            mSharedPreferences.edit().remove(key).apply();
        }
    }

    @Nullable
    public String getText(String key) {
        return mSharedPreferences.getString(key, null);
    }


    private static abstract class TextChangeWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
    }
}
