const API_BASE_URL = "https://apiadas-backend.onrender.com";;

const listaPiadas = document.getElementById("listaPiadas");
const listaPendentes = document.getElementById("listaPendentes");
const formPiada = document.getElementById("formPiada");
const mensagemEnvio = document.getElementById("mensagemEnvio");
const piadaAleatoriaBox = document.getElementById("piadaAleatoriaBox");

const btnCarregarPiadas = document.getElementById("btnCarregarPiadas");
const btnPiadaAleatoria = document.getElementById("btnPiadaAleatoria");
const btnCarregarPendentes = document.getElementById("btnCarregarPendentes");

btnCarregarPiadas.addEventListener("click", carregarPiadasAprovadas);
btnPiadaAleatoria.addEventListener("click", carregarPiadaAleatoria);
btnCarregarPendentes.addEventListener("click", carregarPiadasPendentes);
formPiada.addEventListener("submit", enviarPiada);

window.addEventListener("load", () => {
    carregarPiadasAprovadas();
});

async function carregarPiadasAprovadas() {
    try {
        listaPiadas.innerHTML = criarMensagemCarregando("Carregando piadas aprovadas...");

        const response = await fetch(`${API_BASE_URL}/api/piadas`);
        const piadas = await response.json();

        if (!response.ok) {
            throw new Error("Erro ao carregar piadas aprovadas.");
        }

        if (piadas.length === 0) {
            listaPiadas.innerHTML = criarMensagemVazia("Nenhuma piada aprovada ainda.");
            return;
        }

        listaPiadas.innerHTML = piadas.map(criarCardPiada).join("");
    } catch (error) {
        listaPiadas.innerHTML = criarMensagemErro(error.message);
    }
}

async function carregarPiadaAleatoria() {
    try {
        piadaAleatoriaBox.innerHTML = `
            <span class="tag">Piada aleatória</span>
            <h3>Buscando...</h3>
            <p>Consultando a APiadas.</p>
        `;

        const response = await fetch(`${API_BASE_URL}/api/piadas/aleatoria`);
        const data = await response.json();

        if (!response.ok) {
            piadaAleatoriaBox.innerHTML = `
                <span class="tag">Piada aleatória</span>
                <h3>Nenhuma piada encontrada</h3>
                <p>${data.erro || "Ainda não existe piada aprovada."}</p>
            `;
            return;
        }

        piadaAleatoriaBox.innerHTML = `
            <span class="tag">${data.categoria}</span>
            <h3>${escaparHtml(data.pergunta)}</h3>
            <p>${escaparHtml(data.resposta)}</p>
            <div class="card-meta">
                <span class="badge approved">${data.status}</span>
                <span class="badge">Autor: ${escaparHtml(data.autor)}</span>
            </div>
        `;
    } catch (error) {
        piadaAleatoriaBox.innerHTML = `
            <span class="tag">Erro</span>
            <h3>Não foi possível buscar a piada</h3>
            <p>${error.message}</p>
        `;
    }
}

async function enviarPiada(event) {
    event.preventDefault();

    const formData = new FormData(formPiada);

    const payload = {
        pergunta: formData.get("pergunta"),
        resposta: formData.get("resposta"),
        categoria: formData.get("categoria"),
        autor: formData.get("autor")
    };

    try {
        mensagemEnvio.className = "message";
        mensagemEnvio.textContent = "Enviando piada para aprovação...";

        const response = await fetch(`${API_BASE_URL}/api/piadas/sugestoes`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (!response.ok) {
            mensagemEnvio.className = "message error";
            mensagemEnvio.textContent = data.erro || "Erro ao enviar piada.";
            return;
        }

        mensagemEnvio.className = "message success";
        mensagemEnvio.textContent = "Piada enviada com sucesso! Ela está aguardando aprovação.";

        formPiada.reset();

        carregarPiadasPendentes();
    } catch (error) {
        mensagemEnvio.className = "message error";
        mensagemEnvio.textContent = error.message;
    }
}

async function carregarPiadasPendentes() {
    try {
        listaPendentes.innerHTML = criarMensagemCarregando("Carregando piadas pendentes...");

        const response = await fetch(`${API_BASE_URL}/api/admin/piadas/pendentes`);
        const piadas = await response.json();

        if (!response.ok) {
            throw new Error("Erro ao carregar piadas pendentes.");
        }

        if (piadas.length === 0) {
            listaPendentes.innerHTML = criarMensagemVazia("Nenhuma piada pendente no momento.");
            return;
        }

        listaPendentes.innerHTML = piadas.map(criarCardAdmin).join("");
    } catch (error) {
        listaPendentes.innerHTML = criarMensagemErro(error.message);
    }
}

async function aprovarPiada(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/piadas/${id}/aprovar`, {
            method: "POST"
        });

        const data = await response.json();

        if (!response.ok) {
            alert(data.erro || "Erro ao aprovar piada.");
            return;
        }

        alert("Piada aprovada! O webhook foi disparado.");

        carregarPiadasPendentes();
        carregarPiadasAprovadas();
    } catch (error) {
        alert(error.message);
    }
}

async function rejeitarPiada(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/piadas/${id}/rejeitar`, {
            method: "POST"
        });

        const data = await response.json();

        if (!response.ok) {
            alert(data.erro || "Erro ao rejeitar piada.");
            return;
        }

        alert("Piada rejeitada.");

        carregarPiadasPendentes();
    } catch (error) {
        alert(error.message);
    }
}

function criarCardPiada(piada) {
    return `
        <article class="card">
            <h3>${escaparHtml(piada.pergunta)}</h3>
            <p>${escaparHtml(piada.resposta)}</p>

            <div class="card-meta">
                <span class="badge approved">${escaparHtml(piada.status)}</span>
                <span class="badge">${escaparHtml(piada.categoria)}</span>
                <span class="badge">Autor: ${escaparHtml(piada.autor)}</span>
            </div>
        </article>
    `;
}

function criarCardAdmin(piada) {
    return `
        <article class="card">
            <h3>${escaparHtml(piada.pergunta)}</h3>
            <p>${escaparHtml(piada.resposta)}</p>

            <div class="card-meta">
                <span class="badge pending">${escaparHtml(piada.status)}</span>
                <span class="badge">${escaparHtml(piada.categoria)}</span>
                <span class="badge">Autor: ${escaparHtml(piada.autor)}</span>
                <span class="badge">ID: ${piada.id}</span>
            </div>

            <div class="admin-actions">
                <button class="btn secondary" onclick="aprovarPiada(${piada.id})">
                    Aprovar
                </button>

                <button class="btn danger" onclick="rejeitarPiada(${piada.id})">
                    Rejeitar
                </button>
            </div>
        </article>
    `;
}

function criarMensagemCarregando(texto) {
    return `<div class="empty">${texto}</div>`;
}

function criarMensagemVazia(texto) {
    return `<div class="empty">${texto}</div>`;
}

function criarMensagemErro(texto) {
    return `<div class="empty message error">${texto}</div>`;
}

function escaparHtml(valor) {
    if (valor === null || valor === undefined) {
        return "";
    }

    return String(valor)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}