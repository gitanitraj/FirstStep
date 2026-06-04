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