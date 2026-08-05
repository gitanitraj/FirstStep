// ResourceDetailModal.jsx

export function ResourceDetailModal({ resource, onClose, isOpen }) {
  const [activeTab, setActiveTab] = useState("details");
  const [selectedResource, setSelectedResource] = useState(resource);
  const [allResources, setAllResources] = useState([]);
  const [policyUpdates, setPolicyUpdates] = useState([]);
  const [filters, setFilters] = useState({
    category: null,
    community: null,
    cost: null,
    urgent: false,
    search: ""
  });

  // Fetch all matching resources on mount
  useEffect(() => {
    fetchAllMatchingResources(resource.category).then(setAllResources);
  }, [resource.category]);

  // Fetch policy updates whenever selected resource changes
  useEffect(() => {
    fetchPolicyUpdates(selectedResource.categoryTags).then(setPolicyUpdates);
  }, [selectedResource.id]);

  // When user clicks a resource in "All Results", update selection
  const handleSelectResource = (newResource) => {
    setSelectedResource(newResource);
    // Optionally switch to "Most Relevant" tab to show details
    // setActiveTab("details");
  };

  // Filter the list based on active filters
  const filteredResources = allResources.filter((r) => {
    if (filters.category && r.category !== filters.category) return false;
    if (filters.community && r.communityId !== filters.community) return false;
    if (filters.cost && r.cost !== filters.cost) return false;
    if (filters.urgent && !["emergency", "time-limited"].includes(r.urgency?.toLowerCase())) return false;
    if (filters.search) {
      const q = filters.search.toLowerCase();
      return (
        r.organization.toLowerCase().includes(q) ||
        r.summary?.toLowerCase().includes(q)
      );
    }
    return true;
  });

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div 
        className="modal-content modal-detail" 
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="modal-header">
          <button className="modal-back" onClick={onClose}>← Back</button>
          <h2 className="modal-title">{selectedResource.organization}</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        {/* Tabs */}
        <div className="modal-tabs">
          <button 
            className={`tab-button ${activeTab === "details" ? "active" : ""}`}
            onClick={() => setActiveTab("details")}
          >
            Most Relevant
          </button>
          <button 
            className={`tab-button ${activeTab === "all" ? "active" : ""}`}
            onClick={() => setActiveTab("all")}
          >
            All Results
            <span className="tab-badge">{allResources.length}</span>
          </button>
          <button 
            className={`tab-button ${activeTab === "policy" ? "active" : ""}`}
            onClick={() => setActiveTab("policy")}
          >
            Policy Updates
          </button>
        </div>

        {/* Content Area */}
        <div className="modal-body">
          {activeTab === "details" && (
            <ResourceDetailsTab 
              resource={selectedResource}
              policyUpdates={policyUpdates}
              allResourcesCount={allResources.length}
              onViewAll={() => setActiveTab("all")}
            />
          )}
          {activeTab === "all" && (
            <AllResultsTab 
              resources={filteredResources}
              selectedResource={selectedResource}
              onSelect={handleSelectResource}
              filters={filters}
              onFilterChange={setFilters}
            />
          )}
          {activeTab === "policy" && (
            <PolicyUpdatesTab 
              updates={policyUpdates}
              resource={selectedResource}
            />
          )}
        </div>

        {/* Sticky Action Bar */}
        <div className="modal-actions">
          <button className="action-call" onClick={() => callNumber(selectedResource)}>
            📞 Call
          </button>
          <button className="action-save" onClick={() => saveResource(selectedResource)}>
            💾 Save
          </button>
          <button className="action-share" onClick={() => shareResource(selectedResource)}>
            📤 Share
          </button>
        </div>
      </div>
    </div>
  );
}

