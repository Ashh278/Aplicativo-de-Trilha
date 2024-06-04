package com.bernardo.atvmapa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // Método de clique para abrir a atividade do mapa
    public void onClickMap(View view){
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    // Método de clique para abrir a atividade de registro de trilha
    public void onClickRegistro(View view){
        Intent intent = new Intent(this, RegistroActivity.class);
        startActivity(intent);
    }

    // Método de clique para abrir a atividade de gerenciamento de trilha
    public void onClickGerenciar(View view){
        Intent intent = new Intent(this, GerenciarActivity.class);
        startActivity(intent);
    }

    // Método de clique para abrir a atividade de configurações
    public void onClickConfig(View view) {
        Intent intent = new Intent(this, ConfigActivity.class);
        startActivity(intent);
    }

    // Método de clique para abrir a atividade "Sobre"
    public void onClickSobre(View view){
        Intent intent = new Intent(this, SobreActivity.class);
        startActivity(intent);
    }

    // Método de clique para compartilhar a trilha
    public void onClickCompartilhar(View view){
        Intent intent = new Intent(this, CompartilharActivity.class);
        startActivity(intent);

    }
}
