const housingButton =
    document.getElementById("housing-help-button");

const resultsContainer =
    document.getElementById("results");

const filterScreen =
    document.getElementById("filter-screen");

const urgentFilterButton =
    document.getElementById("urgent-filter");

let urgentFilterSelected = false;

const continueButton =
    document.getElementById("continue-button");

const weeklyUpdatesButton =
    document.getElementById("weekly-updates-button");

const newsResultsContainer =
    document.getElementById("news-results");

const seasonalButton =
    document.getElementById("seasonal-resources-button");

const seasonalResultsContainer =
    document.getElementById("seasonal-results");

const essentialsButton =
    document.getElementById("essentials-button");

const essentialsResultsContainer =
    document.getElementById("essentials-results");

const homeScreen =
    document.getElementById("home-screen");

const detailScreen =
    document.getElementById("detail-screen");

const detailView =
    document.getElementById("detail-view");

const backResultsButton =
    document.getElementById("back-results-button");

const resultsScreen =
    document.getElementById("results-screen");

const backHomeButton =
    document.getElementById("back-home-button");

const increaseTextButton =
    document.getElementById(
        "increase-text-button"
    );

const decreaseTextButton =
    document.getElementById(
        "decrease-text-button"
    );

const contrastButton =
    document.getElementById(
        "contrast-button"
    );

contrastButton.addEventListener(
    "click",
    () => {

        document.body.classList.toggle(
            "high-contrast"
        );
    }
);

increaseTextButton.addEventListener(
    "click",
    () => {

        const current =
            parseFloat(
                getComputedStyle(document.body)
                    .fontSize
            );

        document.body.style.fontSize =
            (current + 2) + "px";
    }
);

decreaseTextButton.addEventListener(
    "click",
    () => {

        const current =
            parseFloat(
                getComputedStyle(document.body)
                    .fontSize
            );

        document.body.style.fontSize =
            Math.max(current - 2, 12) + "px";
    }
);

housingButton.addEventListener("click", () => {
    homeScreen.style.display = "none";
    filterScreen.style.display = "block";
    resultsContainer.innerHTML = "";
});

    continueButton.addEventListener("click", () => {
    filterScreen.style.display = "none";
    loadHousingResources();
});

urgentFilterButton.addEventListener("click",() => {
        urgentFilterSelected =
            !urgentFilterSelected;

        urgentFilterButton.classList.toggle(
            "selected"
        );
    }
);

    weeklyUpdatesButton.addEventListener(
    "click",
    loadNewsUpdates
);

seasonalButton.addEventListener(
    "click",
    showSeasonalResources
);

essentialsButton.addEventListener(
    "click",
    loadEssentialsResources
);

backHomeButton.addEventListener(
    "click",
    showHomeScreen
);

backResultsButton.addEventListener(
    "click",
    hideDetailScreen
);

document.getElementById("back-from-filter-button")
    .addEventListener("click", showHomeScreen);

async function loadHousingResources() {
    // hide other result panes
    newsResultsContainer.style.display = "none";
    essentialsResultsContainer.style.display = "none";
    seasonalResultsContainer.style.display = "none";
    detailScreen.style.display = "none";

    resultsContainer.style.display = "block";
    resultsContainer.innerHTML = "<p>Loading...</p>";

    try {
        const response = await fetch("/api/resources");
        const resources = await response.json();

        let housingResources = resources.filter(resource =>
            resource.category &&
            resource.category.toLowerCase().includes("housing")
        );

        if (urgentFilterSelected) {
            housingResources = housingResources.filter(resource =>
                resource.urgency &&
                (
                    resource.urgency.toLowerCase() === "emergency" ||
                    resource.urgency.toLowerCase() === "time-limited"
                )
            );
        }

        displayResources(housingResources);
        showResultsScreen();

    } catch (error) {
        console.error(error);
        resultsContainer.innerHTML = "<p>Unable to load resources.</p>";
    }
}

