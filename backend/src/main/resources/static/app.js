const housingButton = document.getElementById("housing-help-button");
const resultsContainer = document.getElementById("results");

const filterScreen =
    document.getElementById("filter-screen");

housingButton.addEventListener("click", () => {

    filterScreen.style.display = "block";

    resultsContainer.innerHTML = "";
});

const continueButton =
    document.getElementById("continue-button");

continueButton.addEventListener("click", () => {

    filterScreen.style.display = "none";

    loadHousingResources();
});

const weeklyUpdatesButton =
    document.getElementById("weekly-updates-button");

const newsResultsContainer =
    document.getElementById("news-results");

weeklyUpdatesButton.addEventListener(
    "click",
    loadNewsUpdates
);

const seasonalButton =
    document.getElementById("seasonal-resources-button");

const seasonalResultsContainer =
    document.getElementById("seasonal-results");

seasonalButton.addEventListener(
    "click",
    showSeasonalResources
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

function showSeasonalResources() {

    seasonalResultsContainer.innerHTML = `
        <div class="resource-card">
            <h3>Seasonal Resources</h3>

            <p>
                Coming Soon
            </p>

            <p>
                Future versions will display
                seasonal programs, events,
                community resources, and
                organization flyers.
            </p>
        </div>
    `;
}
}