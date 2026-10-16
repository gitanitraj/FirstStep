// Self-hosted fonts, latin subset only, exactly the FOUR faces the role map
// uses. Montserrat 600 was imported and then removed: Option B deleted the
// displayCard role, and every remaining Montserrat role is 700. An unused face
// is 19KB nobody downloads for a reason. No CDN: index.html loads nothing external, so a resident's browser
// makes no third-party request revealing which pages they read, and the app
// renders identically offline and inside the container.
import '@fontsource/montserrat/latin-700.css';
import '@fontsource/open-sans/latin-400.css';
import '@fontsource/open-sans/latin-600.css';
import '@fontsource/open-sans/latin-700.css';

import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
