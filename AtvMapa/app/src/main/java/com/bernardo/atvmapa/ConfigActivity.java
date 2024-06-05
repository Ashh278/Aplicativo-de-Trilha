package com.bernardo.atvmapa;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

public class ConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_actvity);

        // Carregar as configurações salvas
        loadSavedSettings();
    }

    public void onClickSalvar(View view){
        RadioGroup radioGroup1 = findViewById(R.id.radioGroup1);
        RadioGroup radioGroup2 = findViewById(R.id.radioGroup2);
        RadioGroup radioGroup3 = findViewById(R.id.radioGroup3);
        RadioGroup radioGroup4 = findViewById(R.id.radioGroup4);

        // Salvar configurações
        saveSettings("radioGroup1", radioGroup1);
        saveSettings("radioGroup2", radioGroup2);
        saveSettings("radioGroup3", radioGroup3);
        saveSettings("radioGroup4", radioGroup4);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show();
    }

    private void saveSettings(String groupName, RadioGroup radioGroup) {
        SharedPreferences sharedPreferences = getSharedPreferences(groupName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        editor.putInt("select_radio", selectedRadioButtonId);
        editor.apply();
    }

    private void loadSavedSettings() {
        loadSavedSettingsForGroup("radioGroup1", R.id.radioGroup1);
        loadSavedSettingsForGroup("radioGroup2", R.id.radioGroup2);
        loadSavedSettingsForGroup("radioGroup3", R.id.radioGroup3);
        loadSavedSettingsForGroup("radioGroup4", R.id.radioGroup4);
    }

    private void loadSavedSettingsForGroup(String groupName, int radioGroupId) {
        SharedPreferences sharedPreferences = getSharedPreferences(groupName, Context.MODE_PRIVATE);
        int selectedRadioButtonId = sharedPreferences.getInt("select_radio", -1);
        if (selectedRadioButtonId != -1) {
            RadioGroup radioGroup = findViewById(radioGroupId);
            radioGroup.check(selectedRadioButtonId);
        }
    }
}
