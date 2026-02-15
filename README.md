# System-testing-prestashop
Software Quality Engineering - System & Automation Testing
The project integrates advanced combinatorial test design with behavior-driven development (BDD) and automated UI testing.

Project Overview
The project demonstrates a full QA lifecycle for an E-commerce platform (PrestaShop):
Combinatorial Test Design: Using the ACTS tool to generate optimized test suites that cover complex interactions between system parameters.
Automated E2E Testing: Implementing automated scenarios using Selenium WebDriver and Cucumber, ensuring the system behaves correctly under various business conditions.

Tech Stack & Tools
ACTS Tool: For generating t-way combinatorial covering arrays to optimize test coverage.
Java: The primary language used for the automation framework.
Selenium WebDriver: For browser automation and UI interaction.
Cucumber & JUnit: For BDD-style testing, allowing scenarios to be written in Gherkin and executed via Java.

Key Components
1. Combinatorial Design (ACTS)
The ACTS/ directory includes the input models and the generated test sets. These sets ensure that we test significant combinations of features like user types (Guest/VIP), shipping methods, and payment options without needing to test every single possibility.

2. Automation Framework (Java Implementation)
The automation logic is built upon two core files:

StepDefinitions.java: The heart of the automation. It maps Gherkin steps to executable Java code. It includes:
Dynamic navigation and element interaction using WebDriverWait.
Complex business logic verification, such as VIP discount calculations and Self-pickup (no shipping cost) logic.
Error handling and synchronization to ensure stable test execution.
RunCucumberTest.java: The entry point for the test suite. It uses the JUnit Platform Suite to orchestrate the execution of Cucumber feature files and generate detailed execution reports.

Testing Scenarios
The automated suite covers critical user journeys, including:
Full checkout process from cart to payment.
Verification of price accuracy for different shipping tiers.
Validation of "Self-pickup" logic (ensuring shipping costs are correctly omitted).
User registration and address management.
