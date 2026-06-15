// ===== Translations =====
const STRINGS = {
    en: {
        getHelp: "Get Help",
        communityInfo: "Community Info",
        announcements: "Announcements",
        communityInfoSoon: "Community Info section coming soon!",
        announcementsSoon: "Announcements section coming soon!",
        heroTitle: "What do you need help with today?",
        heroSubtitle: "Find housing, essentials, community programs and local updates in one trusted place.",
        housingHelp: "Housing Help",
        housingHelpSub: "Home and Rental Assistance, Shelters, Home Repairs",
        housingHelpDesc: "Find programs and local organizations that can help you find, buy or rent a place to live. Browse emergency shelter options, rental assistance programs and homeownership or mortgage resources. Listings include contact details and eligibility information so you can act quickly.",
        essentials: "Free / Low-Cost Essentials",
        essentialsSub: "Food, Furniture, Clothing",
        essentialsDesc: "Check out these local programs and nonprofits offering furniture, utilities, repairs for free or at a low cost. Make your home more comfortable and safe with a few simple steps.",
        seasonal: "Community Resources",
        seasonalSub: "Programs, Events and Community Opportunities",
        weeklyUpdates: "Weekly Updates",
        weeklyUpdatesSub: "News and Changes Impacting You",
        weeklyUpdatesDesc: "Stay up to date on the rules, public meetings and changes that affect housing, benefits and community services. Read the highlights and learn about important deadlines, new requirements and policy updates so you can participate and plan ahead.",
        aiTitle: "AI Guidance",
        aiSubtitle: "Ask a question in natural language to find resources",
        aiPlaceholder: "E.g., I need rental help near Wilmington for seniors",
        aiButton: "Get Help",
        aiUrgent: "🚨 Urgent",
        aiHousing: "🏠 Housing",
        aiEssentials: "🛒 Essentials",
        latestUpdates: "Latest Updates",
        refineSearch: "Refine Your Search",
        filterSubtitle: "Choose filters to narrow results.",
        viewResults: "View Results",
        backHome: "← Back to Home",
        backResults: "← Back to Results",
        back: "← Back",
        noResults: "No matching resources found.",
        loading: "Loading...",
        loadingAI: "Finding resources for you…",
        phone: "Phone",
        address: "Location",
        website: "Visit Website →",
        eligibility: "Who Qualifies",
        category: "Category",
        urgency: "Urgency",
        about: "About",
        sources: "Sources",
        viewDetails: "View Details →",
        callNow: "📞 Call",
        aiSummary: "AI Summary & Guidance",
        aiAnalyzed: "AI analyzed your query and found relevant resources",
    },
    es: {
        getHelp: "Obtener Ayuda",
        communityInfo: "Info Comunitaria",
        announcements: "Anuncios",
        communityInfoSoon: "¡La sección de Información Comunitaria estará disponible pronto!",
        announcementsSoon: "¡La sección de Anuncios estará disponible pronto!",
        heroTitle: "¿Con qué necesitas ayuda hoy?",
        heroSubtitle: "Encuentra vivienda, artículos esenciales, programas comunitarios y noticias locales en un solo lugar de confianza.",
        housingHelp: "Ayuda con Vivienda",
        housingHelpSub: "Asistencia para Alquiler, Refugios, Reparaciones del Hogar",
        housingHelpDesc: "Encuentra programas y organizaciones locales que pueden ayudarte a encontrar, comprar o alquilar un lugar donde vivir.",
        essentials: "Artículos Esenciales Gratis / Económicos",
        essentialsSub: "Comida, Muebles, Ropa",
        essentialsDesc: "Consulta estos programas locales y organizaciones sin fines de lucro que ofrecen muebles, servicios y reparaciones de forma gratuita o a bajo costo.",
        seasonal: "Recursos Comunitarios",
        seasonalSub: "Programas, Eventos y Oportunidades Comunitarias",
        weeklyUpdates: "Actualizaciones Semanales",
        weeklyUpdatesSub: "Noticias y Cambios que te Afectan",
        weeklyUpdatesDesc: "Mantente al día sobre las reglas, reuniones públicas y cambios que afectan la vivienda, los beneficios y los servicios comunitarios.",
        aiTitle: "Orientación con IA",
        aiSubtitle: "Haz una pregunta para encontrar recursos",
        aiPlaceholder: "Ej., Necesito ayuda con el alquiler cerca de Wilmington para personas mayores",
        aiButton: "Obtener Ayuda",
        aiUrgent: "🚨 Urgente",
        aiHousing: "🏠 Vivienda",
        aiEssentials: "🛒 Esenciales",
        latestUpdates: "Últimas Actualizaciones",
        refineSearch: "Refinar Búsqueda",
        filterSubtitle: "Elige filtros para reducir resultados.",
        viewResults: "Ver Resultados",
        backHome: "← Volver al Inicio",
        backResults: "← Volver a Resultados",
        back: "← Volver",
        noResults: "No se encontraron recursos.",
        loading: "Cargando...",
        loadingAI: "Buscando recursos para ti…",
        phone: "Teléfono",
        address: "Ubicación",
        website: "Visitar Sitio Web →",
        eligibility: "Quién Califica",
        category: "Categoría",
        urgency: "Urgencia",
        about: "Acerca de",
        sources: "Fuentes",
        viewDetails: "Ver Detalles →",
        callNow: "📞 Llamar",
        aiSummary: "Resumen y Orientación con IA",
        aiAnalyzed: "La IA analizó tu pregunta y encontró recursos relevantes",
    }
};

