<!-- Home Screen -->
<section id="home-screen">

    <div class="hero-section">
        <h2 class="hero-title" data-i18n="heroTitle">What do you need help with today?</h2>
        <p class="hero-subtitle" data-i18n="heroSubtitle">Find housing, essentials, community programs and local updates in one trusted place.</p>
    </div>

    <div class="home-grid">

        <div class="home-main">
            
            <!-- NEW: Sticky Refinement Bar -->
            <div class="sticky-refinement-bar">
                <div class="refinement-content">
                    <input type="text" id="home-search" placeholder="🔍 Search resources..." class="refinement-search" />
                    <select id="home-category-filter" class="refinement-select">
                        <option value="">All Categories</option>
                        <option value="housing">Housing</option>
                        <option value="food">Food & Groceries</option>
                        <option value="essentials">Essentials</option>
                        <option value="health">Health Services</option>
                    </select>
                    <select id="home-cost-filter" class="refinement-select">
                        <option value="">Any Cost</option>
                        <option value="free">Free Only</option>
                        <option value="low-cost">Low-Cost</option>
                    </select>
                    <label class="refinement-checkbox">
                        <input type="checkbox" id="home-urgent-filter" />
                        <span>Urgent Only</span>
                    </label>
                </div>
            </div>

            <!-- NEW: Collections Grid -->
            <section id="collections-grid" class="collections-grid">
                <h3 class="collections-title">Browse by Collection</h3>
                <div class="collections-container">
                    <button class="collection-card" data-collection="housing">
                        <div class="collection-icon">🏠</div>
                        <div class="collection-info">
                            <h4>Housing & Rental Assistance</h4>
                            <p class="collection-count">18 resources</p>
                            <span class="collection-cta">Browse →</span>
                        </div>
                    </button>

                    <button class="collection-card" data-collection="essentials">
                        <div class="collection-icon">🛒</div>
                        <div class="collection-info">
                            <h4>Free / Low-Cost Essentials</h4>
                            <p class="collection-count">12 resources</p>
                            <span class="collection-cta">Browse →</span>
                        </div>
                    </button>

                    <button class="collection-card" data-collection="food">
                        <div class="collection-icon">🍽️</div>
                        <div class="collection-info">
                            <h4>Food & Groceries Near You</h4>
                            <p class="collection-count">8 resources</p>
                            <span class="collection-cta">Browse →</span>
                        </div>
                    </button>
                </div>
                <p class="collections-fallback">Or scroll below for more categories</p>
            </section>

            <!-- NEW: Collections Expanded View (Hidden by default) -->
            <section id="collection-detail" style="display:none;" class="collection-detail">
                <div class="collection-detail-header">
                    <button class="close-collection-detail">← Back to Collections</button>
                    <h3 id="collection-detail-title"></h3>
                </div>
                <div class="collection-detail-content">
                    <div id="collection-detail-list"></div>
                </div>
            </section>

            <!-- EXISTING: Traditional Category Cards -->
            <section class="category-list" id="traditional-categories">

                <button id="housing-help-button" class="category-card">
                    <div class="category-icon">🏠</div>
                    <div>
                        <h3 data-i18n="housingHelp">Housing Help</h3>
                        <p data-i18n="housingHelpSub">Home and Rental Assistance, Shelters, Home Repairs</p>
                    </div>
                </button>

                <button id="essentials-button" class="category-card">
                    <div class="category-icon">🛒</div>
                    <div>
                        <h3 data-i18n="essentials">Free / Low-Cost Essentials</h3>
                        <p data-i18n="essentialsSub">Food, Furniture, Clothing</p>
                    </div>
                </button>

                <button id="seasonal-resources-button" class="category-card">
                    <div class="category-icon">📅</div>
                    <div>
                        <h3 data-i18n="seasonal">Community Resources</h3>
                        <p data-i18n="seasonalSub">Programs, Events and Community Opportunities</p>
                    </div>
                </button>

                <button id="weekly-updates-button-home" class="category-card">
                    <div class="category-icon">📰</div>
                    <div>
                        <h3 data-i18n="weeklyUpdates">Weekly Updates</h3>
                        <p data-i18n="weeklyUpdatesSub">News and Changes Impacting You</p>
                    </div>
                </button>

            </section>

            <!-- EXISTING: AI Guidance -->
            <section id="ai-guidance-home" class="ai-guidance-section">
                <!-- (unchanged) -->
            </section>

        </div>

        <!-- EXISTING: Sidebar -->
        <aside class="home-side">
            <!-- (unchanged) -->
        </aside>

    </div>

</section>