async function loadEssentialsResources() {
    // hide other panes and show essentials
    resultsContainer.style.display = "none";
    newsResultsContainer.style.display = "none";
    seasonalResultsContainer.style.display = "none";
    detailScreen.style.display = "none";

    essentialsResultsContainer.style.display = "block";
    essentialsResultsContainer.innerHTML = "<p>Loading...</p>";

    try {
        const response = await fetch("/api/resources");
        const resources = await response.json();

        const essentialsResources = resources.filter(resource =>
            resource.cost && resource.cost.toLowerCase() === "free"
        );

        displayEssentials(essentialsResources);
        showResultsScreen();

    } catch (error) {
        console.error(error);
        essentialsResultsContainer.innerHTML = "<p>Unable to load resources.</p>";
    }
}

async function loadNewsUpdates() {
    // hide other panes and show news
    resultsContainer.style.display = "none";
    essentialsResultsContainer.style.display = "none";
    seasonalResultsContainer.style.display = "none";
    detailScreen.style.display = "none";

    newsResultsContainer.style.display = "block";
    newsResultsContainer.innerHTML = "<p>Loading updates...</p>";

    try {
        const response = await fetch("/api/news");
        const newsItems = await response.json();

        displayNews(newsItems);
        showResultsScreen();

    } catch (error) {
        console.error(error);
        newsResultsContainer.innerHTML = "<p>Unable to load updates.</p>";
    }
}

function showHomeScreen() {

    homeScreen.style.display = "block";

    filterScreen.style.display = "none";

    resultsScreen.style.display = "none";

    detailScreen.style.display = "none";
}

function showResultsScreen() {

    homeScreen.style.display = "none";

    filterScreen.style.display = "none";

    detailScreen.style.display = "none";
    
    resultsScreen.style.display = "block";
}

function showDetailScreen() {

    resultsContainer.style.display = "none";

    detailScreen.style.display = "block";
}

function hideDetailScreen() {

    detailScreen.style.display = "none";

    resultsContainer.style.display = "block";
}

//function renderPageHeader(emoji, title, description) {
    //return `
        //<div class="page-header">
            //<h2>${emoji} ${title}</h2>
            //<p class="page-description">${description}</p>
        //</div>
    //`;
//}
function renderPageHeader(emoji, title, description) {
    return `
        <div class="page-header">
            <h2>${title}</h2>
            <p class="page-description">${description}</p>
        </div>
    `;
}

function displayResources(resources) {

    resultsContainer.innerHTML = renderPageHeader(
        "",
        "Housing Help",
        "Find programs and local organizations that can help you find, buy or rent a place to live. Browse emergency shelter options, rental assistance programs and homeownership or mortgage resources. Listings include contact details and eligibility information so you can act quickly."
    );

    resources.forEach(resource => {

        const card = document.createElement("div");

        card.className = "resource-card";

        card.innerHTML = `
            <h3>${resource.organization}</h3>

            <p>${resource.summary || ""}</p>

            <p><strong>Urgency:</strong> ${resource.urgency || "Standard"}</p>
        `;

        resultsContainer.appendChild(card);
        card.addEventListener("click",() => showResourceDetails(resource)
        );
    });
}

function displayNews(newsItems) {
    newsResultsContainer.innerHTML =
        renderPageHeader(
            "",
            "Weekly News and Updates",
            "Stay up to date on the rules, public meetings and changes that affect housing, benefits and community services. Read the highlights and learn about important deadlines, new requirements and policy updates so you can participate and plan ahead. Use the source links to join discussions or get more information."
        ) + `
        <div class="ai-banner">
            <strong>✨ How AI will power this section</strong>
            Each week, AI will read new policy updates and community announcements, then rewrite them summarizing what changed, who it affects and why it matters to Wilmington residents.
            The cards below show how the output may appear.
        </div>
    `;

    newsItems.forEach(item => {
        const cats = (item.category_tags || []).join(" · ");
        const card = document.createElement("div");
        card.className = "resource-card";
        card.innerHTML = `
            <span class="urgency-tag urgency-standard">${cats || "General"}</span>
            <h3>${item.headline}</h3>
            <p>${item.summary}</p>
            <p><strong>Why this matters:</strong> ${item.why_it_matters}</p>
            <p style="font-size:0.8rem; color:#888;">
                ${item.source_name} · ${item.published}
            </p>
        `;
        newsResultsContainer.appendChild(card);
    });
}