let currentLanguage = "en";

function t(key) {
    return (STRINGS[currentLanguage] && STRINGS[currentLanguage][key]) || STRINGS.en[key] || key;
}

function applyLanguage() {
    document.querySelectorAll("[data-i18n]").forEach(el => {
        const key = el.getAttribute("data-i18n");
        if (el.tagName === "INPUT" || el.tagName === "TEXTAREA") {
            el.placeholder = t(key);
        } else {
            el.textContent = t(key);
        }
    });
    document.getElementById("language-toggle").textContent =
        currentLanguage === "en" ? "ES" : "EN";
}

// ===== Page Navigation =====
const homeLogoLink = document.getElementById("home-logo-link");
const navGetHelp = document.getElementById("nav-get-help");
const navCommunityInfo = document.getElementById("nav-community-info");
const navAnnouncements = document.getElementById("nav-announcements");
const backFromFilterButton = document.getElementById("back-from-filter-button");
const backHomeButtonResults = document.getElementById("back-home-button-results");

function goHome() {
    document.getElementById("home-screen").style.display = "block";
    document.getElementById("filter-screen").style.display = "none";
    document.getElementById("results-screen").style.display = "none";
    document.getElementById("detail-screen").style.display = "none";
    window.scrollTo(0, 0);
}

homeLogoLink.addEventListener("click", (e) => { e.preventDefault(); goHome(); });

const aiBannerHeader = document.getElementById("ai-banner-header");
aiBannerHeader.addEventListener("click", () => {
    goHome();
    setTimeout(() => {
        document.getElementById("ai-guidance-home").scrollIntoView({ behavior: "smooth" });
        document.getElementById("ai-question").focus();
    }, 50);
});
aiBannerHeader.addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") aiBannerHeader.click(); });

navGetHelp.addEventListener("click", (e) => {
    e.preventDefault();
    goHome();
    navGetHelp.classList.add("active");
    navCommunityInfo.classList.remove("active");
    navAnnouncements.classList.remove("active");
});

navCommunityInfo.addEventListener("click", (e) => {
    e.preventDefault();
    navGetHelp.classList.remove("active");
    navCommunityInfo.classList.add("active");
    navAnnouncements.classList.remove("active");
    showSeasonalResources();
});

navAnnouncements.addEventListener("click", (e) => {
    e.preventDefault();
    navGetHelp.classList.remove("active");
    navCommunityInfo.classList.remove("active");
    navAnnouncements.classList.add("active");
    loadNewsUpdates();
});

