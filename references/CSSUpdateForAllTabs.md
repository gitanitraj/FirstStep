/* All Results Tab */
.all-results-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.results-filters {
  background: #f9f9f9;
  border-bottom: 1px solid #e0e0e0;
  padding: 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-select,
.filter-search,
.filter-sort {
  padding: 8px 12px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  font-size: 13px;
  background: white;
}

.filter-select {
  flex: 0 1 140px;
  min-width: 120px;
}

.filter-search {
  flex: 1;
  min-width: 150px;
}

.filter-sort {
  flex: 0 1 150px;
}

.filter-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

.filter-checkbox input[type="checkbox"] {
  cursor: pointer;
}

.results-meta {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  font-size: 13px;
  color: #666;
}

.results-meta p {
  margin: 0;
}

.results-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.results-item {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  cursor: pointer;
  transition: all 0.2s;
}

.results-item:hover {
  background: #f5f5f5;
}

.results-item.active {
  background: #e3f2fd;
  border-left: 4px solid #0066cc;
  padding-left: 12px;
}

.results-item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}

.results-item-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.results-item-summary {
  margin: 0 0 8px 0;
  font-size: 13px;
  line-height: 1.4;
  color: #666;
}

.results-item-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #999;
  flex-wrap: wrap;
}

.results-item-date {
  color: #999;
}

.results-empty {
  padding: 32px 16px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

/* Browse All CTA in details view */
.detail-browse-all {
  background: #f5f5f5;
  border-left: 4px solid #ffc107;
  padding: 16px;
  border-radius: 4px;
  margin-top: 20px;
}

.detail-browse-all p {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #666;
}

.browse-all-btn {
  width: 100%;
  padding: 10px;
  background: #ffc107;
  color: #333;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.browse-all-btn:hover {
  background: #ffb300;
}

/* Tab badge showing count */
.tab-badge {
  display: inline-block;
  background: #0066cc;
  color: white;
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  margin-left: 6px;
}