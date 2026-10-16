// ===== Collections Grid =====
const collectionsGrid = document.getElementById("collections-grid");
const collectionDetail = document.getElementById("collection-detail");
const collectionDetailTitle = document.getElementById("collection-detail-title");
const collectionDetailList = document.getElementById("collection-detail-list");
const closeCollectionDetailBtn = document.querySelector(".close-collection-detail");

// Map collection IDs to resource filters
const collectionFilters = {
  housing: (r) => r.category && r.category.toLowerCase().includes("housing"),
  essentials: (r) => r.cost && r.cost.toLowerCase() === "free",
  food: (r) => r.category && r.category.toLowerCase().includes("food")
};

// Map collection IDs to display names
const collectionNames = {
  housing: "Housing & Rental Assistance",
  essentials: "Free / Low-Cost Essentials",
  food: "Food & Groceries Near You"
};

// Fetch initial resource counts for collection cards
async function initializeCollections() {
  try {
    const response = await fetch("/api/resources");
    const resources = await response.json();

    document.querySelectorAll(".collection-card").forEach((card) => {
      const collectionId = card.getAttribute("data-collection");
      const filterFn = collectionFilters[collectionId];
      const matching = resources.filter(filterFn);
      
      const countEl = card.querySelector(".collection-count");
      if (countEl) {
        countEl.textContent = `${matching.length} resource${matching.length !== 1 ? "s" : ""}`;
      }
    });
  } catch (error) {
    console.error("Failed to initialize collections:", error);
  }
}

// Handle collection card click
document.querySelectorAll(".collection-card").forEach((card) => {
  card.addEventListener("click", async () => {
    const collectionId = card.getAttribute("data-collection");
    const displayName = collectionNames[collectionId];
    
    try {
      const response = await fetch("/api/resources");
      const resources = await response.json();
      const filterFn = collectionFilters[collectionId];
      const filtered = resources.filter(filterFn);

      // Render collection detail view
      collectionDetailTitle.textContent = displayName;
      collectionDetailList.innerHTML = "";

      if (filtered.length === 0) {
        collectionDetailList.innerHTML = `
          <p class="collection-empty" style="text-align: center; color: #999; padding: 20px;">
            No resources found in this collection.
          </p>
        `;
      } else {
        filtered.forEach((resource) => {
          const phone = resource.phones?.[0]?.number;
          const location = resource.locations?.[0];

          const item = document.createElement("div");
          item.className = "collection-item";
          item.innerHTML = `
            <div class="collection-item-header">
              <h4 class="collection-item-title">${resource.organization}</h4>
              ${resource.urgency ? `<span class="urgency-tag ${urgencyClass(resource.urgency)}">${resource.urgency}</span>` : ""}
            </div>
            <p class="collection-item-summary">${resource.summary || ""}</p>
            <div class="collection-item-actions">
              ${phone ? `
                <a href="tel:${phone}" class="collection-item-action primary" onclick="event.stopPropagation()">
                  📞 Call
                </a>
              ` : ""}
              ${location && location.address ? `
                <a href="https://maps.google.com/maps?q=${encodeURIComponent(location.address)}" target="_blank" rel="noopener" class="collection-item-action" onclick="event.stopPropagation()">
                  📍 Directions
                </a>
              ` : ""}
              <button class="collection-item-action" onclick="event.stopPropagation(); saveResource(${JSON.stringify(resource).replace(/"/g, '&quot;')})">
                💾 Save
              </button>
            </div>
          `;
          
          // Also allow clicking the item to open detail modal
          item.addEventListener("click", () => showResourceDetails(resource));
          collectionDetailList.appendChild(item);
        });
      }

      // Show collection detail, hide grid
      collectionsGrid.style.display = "none";
      collectionDetail.style.display = "block";
      window.scrollTo(0, 0);

    } catch (error) {
      console.error("Failed to load collection:", error);
    }
  });
});

// Close collection detail view
closeCollectionDetailBtn?.addEventListener("click", () => {
  collectionDetail.style.display = "none";
  collectionsGrid.style.display = "block";
});

// Refinement filters (home page)
const homeSearchInput = document.getElementById("home-search");
const homeCategoryFilter = document.getElementById("home-category-filter");
const homeCostFilter = document.getElementById("home-cost-filter");
const homeUrgentFilter = document.getElementById("home-urgent-filter");

function applyHomeFilters() {
  const searchTerm = (homeSearchInput?.value || "").toLowerCase();
  const category = homeCategoryFilter?.value || "";
  const cost = homeCostFilter?.value || "";
  const urgentOnly = homeUrgentFilter?.checked || false;

  document.querySelectorAll(".collection-card").forEach((card) => {
    const collectionId = card.getAttribute("data-collection");
    let show = true;

    // Filter by category
    if (category && collectionId !== category) {
      show = false;
    }

    // Filter by cost (simplified—assumes collections have cost info)
    // Note: This is a simplified example; you'd need richer metadata on collections

    card.style.display = show ? "block" : "none";
  });
}

homeSearchInput?.addEventListener("input", applyHomeFilters);
homeCategoryFilter?.addEventListener("change", applyHomeFilters);
homeCostFilter?.addEventListener("change", applyHomeFilters);
homeUrgentFilter?.addEventListener("change", applyHomeFilters);

// Initialize on page load
window.addEventListener("DOMContentLoaded", () => {
  initializeCollections();
});

// Helper function to save a resource (to be implemented)
function saveResource(resource) {
  console.log("Saving resource:", resource);
  // TODO: Implement save to local storage or backend
}