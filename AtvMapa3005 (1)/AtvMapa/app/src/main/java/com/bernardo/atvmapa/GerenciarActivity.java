package com.bernardo.atvmapa;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GerenciarActivity extends AppCompatActivity {

    private ListView listViewTrilhas;
    private List<String> trilhas;

    private double latitudeAnterior;
    private double longitudeAnterior;

    private long tempoInicioTrilha;
    private long tempoFimTrilha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar);

        listViewTrilhas = findViewById(R.id.list_view_trilhas);

        // Verificar permissões
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // Obter a lista de trilhas salvas
        trilhas = obterListaTrilhas();

        // Exibir a lista de trilhas no ListView
        exibirListaTrilhas(trilhas);

        // Configurar o listener de clique para os itens da lista
        listViewTrilhas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mostrarDetalhesTrilha(position);
            }
        });
    }

    // Método para obter a lista de trilhas salvas
    private List<String> obterListaTrilhas() {
        List<String> trilhas = new ArrayList<>();
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("trilha_") && file.getName().endsWith(".txt")) {
                    // Extrair a data e hora do nome do arquivo
                    String dateTime = file.getName().replace("trilha_", "").replace(".txt", "");
                    trilhas.add("Trilha: " + dateTime);
                }
            }
            // Classificar as trilhas por data e hora
            Collections.sort(trilhas);
        }
        return trilhas;
    }

    // Método para exibir a lista de trilhas no ListView
    private void exibirListaTrilhas(List<String> trilhas) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, trilhas);
        listViewTrilhas.setAdapter(adapter);
    }

    private void mostrarDetalhesTrilha(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Detalhes da Trilha");

        // Extrair a data e hora da string do item da lista
        String dateTime = trilhas.get(position).substring(8);

        // Ler o arquivo de trilha correspondente e extrair informações adicionais
        String trilhaFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/trilha_" + dateTime.replace(" ", "_").replace(":", "_") + ".txt";
        StringBuilder detalhesTrilha = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(trilhaFilePath))) {
            String line;
            double distanciaTotal = 0.0;
            int numPontos = 0;
            long primeiroTimestamp = 0;
            long ultimoTimestamp = 0;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) { // latitude, longitude, timestamp
                    double latitude = Double.parseDouble(parts[0]);
                    double longitude = Double.parseDouble(parts[1]);
                    long timestamp = Long.parseLong(parts[2]);
                    numPontos++;
                    if (numPontos == 1) {
                        // Registre o ponto de início da trilha
                        latitudeAnterior = latitude;
                        longitudeAnterior = longitude;
                        primeiroTimestamp = timestamp;
                    } else {
                        // Calcular a distância entre o ponto anterior e o ponto atual
                        distanciaTotal += calcularDistancia(latitudeAnterior, longitudeAnterior, latitude, longitude);
                        latitudeAnterior = latitude;
                        longitudeAnterior = longitude;
                    }
                    // Atualize o tempo de fim para o tempo do ponto atual
                    ultimoTimestamp = timestamp;
                }
            }
            // Calcular a duração da trilha
            long duracaoTrilha = ultimoTimestamp - primeiroTimestamp;
            long duracaoTrilhaSegundos = duracaoTrilha / 1000;

            // Calcular a velocidade média
            double velocidadeMedia = 0.0;
            if (duracaoTrilhaSegundos > 0) {
                velocidadeMedia = distanciaTotal / duracaoTrilhaSegundos; // metros por segundo
            }

            // Configurar o texto do AlertDialog com as informações da trilha
            detalhesTrilha.append("Data e Hora: ").append(dateTime).append("\n");
            detalhesTrilha.append("Distância Percorrida: ").append(String.format("%.2f", distanciaTotal)).append(" metros\n");
            detalhesTrilha.append("Duração da trilha: ").append(formatElapsedTime(duracaoTrilha)).append("\n");
            detalhesTrilha.append("Velocidade Média: ").append(String.format("%.2f", velocidadeMedia)).append(" m/s");
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }

        // Configurar botões para excluir ou cancelar
        builder.setMessage(detalhesTrilha.toString());
        builder.setPositiveButton("Excluir", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                excluirTrilha(position);
            }
        });
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Método auxiliar para formatar o tempo decorrido em formato legível
    private String formatElapsedTime(long millis) {
        int seconds = (int) (millis / 1000);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

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

    // Método para excluir a trilha selecionada
    private void excluirTrilha(int position) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("trilha_") && file.getName().endsWith(".txt")) {
                    String dateTime = file.getName().replace("trilha_", "").replace(".txt", "");
                    if (("Trilha: " + dateTime).equals(trilhas.get(position))) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            Toast.makeText(this, "Trilha excluída com sucesso", Toast.LENGTH_SHORT).show();
                            trilhas.remove(position);
                            exibirListaTrilhas(trilhas);
                        } else {
                            Toast.makeText(this, "Erro ao excluir a trilha", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                }
            }
        }
    }
}