// ===== Language Toggle =====
document.getElementById("language-toggle").addEventListener("click", () => {
    currentLanguage = currentLanguage === "en" ? "es" : "en";
    applyLanguage();
});

// ===== Accessibility Controls =====
document.getElementById("contrast-button").addEventListener("click", () => {
    document.body.classList.toggle("high-contrast");
    localStorage.setItem("high-contrast", document.body.classList.contains("high-contrast"));
});

document.getElementById("increase-text-button").addEventListener("click", () => {
    const current = parseFloat(getComputedStyle(document.documentElement).fontSize);
    const next = Math.min(current + 2, 24);
    document.documentElement.style.fontSize = next + "px";
    localStorage.setItem("font-size", next + "px");
});

document.getElementById("decrease-text-button").addEventListener("click", () => {
    const current = parseFloat(getComputedStyle(document.documentElement).fontSize);
    const next = Math.max(current - 2, 12);
    document.documentElement.style.fontSize = next + "px";
    localStorage.setItem("font-size", next + "px");
});

window.addEventListener("DOMContentLoaded", () => {
    if (localStorage.getItem("high-contrast") === "true") {
        document.body.classList.add("high-contrast");
    }
    const savedFontSize = localStorage.getItem("font-size");
    if (savedFontSize) document.documentElement.style.fontSize = savedFontSize;
    loadSidebarNews();
    applyLanguage();
});

// ===== DOM References =====
const resultsContainer = document.getElementById("results");
const resultsScreen = document.getElementById("results-screen");
const filterScreen = document.getElementById("filter-screen");
const urgentFilterButton = document.getElementById("urgent-filter");
const continueButton = document.getElementById("continue-button");
const newsResultsContainer = document.getElementById("news-results");
const seasonalResultsContainer = document.getElementById("seasonal-results");
const essentialsResultsContainer = document.getElementById("essentials-results");
const homeScreen = document.getElementById("home-screen");
const detailScreen = document.getElementById("detail-screen");
const detailView = document.getElementById("detail-view");
const backResultsButton = document.getElementById("back-results-button");
const backHomeButton = document.getElementById("back-home-button");

let urgentFilterSelected = false;

// ===== Category Navigation =====
document.getElementById("housing-help-button").addEventListener("click", () => {
    homeScreen.style.display = "none";
    filterScreen.style.display = "block";
    resultsContainer.innerHTML = "";
    window.scrollTo(0, 0);
});

continueButton.addEventListener("click", () => {
    filterScreen.style.display = "none";
    loadHousingResources();
});

urgentFilterButton.addEventListener("click", () => {
    urgentFilterSelected = !urgentFilterSelected;
    urgentFilterButton.classList.toggle("selected");
});

document.getElementById("weekly-updates-button-home").addEventListener("click", loadNewsUpdates);
document.getElementById("seasonal-resources-button").addEventListener("click", showSeasonalResources);
document.getElementById("essentials-button").addEventListener("click", loadEssentialsResources);

backHomeButton?.addEventListener("click", goHome);
backResultsButton.addEventListener("click", hideDetailScreen);
backFromFilterButton?.addEventListener("click", goHome);
backHomeButtonResults?.addEventListener("click", goHome);

// ===== AI Guidance =====
const aiQuestionEl = document.getElementById("ai-question");
const aiSubmitBtn = document.getElementById("ai-submit");
const aiOutputEl = document.getElementById("ai-output");

let aiUrgent = false;
let aiPreferredCategories = [];

function setAiChipSelected(btn, selected) {
    btn.classList.toggle("selected", selected);
}

document.getElementById("ai-urgent").addEventListener("click", () => {
    aiUrgent = !aiUrgent;
    setAiChipSelected(document.getElementById("ai-urgent"), aiUrgent);
});

document.getElementById("ai-housing").addEventListener("click", () => {
    const idx = aiPreferredCategories.indexOf("housing");
    if (idx >= 0) aiPreferredCategories.splice(idx, 1);
    else aiPreferredCategories.push("housing");
    setAiChipSelected(document.getElementById("ai-housing"), aiPreferredCategories.includes("housing"));
});

