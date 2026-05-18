(function () {
  const root = document.getElementById("chatbot-root");
  if (!root) return;

  const toggleBtn = document.getElementById("chatbot-toggle");
  const closeBtn = document.getElementById("chatbot-close");
  const clearBtn = document.getElementById("chatbot-clear");
  const panel = document.getElementById("chatbot-panel");
  const userSelect = document.getElementById("chatbot-user");
  const startersBox = document.getElementById("chatbot-starters");
  const messagesBox = document.getElementById("chatbot-messages");
  const form = document.getElementById("chatbot-form");
  const input = document.getElementById("chatbot-input");

  const pageContext = window.__chatbotPageContext || { page: "home" };

  const HISTORY_KEY = "chatbot.history";
  const USER_KEY = "chatbot.user";

  userSelect.value = localStorage.getItem(USER_KEY) || "Alice";

  function loadHistory() {
    try { return JSON.parse(localStorage.getItem(HISTORY_KEY)) || []; }
    catch (e) { return []; }
  }
  function saveHistory(h) {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(h.slice(-12)));
  }
  function clearHistory() {
    localStorage.removeItem(HISTORY_KEY);
    messagesBox.innerHTML = "";
  }

  function renderMessages() {
    messagesBox.innerHTML = "";
    for (const m of loadHistory()) {
      appendMessage(m.role, m.content, m.sources);
    }
    messagesBox.scrollTop = messagesBox.scrollHeight;
  }

  function appendMessage(role, content, sources) {
    const div = document.createElement("div");
    div.className = "chatbot-msg " + role;
    div.textContent = content;
    if (role === "assistant" && sources && sources.length) {
      const s = document.createElement("span");
      s.className = "chatbot-sources";
      s.textContent = "Sources: " + sources
        .map(x => x.title || x.id).filter(Boolean).join(", ");
      div.appendChild(s);
    }
    messagesBox.appendChild(div);
    messagesBox.scrollTop = messagesBox.scrollHeight;
  }

  async function loadStarters() {
    startersBox.innerHTML = "";
    const params = new URLSearchParams({
      page: pageContext.page || "home",
      user: userSelect.value
    });
    if (pageContext.id) params.set("id", pageContext.id);
    try {
      const r = await fetch("/chat/starters?" + params);
      const data = await r.json();
      for (const text of data.starters || []) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "chatbot-starter";
        btn.textContent = text;
        btn.addEventListener("click", () => {
          input.value = text;
          form.requestSubmit();
        });
        startersBox.appendChild(btn);
      }
    } catch (e) { /* network failure — silent */ }
  }

  async function sendMessage(text) {
    const history = loadHistory();
    history.push({ role: "user", content: text });
    saveHistory(history);
    appendMessage("user", text);

    const body = {
      user: userSelect.value,
      message: text,
      history: history.slice(0, -1).map(m => ({ role: m.role, content: m.content })),
      page: pageContext
    };
    try {
      const r = await fetch("/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      const data = await r.json();
      const reply = data.reply || "(no reply)";
      const sources = data.sources || [];
      const newHistory = loadHistory();
      newHistory.push({ role: "assistant", content: reply, sources });
      saveHistory(newHistory);
      appendMessage("assistant", reply, sources);
    } catch (e) {
      appendMessage("assistant", "Network error: " + e.message);
    }
  }

  toggleBtn.addEventListener("click", () => {
    panel.hidden = !panel.hidden;
    if (!panel.hidden) {
      renderMessages();
      loadStarters();
      input.focus();
    }
  });
  closeBtn.addEventListener("click", () => { panel.hidden = true; });
  clearBtn.addEventListener("click", clearHistory);
  userSelect.addEventListener("change", () => {
    localStorage.setItem(USER_KEY, userSelect.value);
    loadStarters();
  });
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const text = input.value.trim();
    if (!text) return;
    input.value = "";
    sendMessage(text);
  });
})();
