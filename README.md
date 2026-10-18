# Smart Warehouse Management System

## Project Overview

The Smart Warehouse Management System is a Java-based application designed to manage warehouse operations, customer orders, inventory, and robot task scheduling. It integrates database management with scheduling logic and an AI-based decision support component to improve warehouse task management.

## Technologies Used

- **Java Swing** – Graphical user interface and warehouse management operations.
- **MySQL** – Database management for users, products, orders, inventory, and warehouse tasks.
- **Python** – AI-based decision support and prediction services.
- **TabNet** – Used for demand prediction and intelligent task scheduling recommendations.
- **JDBC** – Connects the Java application to the MySQL database.
- **Operating System Concepts** – Task scheduling, resource management, and robot simulation.

## Key Features

- User login and authentication.
- Product and inventory management.
- Shopping cart and order processing.
- Order tracking and management.
- Robot simulation and task scheduling.
- AI-based demand prediction and stockout risk analysis.
- Scheduling recommendations using algorithms such as FCFS, SJF, Priority Scheduling, and Round Robin.
- MySQL database integration.

## Project Structure

- `Main.java` – Application entry point.
- `LoginFrame.java` – Login interface.
- `AdminDashboard.java` – Admin dashboard.
- `UserDashboard.java` – User dashboard.
- `ProductDAO.java` – Product database operations.
- `OrderDAO.java` – Order database operations.
- `RobotDAO.java` – Robot-related database operations.
- `Scheduler.java` – Task scheduling logic.
- `TabNetClient.java` – Communication with the AI prediction service.
- `DBConnection.java` – MySQL database connection.

## Prerequisites

- Java Development Kit (JDK)
- MySQL Server
- MySQL Connector/J JDBC driver
- Python, if using the AI prediction service
- Required Python packages and a configured TabNet model, if applicable

## Setup and Execution

1. Install Java and MySQL on your system.
2. Create the `smart_warehouse` database and configure the required tables.
3. Configure the database connection in `DBConnection.java`.
4. Add the MySQL Connector/J driver to the Java classpath.
5. Start the Python AI prediction service if you want to use AI-based predictions.
6. Compile the Java source files with the required dependencies.
7. Run the `com.smartwarehouse.Main` class to launch the application.

## Project Objective

The main objective is to improve warehouse management by combining database operations, task scheduling, robot simulation, and AI-based decision support in a single application.

## Future Scope

- Improve demand forecasting using trained machine learning models.
- Add real-time warehouse monitoring.
- Enhance robot coordination and resource allocation.
- Improve reporting and performance analysis.

## Author

Developed as an academic project.