document.getElementById("ai-essentials").addEventListener("click", () => {
    const idx = aiPreferredCategories.indexOf("essentials");
    if (idx >= 0) aiPreferredCategories.splice(idx, 1);
    else aiPreferredCategories.push("essentials");
    setAiChipSelected(document.getElementById("ai-essentials"), aiPreferredCategories.includes("essentials"));
});

aiSubmitBtn.addEventListener("click", submitDecision);
aiQuestionEl.addEventListener("keydown", (e) => { if (e.key === "Enter") submitDecision(); });

document.getElementById("ai-reset").addEventListener("click", () => {
    aiQuestionEl.value = "";
    aiOutputEl.innerHTML = "";
    aiUrgent = false;
    aiPreferredCategories = [];
    setAiChipSelected(document.getElementById("ai-urgent"), false);
    setAiChipSelected(document.getElementById("ai-housing"), false);
    setAiChipSelected(document.getElementById("ai-essentials"), false);
    aiQuestionEl.focus();
});

async function submitDecision() {
    const userQuery = (aiQuestionEl.value || "").trim();
    if (!userQuery) {
        aiOutputEl.innerHTML = `<p class="ai-error">${t("aiPlaceholder")}</p>`;
        return;
    }

    aiOutputEl.innerHTML = `
        <div class="ai-loading">
            <div class="spinner"></div>
            <span>${t("loadingAI")}</span>
        </div>`;

    try {
        const res = await fetch("/api/decide", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                userQuery,
                urgent: aiUrgent,
                preferredCategories: aiPreferredCategories
            })
        });

        if (!res.ok) {
            const text = await res.text();
            throw new Error("HTTP " + res.status + ": " + text);
        }

        const data = await res.json();
        renderDecisionResponse(data);

    } catch (err) {
        console.error(err);
        aiOutputEl.innerHTML = `<p class="ai-error">Unable to get AI guidance: ${err.message || String(err)}</p>`;
    }
}

function renderDecisionResponse(data) {
    const title = data?.answerTitle || "Guidance";
    const notes = data?.notes || "";
    const steps = data?.steps || [];
    const citations = data?.citations || [];

    let stepsHtml = "";
    if (steps.length > 0) {
        stepsHtml = steps
            .sort((a, b) => (a.order || 0) - (b.order || 0))
            .map((s, i) => `
                <div class="ai-step">
                    <div class="ai-step-number">${i + 1}</div>
                    <div class="ai-step-body">
                        <div class="ai-step-title">${s.title || "Step"}: ${s.action || ""}</div>
                        ${s.why ? `<div class="ai-step-why"><strong>Why:</strong> ${s.why}</div>` : ""}
                    </div>
                </div>
            `).join("");
    } else {
        stepsHtml = `<p class="ai-no-steps">${notes || "No specific steps available for this query."}</p>`;
    }

    const citationsHtml = citations.length > 0
        ? `<div class="ai-citations">
            <strong>${t("sources")}:</strong>
            <ul>${citations.map(c => `<li>${c.sourceType || ""}: ${c.label || c.id || ""}</li>`).join("")}</ul>
           </div>`
        : "";

    aiOutputEl.innerHTML = `
        <div class="ai-response-card">
            <div class="ai-response-header">
                <span class="ai-response-icon">🤖</span>
                <h4 class="ai-response-title">${title}</h4>
            </div>
            ${notes && steps.length > 0 ? `<p class="ai-response-notes">${notes}</p>` : ""}
            <div class="ai-steps">${stepsHtml}</div>
            ${citationsHtml}
        </div>
    `;
}

