/* Layout: Sidebar + Main */
.home-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 32px;
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

/* Sidebar */
.home-sidebar {
  position: sticky;
  top: 80px; /* Below header */
  height: fit-content;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: #666;
  margin: 0;
}

.category-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 14px;
  cursor: pointer;
}

.category-checkbox input {
  cursor: pointer;
}

.category-count {
  color: #999;
  font-size: 13px;
  margin-left: auto;
}

/* Main Content */
.home-main {
  display: flex;
  flex-direction: column;
  gap: 40px;
}

/* AI Widget */
.ai-guidance-card {
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  border: 1px solid #bbdefb;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.ai-guidance-card h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

/* Resource Card */
.resource-card-vertical {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-card-vertical:hover {
  border-color: #0066cc;
  box-shadow: 0 4px 16px rgba(0, 102, 204, 0.12);
}

.resource-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.resource-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.verified-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #4caf50;
  font-weight: 500;
  background: #f1f8f4;
  padding: 4px 8px;
  border-radius: 4px;
}

.resource-meta {
  font-size: 12px;
  color: #999;
}

.resource-description {
  font-size: 13px;
  line-height: 1.5;
  color: #666;
  margin: 0;
}

.resource-actions {
  display: flex;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.action-button {
  flex: 1;
  padding: 8px;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s;
}

.action-button.primary {
  background: #0066cc;
  color: white;
  border-color: #0066cc;
}

.action-button:hover {
  border-color: #0066cc;
  color: #0066cc;
}

/* Category Group */
.category-group {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-group-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-icon {
  font-size: 28px;
}

.category-group-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.category-group-meta {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.category-group-description {
  font-size: 13px;
  line-height: 1.4;
  color: #666;
  margin: 0;
}

.browse-button {
  align-self: flex-start;
  padding: 10px 16px;
  background: none;
  border: 1px solid #d0d0d0;
  border-radius: 4px;
  color: #0066cc;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.browse-button:hover {
  background: #f0f6ff;
  border-color: #0066cc;
}

/* Mobile */
@media (max-width: 768px) {
  .home-layout {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 16px;
  }

  .home-sidebar {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 12px;
  }

  .sidebar-section {
    flex: 1;
    min-width: 150px;
  }
}