function displayEssentials(resources) {

    essentialsResultsContainer.innerHTML = renderPageHeader(
        "",
        "Free / Low-Cost Essentials",
        "Check out these local programs and nonprofits offering furniture, utilities, repairs for free or at a low cost. Make your home more comfortable and safe with a few simple steps."
    );

    const grouped = {};
    resources.forEach(resource => {
        const cat = resource.category || "Other";
        if (!grouped[cat]) grouped[cat] = [];
        grouped[cat].push(resource);
    });

    Object.entries(grouped).forEach(([category, items]) => {
        essentialsResultsContainer.innerHTML += `
            <h3 class="category-group-header">${category}</h3>
        `;
        items.forEach(resource => {
            const card = document.createElement("div");
            card.className = "resource-card";
            card.innerHTML = `
                <h3>${resource.organization}</h3>
                <p>${resource.summary || ""}</p>
                <p><strong>Category:</strong> ${resource.category || ""}</p>
            `;
            essentialsResultsContainer.appendChild(card);
        });
    });
}

function showSeasonalResources() {
    resultsContainer.style.display = "none";
    newsResultsContainer.style.display = "none";
    essentialsResultsContainer.style.display = "none";
    detailScreen.style.display = "none";

    const flyers = [
        { src: "images/seasonal/1985.jpg", caption: "Community Opportunity" },
        { src: "images/seasonal/1987.jpg", caption: "Community Opportunity" },
        { src: "images/seasonal/1989.jpg", caption: "Volunteer Opportunity" },
        { src: "images/seasonal/1991.jpg", caption: "Fundraiser" },
        { src: "images/seasonal/1993.jpg", caption: "Community Opportunity" },
        { src: "images/seasonal/1995.jpg", caption: "Community Event" },
        { src: "images/seasonal/1997.jpg", caption: "Public Notice" },
    ];

    const carousel = document.getElementById("seasonal-carousel");
    carousel.innerHTML = "";

    flyers.forEach(flyer => {
        carousel.innerHTML += `
            <div class="carousel-card">
                <img src="${flyer.src}" alt="${flyer.caption}">
                <div class="carousel-caption">${flyer.caption}</div>
            </div>
        `;
    });

    seasonalResultsContainer.style.display = "block";
    showResultsScreen();
}

function showResourceDetails(resource) {

    const phone =
        resource.phones?.[0]?.number || "Not listed";

    const website =
        resource.websites?.[0]?.url || "";

    const location =
        resource.locations?.[0];

    detailView.innerHTML = `

        <div class="resource-card">

            <h2>${resource.organization}</h2>

            <p>
                <strong>Summary:</strong>
                ${resource.summary || ""}
            </p>

            <p>
                <strong>Description:</strong>
                ${resource.description || ""}
            </p>

            <p>
                <strong>Eligibility:</strong>
                ${resource.eligibility || "Not listed"}
            </p>

            <p>
                <strong>Category:</strong>
                ${resource.category || ""}
            </p>

            <p>
                <strong>Urgency:</strong>
                ${resource.urgency || "Standard"}
            </p>

            <p>
                <strong>Phone:</strong>
                ${phone}
            </p>

                ${location && !location.confidential && location.address
            ? `<p><strong>Address:</strong>
                ${location.address},
                ${location.city},
                ${location.state}
                ${location.zip || ""}
            </p>`
            : location && !location.confidential && location.city
            ? `<p><strong>Location:</strong>
                ${location.city},
                ${location.state}
            </p>`
            : ""
                }

            ${
                website
                    ? `
                    <p>
                        <strong>Website:</strong>
                        <a href="${website}" target="_blank">
                            Visit Website
                        </a>
                    </p>
                    `
                    : ""
            }

        </div>
    `;

    showDetailScreen();
}
