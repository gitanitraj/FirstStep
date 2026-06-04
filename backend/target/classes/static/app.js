const housingButton =
    document.getElementById("housing-help-button");

const resultsContainer =
    document.getElementById("results");

const filterScreen =
    document.getElementById("filter-screen");

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


housingButton.addEventListener("click", () => {

    filterScreen.style.display = "block";

    resultsContainer.innerHTML = "";
});

    continueButton.addEventListener("click", () => {

    filterScreen.style.display = "none";

    loadHousingResources();
});

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

async function loadHousingResources() {

    resultsContainer.innerHTML = "<p>Loading...</p>";

    try {

        const response = await fetch("/api/resources");
        const resources = await response.json();

        const housingResources = resources.filter(resource =>
            resource.category &&
            resource.category.toLowerCase().includes("housing")
        );

        displayResources(housingResources);

    } catch (error) {

        console.error(error);

        resultsContainer.innerHTML =
            "<p>Unable to load resources.</p>";
    }
}

async function loadEssentialsResources() {

    essentialsResultsContainer.innerHTML =
        "<p>Loading...</p>";

    try {

        const response =
            await fetch("/api/resources");

        const resources =
            await response.json();

        const essentialsResources =
            resources.filter(resource =>
                resource.cost &&
                resource.cost.toLowerCase() === "free"
            );

        displayEssentials(essentialsResources);

    } catch (error) {

        console.error(error);

        essentialsResultsContainer.innerHTML =
            "<p>Unable to load resources.</p>";
    }
}

async function loadNewsUpdates() {

    newsResultsContainer.innerHTML =
        "<p>Loading updates...</p>";

    try {

        const response =
            await fetch("/api/news");

        const newsItems =
            await response.json();

        displayNews(newsItems);

    } catch (error) {

        console.error(error);

        newsResultsContainer.innerHTML =
            "<p>Unable to load updates.</p>";
    }
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

        essentialsResultsContainer
            .appendChild(card);
    });
}

function showSeasonalResources() {

    seasonalResultsContainer.innerHTML = `
        <div class="resource-card">
            <h3>Seasonal Resources</h3>

            <p>
                Coming Soon
            </p>

            <p>
                Information about seasonal programs, events, community resources and
                organization flyers will be posted here.
            </p>
        </div>
    `;
}
}