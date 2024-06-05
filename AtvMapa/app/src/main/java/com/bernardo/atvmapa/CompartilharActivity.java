package com.bernardo.atvmapa;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompartilharActivity extends AppCompatActivity {

    private List<String> trilhas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compartilhar);

        Button shareButton = findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selecionarTrilhaParaCompartilhar();
            }
        });

        // Obter a lista de trilhas salvas
        trilhas = obterListaTrilhas();
    }

    // Método para obter a lista de trilhas salvas
    private List<String> obterListaTrilhas() {
        List<String> trilhas = new ArrayList<>();
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith("trilha_") && file.getName().endsWith(".txt")) {
                        // Extrair a data e hora do nome do arquivo
                        String dateTime = file.getName().replace("trilha_", "").replace(".txt", "");
                        trilhas.add(dateTime);
                    }
                }
            }
        }
        return trilhas;
    }

    // Método para selecionar uma trilha para compartilhar
    private void selecionarTrilhaParaCompartilhar() {
        if (trilhas.isEmpty()) {
            Toast.makeText(this, "Nenhuma trilha encontrada para compartilhar", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecione uma trilha para compartilhar");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice, trilhas);

        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String trilhaSelecionada = trilhas.get(which);
                compartilharTrilha(trilhaSelecionada);
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    // Método para compartilhar os detalhes da trilha selecionada
    private void compartilharTrilha(String trilha) {
        String trilhaFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/trilha_" + trilha + ".txt";
        StringBuilder detalhesTrilha = new StringBuilder();

        double latitudeAnterior = 0;
        double longitudeAnterior = 0;
        double latitudeInicio = 0;
        double longitudeInicio = 0;
        double latitudeFim = 0;
        double longitudeFim = 0;
        double distanciaTotal = 0.0;
        long tempoInicioTrilha = 0;
        long tempoFimTrilha = 0;
        int numPontos = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(trilhaFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    double latitude = Double.parseDouble(parts[0]);
                    double longitude = Double.parseDouble(parts[1]);
                    long timestamp = Long.parseLong(parts[2]);

                    numPontos++;
                    if (numPontos == 1) {
                        latitudeInicio = latitude;
                        longitudeInicio = longitude;
                        tempoInicioTrilha = timestamp;
                    } else {
                        latitudeFim = latitude;
                        longitudeFim = longitude;
                        tempoFimTrilha = timestamp;
                    }

                    if (numPontos > 1) {
                        distanciaTotal += calcularDistancia(latitudeAnterior, longitudeAnterior, latitude, longitude);
                    }
                    latitudeAnterior = latitude;
                    longitudeAnterior = longitude;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        long duracaoTrilha = tempoFimTrilha - tempoInicioTrilha;
        double velocidadeMedia = 0.0;
        if (duracaoTrilha > 0) {
            long duracaoTrilhaEmSegundos = duracaoTrilha / 1000;
            velocidadeMedia = (distanciaTotal / duracaoTrilhaEmSegundos); // metros por segundo
        }

        detalhesTrilha.append("Data e Hora: ").append(trilha).append("\n");
        detalhesTrilha.append("Ponto de Início: ").append(latitudeInicio).append(", ").append(longitudeInicio).append("\n");
        detalhesTrilha.append("Ponto de Chegada: ").append(latitudeFim).append(", ").append(longitudeFim).append("\n");
        detalhesTrilha.append("Distância Percorrida: ").append(String.format("%.2f", distanciaTotal)).append(" metros\n");
        detalhesTrilha.append("Duração da Trilha: ").append(formatElapsedTime(duracaoTrilha)).append("\n");
        detalhesTrilha.append("Velocidade Média: ").append(String.format("%.2f", velocidadeMedia)).append(" m/s");

        String conteudo = detalhesTrilha.toString();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, conteudo);

        startActivity(Intent.createChooser(intent, "Compartilhar Trilha"));
    }

    // Método auxiliar para calcular a distância entre dois pontos
    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Raio da Terra em km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convertendo para metros
    }

    // Método auxiliar para formatar o tempo decorrido em formato legível
    private String formatElapsedTime(long millis) {
        int seconds = (int) (millis / 1000);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