// ===== Resource Loading =====
async function loadHousingResources() {
    newsResultsContainer.style.display = "none";
    essentialsResultsContainer.style.display = "none";
    seasonalResultsContainer.style.display = "none";
    detailScreen.style.display = "none";
    resultsContainer.style.display = "block";
    resultsContainer.innerHTML = `<p>${t("loading")}</p>`;

    try {
        const response = await fetch("/api/resources");
        const resources = await response.json();

        let housingResources = resources.filter(r =>
            r.category && r.category.toLowerCase().includes("housing")
        );

        if (urgentFilterSelected) {
            housingResources = housingResources.filter(r => {
                const u = (r.urgency || "").toLowerCase();
                return u === "emergency" || u === "time-limited";
            });
        }

        displayResources(housingResources);
        showResultsScreen();

    } catch (error) {
        console.error(error);
        resultsContainer.innerHTML = "<p>Unable to load resources.</p>";
    }
}

async function loadEssentialsResources() {
    resultsContainer.style.display = "none";
    newsResultsContainer.style.display = "none";
    seasonalResultsContainer.style.display = "none";
    detailScreen.style.display = "none";
    essentialsResultsContainer.style.display = "block";
    essentialsResultsContainer.innerHTML = `<p>${t("loading")}</p>`;

    try {
        const response = await fetch("/api/resources");
        const resources = await response.json();
        const free = resources.filter(r => r.cost && r.cost.toLowerCase() === "free");
        displayEssentials(free);
        showResultsScreen();
    } catch (error) {
        console.error(error);
        essentialsResultsContainer.innerHTML = "<p>Unable to load resources.</p>";
    }
}

async function loadNewsUpdates() {
    resultsContainer.style.display = "none";
    essentialsResultsContainer.style.display = "none";
    seasonalResultsContainer.style.display = "none";
    detailScreen.style.display = "none";
    newsResultsContainer.style.display = "block";
    newsResultsContainer.innerHTML = `<p>${t("loading")}</p>`;

    try {
        const response = await fetch("/api/news");
        const newsItems = await response.json();
        displayNews(newsItems);
        showResultsScreen();
    } catch (error) {
        console.error(error);
        newsResultsContainer.innerHTML = "<p>Unable to load updates.</p>";
    }
    return Promise.resolve();
}

async function loadSidebarNews() {
    try {
        const response = await fetch("/api/news");
        const items = await response.json();
        const newsResults = document.getElementById("sidebar-news");
        newsResults.innerHTML = "";

        items.filter(item => item.active).slice(0, 3).forEach(item => {
            const card = document.createElement("div");
            card.className = "news-item";
            card.innerHTML = `
                <h4>${item.headline}</h4>
                <div class="news-date">${item.published || "Latest"}</div>
            `;
            card.addEventListener("click", () => {
                loadNewsUpdates().then(() => showNewsDetail(item));
            });
            newsResults.appendChild(card);
        });
    } catch (error) {
        console.error("Sidebar news failed to load:", error);
        document.getElementById("sidebar-news").innerHTML =
            '<p style="color: var(--text-secondary); font-size: 13px;">Unable to load updates</p>';
    }
}

// ===== Screen Management =====
let activeResultsContainer = null;

function showResultsScreen() {
    homeScreen.style.display = "none";
    filterScreen.style.display = "none";
    detailScreen.style.display = "none";
    resultsScreen.style.display = "block";
    window.scrollTo(0, 0);
}

function showDetailScreen() {
    // Hide whichever results container is currently visible
    [resultsContainer, newsResultsContainer, essentialsResultsContainer, seasonalResultsContainer].forEach(el => {
        if (el.style.display !== "none") {
            activeResultsContainer = el;
            el.style.display = "none";
        }
    });
    detailScreen.style.display = "block";
    window.scrollTo(0, 0);
}

function hideDetailScreen() {
    detailScreen.style.display = "none";
    if (activeResultsContainer) {
        activeResultsContainer.style.display = "block";
    } else {
        resultsContainer.style.display = "block";
    }
    window.scrollTo(0, 0);
}

function renderPageHeader(title, description) {
    return `
        <div class="page-header">
            <h2>${title}</h2>
            <p class="page-description">${description}</p>
        </div>
    `;
}

function urgencyClass(urgency) {
    const u = (urgency || "standard").toLowerCase().replace(/\s+/g, "-");
    if (u === "emergency") return "urgency-emergency";
    if (u === "time-limited") return "urgency-time-limited";
    return "urgency-standard";
}

