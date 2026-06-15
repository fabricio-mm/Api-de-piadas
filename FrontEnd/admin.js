const API_BASE_URL = "https://apiadas-backend.onrender.com";

const formLoginAdmin = document.getElementById("formLoginAdmin");
const mensagemLogin = document.getElementById("mensagemLogin");
const loginAdmin = document.getElementById("loginAdmin");
const painelAdmin = document.getElementById("painelAdmin");
const listaPendentes = document.getElementById("listaPendentes");
const btnCarregarPendentes = document.getElementById("btnCarregarPendentes");
const btnSairAdmin = document.getElementById("btnSairAdmin");

formLoginAdmin.addEventListener("submit", entrarAdmin);
btnCarregarPendentes.addEventListener("click", carregarPiadasPendentes);
btnSairAdmin.addEventListener("click", sairAdmin);

window.addEventListener("load", () => {
    const token = localStorage.getItem("adminToken");

    if (token) {
        mostrarPainelAdmin();
        carregarPiadasPendentes();
    }
});

function entrarAdmin(event) {
    event.preventDefault();

    const token = document.getElementById("adminToken").value;

    if (!token || token.trim() === "") {
        mensagemLogin.className = "message error";
        mensagemLogin.textContent = "Digite o token admin.";
        return;
    }

    localStorage.setItem("adminToken", token.trim());

    mensagemLogin.className = "message success";
    mensagemLogin.textContent = "Login realizado com sucesso.";

    mostrarPainelAdmin();
    carregarPiadasPendentes();
}

function sairAdmin() {
    localStorage.removeItem("adminToken");

    painelAdmin.classList.add("hidden");
    loginAdmin.classList.remove("hidden");

    listaPendentes.innerHTML = "";
}

function mostrarPainelAdmin() {
    loginAdmin.classList.add("hidden");
    painelAdmin.classList.remove("hidden");
}

function obterHeadersAdmin() {
    const token = localStorage.getItem("adminToken");

    return {
        "Authorization": `Bearer ${token}`
    };
}

async function carregarPiadasPendentes() {
    try {
        listaPendentes.innerHTML = criarMensagemCarregando("Carregando piadas pendentes...");

        const response = await fetch(`${API_BASE_URL}/api/admin/piadas/pendentes`, {
            headers: obterHeadersAdmin()
        });

        const piadas = await response.json();

        if (response.status === 401) {
            sairAdmin();
            mensagemLogin.className = "message error";
            mensagemLogin.textContent = "Token inválido ou expirado.";
            return;
        }

        if (!response.ok) {
            throw new Error(piadas.erro || "Erro ao carregar piadas pendentes.");
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
            method: "POST",
            headers: obterHeadersAdmin()
        });

        const data = await response.json();

        if (response.status === 401) {
            sairAdmin();
            mensagemLogin.className = "message error";
            mensagemLogin.textContent = "Token inválido ou expirado.";
            return;
        }

        if (!response.ok) {
            alert(data.erro || "Erro ao aprovar piada.");
            return;
        }

        alert("Piada aprovada! O webhook foi disparado.");
        carregarPiadasPendentes();
    } catch (error) {
        alert(error.message);
    }
}

async function rejeitarPiada(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/piadas/${id}/rejeitar`, {
            method: "POST",
            headers: obterHeadersAdmin()
        });

        const data = await response.json();

        if (response.status === 401) {
            sairAdmin();
            mensagemLogin.className = "message error";
            mensagemLogin.textContent = "Token inválido ou expirado.";
            return;
        }

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