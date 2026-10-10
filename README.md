# Nada Networks — People API

A REST API built with Java and Spring Boot to manage people, their mutual connections, and the shortest connection path between them.

## Features

- Create people with unique IDs.
- Establish mutual connections between people.
- Retrieve a person's direct contacts.
- Find the shortest path between two people, up to a maximum of 3 hops.
- Handle invalid requests and unknown person IDs with appropriate HTTP status codes.
- Store data in memory using Java collections.

## Tech Stack

- **Java:** 17
- **Framework:** Spring Boot
- **Build Tool:** Maven
- **Testing:** JUnit 5
- **Storage:** In-memory Java collections

## Getting Started

### Prerequisites

- Java 17 or later
- Maven

### Run the Server

Clone the repository and navigate to the project directory:

```bash
git clone https://github.com/masaladosa619/nada-people-api
cd nada-people-api
```

Start the application:

```bash
mvn spring-boot:run
```

The server starts on port **8080** by default.

Base URL: `http://localhost:8080`

### Run the Tests

Execute the automated tests with:

```bash
mvn test
```

The tests cover both sides of the three-hop limit:

- A path of exactly 3 hops is considered connected.
- A path requiring more than 3 hops is considered not connected.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/people` | Create a person |
| `POST` | `/knows` | Create a mutual connection |
| `GET` | `/people/{id}/contacts` | Retrieve direct contacts |
| `GET` | `/path?from={id}&to={id}` | Find the shortest path within 3 hops |

### Example Requests

**1. Create a person**

`POST /people`

Request body:

```json
{
  "name": "Asha"
}
```

Returns `201 Created` with the person's ID and name.

**2. Connect two people**

`POST /knows`

Request body:

```json
{
  "person1": 1,
  "person2": 2
}
```

Creates a mutual connection and returns `204 No Content`. Repeating an existing connection does not create a duplicate.

**3. Get direct contacts**

`GET /people/1/contacts`

Returns a JSON array containing the person's direct contacts.

**4. Find the shortest path**

`GET /path?from=1&to=4`

A successful path returns `connected: true`, the number of hops, and the ordered list of people in the path. If the destination cannot be reached within 3 hops, the response indicates `connected: false`.

## Design Decisions and Limitations

- People and connections are stored using in-memory Java collections.
- Connections are mutual, and duplicate connections are ignored.
- The shortest path is found using Breadth-First Search (BFS).
- The maximum path length is currently fixed at 3 hops.
- Data is not persisted, so it is lost when the application restarts.

## What I Would Improve With More Time

- **Authentication and authorization:** Secure the API and control access to its endpoints.
- **Persistent storage:** Integrate a database so people and connections survive application restarts.
- **Richer person details:** Store additional information about each person instead of only their ID and name.
- **Configurable hop limit:** Allow clients to specify the maximum number of hops through a query parameter instead of hardcoding it to 3.

## AI Tools Used

The following AI tools were used during development:

- ChatGPT
- Claude
- Antigravity CLI

These tools assisted with development, debugging, and understanding implementation details.