// ===== Display Functions =====
function displayResources(resources) {
    resultsContainer.innerHTML = renderPageHeader(t("housingHelp"), t("housingHelpDesc"));

    if (resources.length === 0) {
        resultsContainer.innerHTML += `<p class="empty-state">${t("noResults")}</p>`;
        return;
    }

    resources.forEach(resource => {
        const phone = resource.phones?.[0]?.number;
        const urgency = resource.urgency || "Standard";

        const card = document.createElement("div");
        card.className = "resource-card";
        card.innerHTML = `
            <div class="card-top">
                <h3 class="card-title">${resource.organization}</h3>
                <span class="urgency-tag ${urgencyClass(urgency)}">${urgency}</span>
            </div>
            <p class="card-summary">${resource.summary || ""}</p>
            ${phone ? `<p class="card-phone"><a href="tel:${phone}" onclick="event.stopPropagation()">📞 ${phone}</a></p>` : ""}
            <p class="card-cta">${t("viewDetails")}</p>
        `;
        card.addEventListener("click", () => showResourceDetails(resource));
        resultsContainer.appendChild(card);
    });
}

function displayEssentials(resources) {
    essentialsResultsContainer.innerHTML = renderPageHeader(t("essentials"), t("essentialsDesc"));

    if (resources.length === 0) {
        essentialsResultsContainer.innerHTML += `<p class="empty-state">${t("noResults")}</p>`;
        return;
    }

    const grouped = {};
    resources.forEach(r => {
        const cat = r.category || "Other";
        if (!grouped[cat]) grouped[cat] = [];
        grouped[cat].push(r);
    });

    const categories = Object.keys(grouped);

    // Category anchor nav at the top
    const anchorSlug = cat => cat.toLowerCase().replace(/[^a-z0-9]+/g, "-");
    const navHtml = `
        <div class="essentials-category-nav">
            <span class="essentials-nav-label">Jump to:</span>
            ${categories.map(cat => `
                <a class="essentials-nav-link" href="#cat-${anchorSlug(cat)}">${cat}</a>
            `).join("")}
        </div>
    `;
    essentialsResultsContainer.innerHTML += navHtml;

    Object.entries(grouped).forEach(([category, items]) => {
        const slug = anchorSlug(category);
        essentialsResultsContainer.innerHTML += `<h3 class="category-group-header" id="cat-${slug}">${category}</h3>`;
        items.forEach(resource => {
            const phone = resource.phones?.[0]?.number;
            const card = document.createElement("div");
            card.className = "resource-card";
            card.innerHTML = `
                <div class="card-top">
                    <h3 class="card-title">${resource.organization}</h3>
                    <span class="urgency-tag urgency-standard">Free</span>
                </div>
                <p class="card-summary">${resource.summary || ""}</p>
                ${phone ? `<p class="card-phone"><a href="tel:${phone}" onclick="event.stopPropagation()">📞 ${phone}</a></p>` : ""}
                <p class="card-cta">${t("viewDetails")}</p>
            `;
            card.addEventListener("click", () => showResourceDetails(resource));
            essentialsResultsContainer.appendChild(card);
        });
    });
}

function displayNews(newsItems) {
    newsResultsContainer.innerHTML = renderPageHeader(t("weeklyUpdates"), t("weeklyUpdatesDesc"));

    newsItems.forEach(item => {
        const cats = (item.category_tags || []).join(" · ");
        const sourceLink = item.source_url
            ? `<a href="${item.source_url}" target="_blank" rel="noopener noreferrer" class="card-source-link" onclick="event.stopPropagation()">
                   ${item.source_name} ↗
               </a>`
            : item.source_name;
        const card = document.createElement("div");
        card.className = "resource-card";
        card.innerHTML = `
            <span class="urgency-tag urgency-standard">${cats || "General"}</span>
            <h3 class="card-title" style="margin-top:10px;">${item.headline}</h3>
            <p class="card-summary">${item.summary}</p>
            <p class="card-why"><strong>Why this matters:</strong> ${item.why_it_matters}</p>
            <p class="card-source">${sourceLink} · ${item.published}</p>
            <p class="card-cta">${t("viewDetails")}</p>
        `;
        card.addEventListener("click", () => showNewsDetail(item));
        newsResultsContainer.appendChild(card);
    });
}

