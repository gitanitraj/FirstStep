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


housingButton.addEventListener("click", () => {

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

function displayResources(resources) {

    resultsContainer.innerHTML = "";

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

    newsResultsContainer.innerHTML = "";

    newsItems.forEach(item => {

        const card =
            document.createElement("div");

        card.className = "resource-card";

        card.innerHTML = `
            <h3>${item.headline}</h3>

            <p>${item.summary}</p>

            <p>
                <strong>Why This Matters:</strong>
                ${item.whyItMatters}
            </p>

            <p>
                <strong>Urgency:</strong>
                ${item.urgency}
            </p>
        `;

        newsResultsContainer.appendChild(card);
    });
}

function displayEssentials(resources) {

    essentialsResultsContainer.innerHTML = "";

    resources.forEach(resource => {

        const card =
            document.createElement("div");

        card.className = "resource-card";

        card.innerHTML = `
            <h3>${resource.organization}</h3>

            <p>${resource.summary || ""}</p>

            <p>
                <strong>Category:</strong>
                ${resource.category || ""}
            </p>
        `;

        essentialsResultsContainer.appendChild(card);
    });
}

function showSeasonalResources() {

    seasonalResultsContainer.innerHTML = `
        <div class="resource-card">

            <h3>Seasonal Resources</h3>

            <p>Coming Soon</p>

            <p>
                Information about seasonal programs,
                events, community resources, announcements,
                and organization flyers will be posted here.
            </p>

        </div>
    `;
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

            ${
                location
                    ? `
                    <p>
                        <strong>Address:</strong>
                        ${location.address},
                        ${location.city},
                        ${location.state}
                    </p>
                    `
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
