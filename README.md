# Distributed GoBang (Five-in-a-Row) Game System

This project is a full-stack implementation of the classic strategy game **GoBang** (also known as Five-in-a-Row or Gomoku), designed as a **distributed computational infrastructure**. It was developed as a **group assignment** to consolidate knowledge in distributed systems, network communication protocols, and web development technologies (**JSP**).

The project was divided into two phases: first, establishing the core distributed backend, and second, extending it with a web interface for global access.

---

## Group Contribution

This project was successfully completed as a **three-member group effort**. We collaboratively designed the system architecture, implemented the custom application protocol, built both the core server and the web application, and ensured full compliance with all required and optional parameters.

---

## Game Overview

GoBang is a two-player abstract strategy board game. The goal is to be the first player to form an unbroken line of five stones of your color (horizontally, vertically, or diagonally) on a 15x15 board.

---

## System Architecture

The solution employs a **distributed architecture** where a central **Game Server** manages all user data, game states, and communication, allowing players to connect via two distinct clients: a **Desktop/GUI Client** and a **Web Client** (built with **JSP**).

### Key Architectural Details

* **Transport Layer Protocols:**
    * **TCP:** Used for reliable communication (game moves, registration, login).
    * **UDP:** Used for specific, non-critical, real-time updates.
* **Data Management:** Player profiles and game history are maintained persistently on the server. Data integrity is ensured using well-defined structures (serialized objects and/or **XML documents with XSD schemas**).
* **Application Protocol:** A custom application-level protocol was defined to manage all client-server interactions (syntax, semantics, and timing).

---

## Core Features and Functionality

### Player Management

* **Self-Registration & Profile Management:** Users can register with full profiles (nickname, password, nationality, age, photo) and update their details.
* **Personalized Skins:** A user can select a **preferred background color/skin** for their profile, which impacts the entire platform's appearance.
* **Statistics Tracking:** The system persistently tracks **wins, losses, and average time spent per game**.

### Gameplay and Competition

* **Simultaneous Games:** Players can participate in **multiple games concurrently**.
* **Time Control:** A **30-second time limit** is enforced for each move.
* **Opponent Search:** Players can efficiently find opponents using an **AutoComplete** control based on the full player name.
* **Honor Board (Leaderboard):** A dynamic leaderboard displays rankings, featuring **photos** and **nationality flags**, sorted by:
    1.  **Victories** (primary sort).
    2.  **Average Time per Game** (tie-breaker: faster time ranks higher).

### Web Platform Enhancements

* The web solution, built with **JavaServer Pages (JSP)**, functions as a secondary client, proving that the distributed backend is robust and accessible over the Internet.
* **Data Backup:** A mechanism is implemented to guarantee the preservation and persistence of the Honor Board data.

---

## Technical Achievement Highlights

Our team successfully implemented **all mandatory and optional requirements**, showcasing competence in:

* Designing robust, concurrent, and distributed system architectures.
* Developing and managing custom network protocols.
* Full-stack development, integrating a **JSP/Web layer** with a dedicated **Java backend server**.
* Implementing advanced features like profile personalization, complex leaderboard logic, and data backup mechanisms.
