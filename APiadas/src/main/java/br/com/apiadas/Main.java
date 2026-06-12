package br.com.apiadas;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static final List<Piada> piadas = new ArrayList<>();
    private static final List<WebhookSubscription> webhooks = new ArrayList<>();

    private static final AtomicInteger piadaId = new AtomicInteger(1);
    private static final AtomicInteger webhookId = new AtomicInteger(1);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Javalin app = Javalin.create(config -> {

            config.bundledPlugins.enableDevLogging();

            config.routes.before(ctx -> {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            });

            config.routes.options("/*", ctx -> ctx.status(204));

            config.routes.get("/health", ctx -> {
                ctx.json(Map.of(
                        "status", "ok",
                        "app", "APiadas",
                        "message", "API rodando"
                ));
            });

    }


    public static class Piada {
        public int id;
        public String pergunta;
        public String resposta;
        public String categoria;
        public String autor;
        public String status;
        public String criadaEm;

        public Piada() {
        }

        public Piada(int id, String pergunta, String resposta, String categoria, String autor, String status, String criadaEm) {
            this.id = id;
            this.pergunta = pergunta;
            this.resposta = resposta;
            this.categoria = categoria;
            this.autor = autor;
            this.status = status;
            this.criadaEm = criadaEm;
        }
    }
}