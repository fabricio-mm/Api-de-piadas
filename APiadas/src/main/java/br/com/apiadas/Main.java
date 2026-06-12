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
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static final List<Piada> piadas = new ArrayList<>();
    private static final List<WebhookSubscription> webhooks = new ArrayList<>();

    private static final AtomicInteger piadaId = new AtomicInteger(1);
    private static final AtomicInteger webhookId = new AtomicInteger(1);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static final Random rnd = new Random();

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Javalin app = Javalin.create(config ->
        {

            config.bundledPlugins.enableDevLogging();

            config.routes.before(ctx ->
            {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            });

            config.routes.options("/*", ctx -> ctx.status(204));

            config.routes.get("/health", ctx ->
            {
                ctx.json(Map.of(
                        "status", "ok",
                        "app", "APiadas",
                        "message", "API rodando"
                ));
            });

            config.routes.get("/api/piadas/aleatoria", ctx ->
            {
                List<Piada> piadasAprovadas = piadas.stream()
                        .filter(p -> p.status.equals("APROVADA"))
                        .toList();
                if (piadasAprovadas.isEmpty()) {
                    ctx.status(404);
                    ctx.json(Map.of("erro", "Nenhuma piada aprovada encontrada"));
                    return;
                }
                ctx.json(piadasAprovadas.get(rnd.nextInt(piadasAprovadas.size())));
            });

            config.routes.post("/api/piadas/sugestoes", ctx ->
            {
                CriarPiadaRequest request = ctx.bodyAsClass(CriarPiadaRequest.class);

                if (request.pergunta == null || request.pergunta.isBlank()) {
                    ctx.status(400).json(Map.of("erro", "A pergunta da piada é obrigatória"));
                    return;
                }

                if (request.resposta == null || request.resposta.isBlank()) {
                    ctx.status(400).json(Map.of("erro", "A resposta da piada é obrigatória"));
                    return;
                }

                Piada novaPiada = new Piada(
                        piadaId.getAndIncrement(),
                        request.pergunta,
                        request.resposta,
                        request.categoria == null || request.categoria.isBlank() ? "geral" : request.categoria,
                        request.autor == null || request.autor.isBlank() ? "Anônimo" : request.autor,
                        "PENDENTE",
                        Instant.now().toString()
                );

                piadas.add(novaPiada);

                ctx.status(201).json(Map.of(
                        "message", "Piada enviada para aprovação",
                        "piada", novaPiada
                ));
            });

            config.routes.post("/api/webhooks/subscriptions", ctx ->
            {

                CriarWebhookRequest webhookRequest = ctx.bodyAsClass(CriarWebhookRequest.class);

                if (webhookRequest.url == null || webhookRequest.url.isBlank()) {
                    ctx.status(400).json(Map.of("erro", "A URL é obrigatória."));
                    return;
                }

                WebhookSubscription novoWebhook = new WebhookSubscription(
                        webhookId.getAndIncrement(),
                        webhookRequest.url,
                        webhookRequest.event == null || webhookRequest.event.isBlank() ? "piada.aprovada" : webhookRequest.event,
                        true
                );

                webhooks.add(novoWebhook);

                ctx.status(201).json(Map.of(
                        "message", "Webhook cadastrado com sucesso",
                        "webhook", novoWebhook
                ));
            });

            config.routes.get("/api/webhooks/subscriptions", ctx -> {
                ctx.json(webhooks);
            });


            config.routes.get("/api/admin/piadas/pendentes", ctx ->
            {
                List<Piada> piadasPendentes = piadas.stream()
                        .filter(p -> p.status.equals("PENDENTE"))
                        .toList();
                ctx.json(piadasPendentes);
            });

            config.routes.get("/api/piadas", ctx -> {
                List<Piada> piadasAprovadas = piadas.stream()
                        .filter(p -> p.status.equals("APROVADA"))
                        .toList();
                ctx.json(piadasAprovadas);
            });

            config.routes.post("/api/admin/piadas/{id}/aprovar", ctx ->
            {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Piada piada = buscarPiadaPorId(id);

                if (piada == null) {
                    ctx.status(404).json(Map.of("erro", "Piada não encontrada"));
                    return;
                }

                piada.status = "APROVADA";

                dispararWebhookPiadaAprovada(piada);

                ctx.json(Map.of(
                        "message", "Piada aprovada",
                        "piada", piada
                ));
            });

            config.routes.post("/api/admin/piadas/{id}/rejeitar", ctx ->
            {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Piada piada = buscarPiadaPorId(id);

                if (piada == null) {
                    ctx.status(404).json(Map.of("erro", "Piada não encontrada"));
                    return;
                }

                piada.status = "REJEITADA";

                ctx.json(Map.of(
                        "message", "Piada rejeitada",
                        "piada", piada
                ));
            });

        }).start(port);
    }

    public static class WebhookSubscription {
        public int id;
        public String url;
        public String event;
        public boolean ativo;

        public WebhookSubscription() {
        }

        public WebhookSubscription(int id, String url, String event, boolean ativo) {
            this.id = id;
            this.url = url;
            this.event = event;
            this.ativo = ativo;
        }
    }

    public static class CriarWebhookRequest {
        public String url;
        public String event;

        public CriarWebhookRequest() {
        }
    }

    private static Piada buscarPiadaPorId(int id) {
        return piadas.stream()
                .filter(p -> p.id == id)
                .findFirst()
                .orElse(null);
    }

    private static void dispararWebhookPiadaAprovada(Piada piada) {
        List<WebhookSubscription> webhooksCadastrados = webhooks.stream()
                .filter(w -> w.ativo)
                .filter(w -> w.event.equals("piada.aprovada"))
                .toList();

        if (webhooksCadastrados.isEmpty()) {
            System.out.println("Nenhum webhook cadastrado para piada.aprovada");
            return;
        }

        Map<String, Object> payload = Map.of(
                "event", "piada.aprovada",
                "createdAt", Instant.now().toString(),
                "data", piada
        );

        try {
            String json = mapper.writeValueAsString(payload);

            for (WebhookSubscription webhook : webhooksCadastrados) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhook.url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            System.out.println("Webhook enviado para: " + webhook.url);
                            System.out.println("Status recebido: " + response.statusCode());
                            System.out.println("Resposta: " + response.body());
                        })
                        .exceptionally(error -> {
                            System.out.println("Erro ao enviar webhook para: " + webhook.url);
                            System.out.println(error.getMessage());
                            return null;
                        });
            }
        } catch (Exception e) {
            System.out.println("Erro ao montar payload do webhook");
            System.out.println(e.getMessage());
        }

    }

    public static class CriarPiadaRequest {
        public String pergunta;
        public String resposta;
        public String categoria;
        public String autor;

        public CriarPiadaRequest() {

        }
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

        public Piada(int id, String pergunta,
                     String resposta, String categoria,
                     String autor, String status, String criadaEm) {
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