// AllResultsTab.jsx
export function AllResultsTab({ resources, selectedResource, onSelect, filters, onFilterChange }) {
  const [sortBy, setSortBy] = useState("relevance");
  const [sortedResources, setSortedResources] = useState(resources);

  useEffect(() => {
    let sorted = [...resources];
    if (sortBy === "alphabetical") {
      sorted.sort((a, b) => a.organization.localeCompare(b.organization));
    } else if (sortBy === "updated") {
      sorted.sort((a, b) => new Date(b.retrieved) - new Date(a.retrieved));
    }
    // "relevance" is already sorted from backend
    setSortedResources(sorted);
  }, [resources, sortBy]);

  const getUniqueValues = (field) => [
    ...new Set(resources.map(r => r[field]).filter(Boolean))
  ].sort();

  const categories = getUniqueValues("category");
  const communities = getUniqueValues("communityId");
  const costs = getUniqueValues("cost");

  return (
    <div className="all-results-tab">
      {/* Filters (Sticky) */}
      <div className="results-filters">
        <div className="filter-row">
          {categories.length > 0 && (
            <select 
              value={filters.category || ""} 
              onChange={(e) => onFilterChange({ ...filters, category: e.target.value || null })}
              className="filter-select"
            >
              <option value="">All Categories</option>
              {categories.map(cat => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          )}

          {communities.length > 0 && (
            <select 
              value={filters.community || ""} 
              onChange={(e) => onFilterChange({ ...filters, community: e.target.value || null })}
              className="filter-select"
            >
              <option value="">All Communities</option>
              {communities.map(comm => (
                <option key={comm} value={comm}>{comm}</option>
              ))}
            </select>
          )}

          {costs.length > 0 && (
            <select 
              value={filters.cost || ""} 
              onChange={(e) => onFilterChange({ ...filters, cost: e.target.value || null })}
              className="filter-select"
            >
              <option value="">Any Cost</option>
              {costs.map(cost => (
                <option key={cost} value={cost}>{cost}</option>
              ))}
            </select>
          )}

          <label className="filter-checkbox">
            <input 
              type="checkbox" 
              checked={filters.urgent}
              onChange={(e) => onFilterChange({ ...filters, urgent: e.target.checked })}
            />
            Urgent Only
          </label>
        </div>

        <div className="filter-row">
          <input 
            type="text" 
            placeholder="Search within results..."
            value={filters.search}
            onChange={(e) => onFilterChange({ ...filters, search: e.target.value })}
            className="filter-search"
          />

          <select 
            value={sortBy} 
            onChange={(e) => setSortBy(e.target.value)}
            className="filter-sort"
          >
            <option value="relevance">Sort: Relevance</option>
            <option value="alphabetical">Alphabetical</option>
            <option value="updated">Recently Updated</option>
          </select>
        </div>
      </div>

      {/* Results Count */}
      <div className="results-meta">
        <p>
          {sortedResources.length} result{sortedResources.length !== 1 ? "s" : ""}
        </p>
      </div>

      {/* List */}
      <div className="results-list">
        {sortedResources.length === 0 ? (
          <p className="results-empty">No resources match your filters.</p>
        ) : (
          sortedResources.map((r) => (
            <div
              key={r.id}
              className={`results-item ${r.id === selectedResource.id ? "active" : ""}`}
              onClick={() => onSelect(r)}
            >
              <div className="results-item-header">
                <h4 className="results-item-title">{r.organization}</h4>
                {r.urgency && (
                  <span className={`urgency-tag ${urgencyClass(r.urgency)}`}>
                    {r.urgency}
                  </span>
                )}
              </div>
              <p className="results-item-summary">{r.summary}</p>
              <div className="results-item-meta">
                {r.category && <span>{r.category}</span>}
                {r.cost && <span>{r.cost}</span>}
                {r.retrieved && (
                  <span className="results-item-date">
                    Updated: {new Date(r.retrieved).toLocaleDateString()}
                  </span>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>

// ResourceDetailsTab.jsx (updated)
export function ResourceDetailsTab({ 
  resource, 
  policyUpdates, 
  allResourcesCount,
  onViewAll 
}) {
  return (
    <div className="detail-layout desktop-split">
      {/* Left: Main Content */}
      <div className="detail-main">
        <section className="detail-section">
          <h3>About</h3>
          <p>{resource.description || resource.summary}</p>
        </section>

        <section className="detail-section">
          <h3>Who Qualifies</h3>
          <p>{resource.eligibility || "See website for details"}</p>
        </section>

        <section className="detail-section">
          <h3>How to Apply</h3>
          <div className="contact-block">
            {resource.phones?.[0] && (
              <p>
                <strong>📞 Call:</strong>{" "}
                <a href={`tel:${resource.phones[0].number}`}>
                  {resource.phones[0].number}
                </a>
              </p>
            )}
            {resource.websites?.[0] && (
              <p>
                <strong>🌐 Visit:</strong>{" "}
                <a href={resource.websites[0].url} target="_blank" rel="noopener">
                  {resource.websites[0].url}
                </a>
              </p>
            )}
            {resource.locations?.[0] && (
              <p>
                <strong>📍 Location:</strong> {resource.locations[0].address}
              </p>
            )}
          </div>
        </section>

        {/* Call to action: Browse all */}
        {allResourcesCount > 1 && (
          <div className="detail-browse-all">
            <p>See {allResourcesCount - 1} more matching resources</p>
            <button onClick={onViewAll} className="browse-all-btn">
              View All Results →
            </button>
          </div>
        )}
      </div>

      {/* Right: Sidebar (Desktop only) */}
      <aside className="detail-sidebar desktop-only">
        <div className="policy-context-box">
          <h4>🔄 Policy Context</h4>
          {policyUpdates.length > 0 ? (
            <ul className="policy-list">
              {policyUpdates.slice(0, 3).map((update) => (
                <li key={update.id} className="policy-item">
                  <div className="policy-headline">{update.headline}</div>
                  <div className="policy-date">{update.published}</div>
                  <a href={update.sourceUrl} target="_blank" rel="noopener">
                    Read More →
                  </a>
                </li>
              ))}
            </ul>
          ) : (
            <p className="policy-empty">No recent policy updates.</p>
          )}
          <button className="follow-updates-btn">
            🔔 Follow Updates
          </button>
        </div>
      </aside>
    </div>
  );
}