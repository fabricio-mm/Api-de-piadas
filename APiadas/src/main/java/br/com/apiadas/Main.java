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
   // private static final List<WebhookSubscription> webhooks = new ArrayList<>();

    private static final AtomicInteger piadaId = new AtomicInteger(1);
    private static final AtomicInteger webhookId = new AtomicInteger(1);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args)
    {
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

            config.routes.get("/api/piadas", ctx -> {
               List<Piada> piadasAprovadas = piadas.stream()
                       .filter(p -> p.status.equals("APROVADA"))
                       .toList();
               ctx.json(piadasAprovadas);
            });

            config.routes.post("/api/piadas/sugestoes", ctx ->
            {
                CriarPiadaRequest request = ctx.bodyAsClass(CriarPiadaRequest.class);

                if(request.pergunta == null || request.pergunta.isBlank())
                {
                    ctx.status(400).json(Map.of("erro", "A pergunta da piada é obrigatória"));
                    return;
                }

                if (request.resposta == null || request.resposta.isBlank())
                {
                    ctx.status(400).json(Map.of("erro", "A resposta da piada é obrigatória"));
                    return;
                }

                Piada novaPiada = new Piada(
                        piadaId.getAndIncrement(),
                        request.pergunta,
                        request.resposta,
                        request.categoria == null || request.categoria.isBlank() ? "geral": request.categoria,
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

            config.routes.get("/api/admin/piadas/pendentes", ctx ->
            {
                List<Piada> piadasPendentes = piadas.stream()
                        .filter(p -> p.status.equals("PENDENTE"))
                        .toList();
                ctx.json(piadasPendentes);
            });

            config.routes.post("/api/admin/piadas/{id}/aprovar", ctx ->
            {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Piada piada = buscarPiadaPorId(id);

                if(piada == null){
                    ctx.status(404).json(Map.of("erro", "Piada não encontrada"));
                    return;
                }

                piada.status = "APROVADA";

                ctx.json(Map.of(
                        "message", "Piada aprovada",
                        "piada", piada
                ));
            });

            config.routes.post("/api/admin/piadas/{id}/rejeitar", ctx ->
            {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Piada piada = buscarPiadaPorId(id);

                if(piada == null)
                {
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

    private static Piada buscarPiadaPorId(int id){
        return piadas.stream()
                .filter(p -> p.id == id)
                .findFirst()
                .orElse(null);
    }

    public static class CriarPiadaRequest{
        public String pergunta;
        public String resposta;
        public String categoria;
        public String autor;

        public CriarPiadaRequest(){

        }
    }

    public static class Piada
    {
        public int id;
        public String pergunta;
        public String resposta;
        public String categoria;
        public String autor;
        public String status;
        public String criadaEm;

        public Piada(){}

        public Piada(int id, String pergunta,
                     String resposta, String categoria,
                     String autor, String status, String criadaEm)
        {
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