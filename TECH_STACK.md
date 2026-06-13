# BookBridge Tech Stack

This document outlines the technology stack used in the BookBridge project, divided into frontend and backend/database.

## Frontend
The frontend is a modern single-page application built with React and Vite.

*   **Core Library:** React 19
*   **Build Tool:** Vite
*   **Language:** TypeScript
*   **Styling:** Tailwind CSS (v4) with PostCSS
*   **Routing:** React Router DOM (v7)
*   **Data Fetching & State:** React Query (`@tanstack/react-query`)
*   **HTTP Client:** Axios
*   **Icons:** Lucide React
*   **Code Quality:** ESLint & TypeScript ESLint

## Backend & Database
The backend is a RESTful API built with Node.js and Express, connecting to a MySQL database.

*   **Runtime Environment:** Node.js
*   **Framework:** Express.js
*   **Database:** MySQL (interfaced via the `mysql2` driver)
*   **Authentication & Authorization:** 
    *   Passport.js with Google OAuth 2.0 (`passport-google-oauth20`)
    *   JSON Web Tokens (`jsonwebtoken`)
*   **API Documentation:** Swagger / OpenAPI (`swagger-jsdoc` & `swagger-ui-express`)
*   **Security & Middleware:**
    *   `helmet` (Security headers)
    *   `cors` (Cross-Origin Resource Sharing)
    *   `express-rate-limit` (DDoS/Brute-force protection)
    *   `express-validator` (Request data validation)
*   **Logging:** `morgan`