function showNewsDetail(item) {
    const cats = (item.category_tags || []).join(" · ");
    const sourceLink = item.source_url
        ? `<a href="${item.source_url}" target="_blank" rel="noopener noreferrer" class="detail-source-link">Read More →</a>`
        : "";

    detailView.innerHTML = `
        <div class="detail-header">
            <h2 class="detail-org">${item.headline}</h2>
            <span class="urgency-tag urgency-standard">${cats || "General"}</span>
        </div>

        ${item.body ? `
        <div class="detail-section">
            <div class="detail-label">${t("about")}</div>
            <div class="detail-value">${item.body}</div>
        </div>` : item.summary ? `
        <div class="detail-section">
            <div class="detail-label">${t("about")}</div>
            <div class="detail-value">${item.summary}</div>
        </div>` : ""}

        ${item.why_it_matters ? `
        <div class="detail-section">
            <div class="detail-label">Why This Matters</div>
            <div class="detail-value">${item.why_it_matters}</div>
        </div>` : ""}

        <div class="detail-section">
            <div class="detail-label">Source</div>
            <div class="detail-value">
                ${item.source_name}${item.published ? " · " + item.published : ""}
                ${sourceLink ? `<br>${sourceLink}` : ""}
            </div>
        </div>
    `;

    showDetailScreen();
}

async function showSeasonalResources() {
    resultsContainer.style.display = "none";
    newsResultsContainer.style.display = "none";
    essentialsResultsContainer.style.display = "none";
    detailScreen.style.display = "none";

    const carousel = document.getElementById("seasonal-carousel");
    carousel.innerHTML = `<p style="padding:16px;color:var(--text-secondary)">Loading...</p>`;
    seasonalResultsContainer.style.display = "block";
    showResultsScreen();

    try {
        const response = await fetch("/api/seasonal-images");
        const paths = await response.json();
        carousel.innerHTML = "";
        if (paths.length === 0) {
            carousel.innerHTML = `<p style="padding:16px;color:var(--text-secondary)">No community resources available.</p>`;
        } else {
            paths.forEach(src => {
                const filename = src.split("/").pop().replace(/\.[^.]+$/, "");
                carousel.innerHTML += `
                    <div class="carousel-card">
                        <img src="${src}" alt="${filename}" loading="lazy">
                        <div class="carousel-caption">${filename}</div>
                    </div>
                `;
            });
        }
    } catch (error) {
        console.error("Failed to load seasonal images:", error);
        carousel.innerHTML = `<p style="padding:16px;color:var(--text-secondary)">Unable to load resources.</p>`;
    }
}

function showResourceDetails(resource) {
    const phone = resource.phones?.[0]?.number;
    const website = resource.websites?.[0]?.url;
    const location = resource.locations?.[0];
    const urgency = resource.urgency || "Standard";

    let addressHtml = "";
    if (location && !location.confidential) {
        if (location.address) {
            addressHtml = `${location.address}, ${location.city}, ${location.state} ${location.zip || ""}`;
        } else if (location.city) {
            addressHtml = `${location.city}, ${location.state}`;
        }
    }

    detailView.innerHTML = `
        <div class="detail-header">
            <h2 class="detail-org">${resource.organization}</h2>
            <span class="urgency-tag ${urgencyClass(urgency)}">${urgency}</span>
        </div>

        ${resource.description || resource.summary ? `
        <div class="detail-section">
            <div class="detail-label">${t("about")}</div>
            <div class="detail-value">${resource.description || resource.summary}</div>
        </div>` : ""}

        ${resource.eligibility ? `
        <div class="detail-section">
            <div class="detail-label">${t("eligibility")}</div>
            <div class="detail-value">${resource.eligibility}</div>
        </div>` : ""}

        ${resource.category ? `
        <div class="detail-section">
            <div class="detail-label">${t("category")}</div>
            <div class="detail-value">${resource.category}${resource.subcategory ? " · " + resource.subcategory : ""}</div>
        </div>` : ""}

        ${phone ? `
        <div class="detail-section">
            <div class="detail-label">${t("phone")}</div>
            <div class="detail-value"><a href="tel:${phone}" class="detail-phone-link">📞 ${phone}</a></div>
        </div>` : ""}

        ${addressHtml ? `
        <div class="detail-section">
            <div class="detail-label">${t("address")}</div>
            <div class="detail-value">${addressHtml}</div>
        </div>` : ""}

        ${website ? `
        <div class="detail-section">
            <div class="detail-label">${t("website")}</div>
            <div class="detail-value"><a href="${website}" target="_blank" rel="noopener noreferrer">${t("website")}</a></div>
        </div>` : ""}
    `;

    showDetailScreen();
}

// ===== Results Page AI Widget =====
const resultsAiQuestion = document.getElementById("results-ai-question");
const resultsAiSubmit = document.getElementById("results-ai-submit");
const resultsAiOutput = document.getElementById("results-ai-output");

async function submitResultsAi(query) {
    const userQuery = (query || resultsAiQuestion.value || "").trim();
    if (!userQuery) return;
    resultsAiQuestion.value = userQuery;
    resultsAiOutput.innerHTML = `
        <div class="ai-loading">
            <div class="spinner"></div>
            <span>${t("loadingAI")}</span>
        </div>`;

    try {
        const res = await fetch("/api/decide", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userQuery, urgent: false, preferredCategories: [] })
        });
        if (!res.ok) throw new Error("HTTP " + res.status);
        const data = await res.json();
        renderResultsAiResponse(data);
    } catch (err) {
        resultsAiOutput.innerHTML = `<p class="ai-error">Unable to get AI guidance: ${err.message}</p>`;
    }
}

function renderResultsAiResponse(data) {
    const title = data?.answerTitle || "Guidance";
    const steps = data?.steps || [];
    const notes = data?.notes || "";

    let stepsHtml = "";
    if (steps.length > 0) {
        stepsHtml = steps
            .sort((a, b) => (a.order || 0) - (b.order || 0))
            .map((s, i) => `
                <div class="ai-step">
                    <div class="ai-step-number">${i + 1}</div>
                    <div class="ai-step-body">
                        <div class="ai-step-title">${s.title || "Step"}: ${s.action || ""}</div>
                        ${s.why ? `<div class="ai-step-why"><strong>Why:</strong> ${s.why}</div>` : ""}
                    </div>
                </div>
            `).join("");
    } else {
        stepsHtml = `<p class="ai-no-steps">${notes || "No specific steps available for this query."}</p>`;
    }

    resultsAiOutput.innerHTML = `
        <div class="ai-response-card">
            <div class="ai-response-header">
                <span class="ai-response-icon">🤖</span>
                <h4 class="ai-response-title">${title}</h4>
            </div>
            ${notes && steps.length > 0 ? `<p class="ai-response-notes">${notes}</p>` : ""}
            <div class="ai-steps">${stepsHtml}</div>
        </div>
    `;
}

resultsAiSubmit.addEventListener("click", () => submitResultsAi());
resultsAiQuestion.addEventListener("keydown", (e) => { if (e.key === "Enter") submitResultsAi(); });

document.querySelectorAll(".results-ai-chip").forEach(chip => {
    chip.addEventListener("click", () => {
        const query = chip.getAttribute("data-query");
        submitResultsAi(query);
        chip.closest(".results-ai-chips").querySelectorAll(".results-ai-chip").forEach(c => c.classList.remove("selected"));
        chip.classList.add("selected");
    });
});

document.getElementById("results-ai-reset").addEventListener("click", () => {
    resultsAiQuestion.value = "";
    resultsAiOutput.innerHTML = "";
    document.querySelectorAll(".results-ai-chip").forEach(c => c.classList.remove("selected"));
    resultsAiQuestion.focus();
});
