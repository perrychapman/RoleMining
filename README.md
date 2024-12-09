
# RoleRecommendation README

## Overview
The **RoleRecommendation** program is a Java-based application that utilizes Apache Spark and its MLlib library to generate role-based entitlement recommendations and association rules from user entitlement data. The program processes entitlement data, applies the FP-Growth algorithm to identify frequent itemsets and generates CSV files with role recommendations and association rules.

---

## Features
1. **Entitlement Frequency Analysis**:
   - Counts the frequency of each entitlement and filters those that appear in more than 80% of users.
   
2. **Transaction Filtering**:
   - Groups user entitlements into unique, filtered sets for processing.

3. **Frequent Pattern Mining**:
   - Uses the FP-Growth algorithm to find frequent entitlement itemsets and association rules.

4. **Role Recommendations**:
   - Generates a list of recommended roles based on frequent itemsets with 3 or more entitlements.

5. **Association Rules Generation**:
   - Creates association rules with confidence scores for analyzed entitlement patterns.

6. **CSV Outputs**:
   - Produces two CSV files:
     - `RoleRecommendations.csv`: Contains recommended roles and their associated entitlements.
     - `AssociationRules.csv`: Contains antecedent-consequent relationships with confidence levels.

---

## Prerequisites
- **Java Development Kit (JDK) 8 or later**
- **Apache Spark 3.0 or later**
- **Scala Library (Compatible with Spark Version)**
- Maven or any other build tool for dependency management

### Dependencies
- **Apache Spark MLlib**: For FP-Growth model and data processing.
- **Apache Commons CSV**: (Optional for extended CSV operations).
- **Scala Collection Converters**: For seamless Java-Scala interoperability.

---

## Input Requirements
The program processes a CSV file named `UserEntitlement.csv` with the following structure:
- **Columns**: `User`, `Entitlement`
- **Description**: Each row represents a user and an associated entitlement.

### Example Input
```csv
User,Entitlement
user1,entitlementA
user1,entitlementB
user2,entitlementA
user3,entitlementC
```

---

## Outputs
### 1. **RoleRecommendations.csv**
Contains role-based recommendations derived from frequent entitlement itemsets.
- **Columns**: `Role Recommendation`, `Associated Entitlements`, `Frequency`
- **Description**: Each role recommendation includes a list of entitlements and their occurrence frequency.

### 2. **AssociationRules.csv**
Contains association rules generated from the FP-Growth model.
- **Columns**: `Antecedent`, `Consequent`, `Confidence`
- **Description**: Shows relationships between entitlement sets with confidence levels.

---

## How to Run
1. **Setup Apache Spark**:
   - Download and configure Spark for your environment.
   - Ensure the `SPARK_HOME` environment variable is set.

2. **Prepare Input Data**:
   - Place `UserEntitlement.csv` in the directory `C:\Java\FpGrowthDemo\`.

3. **Compile and Run**:
   - Compile the program using a build tool like Maven or Gradle.
   - Execute the program with sufficient memory allocation:
     ```bash
     java -Xmx16g -Xms16g -cp target/RoleRecommendation.jar RoleRecommendation
     ```

4. **View Results**:
   - Check the output files in the `C:\Java\FpGrowthDemo\` directory:
     - `RoleRecommendations.csv`
     - `AssociationRules.csv`

---

## Configuration Options
- **Memory Allocation**:
  Adjust memory settings in the code or via JVM arguments to handle larger datasets:
  ```java
  .config("spark.driver.memory", "16g")
  .config("spark.executor.memory", "16g")
  ```

- **Thresholds**:
  Modify the following parameters for custom frequency or confidence levels:
  - **Minimum Support**: `setMinSupport(0.01)`
  - **Minimum Confidence**: `setMinConfidence(0.8)`

---

## Example Output
### RoleRecommendations.csv
```csv
Role Recommendation,Associated Entitlements,Frequency
role1,entitlementA,entitlementB,entitlementC,150
role2,entitlementA,entitlementB,100
```

### AssociationRules.csv
```csv
Antecedent,Consequent,Confidence
entitlementA,entitlementB,0.85
entitlementA,entitlementC,0.76
```

---

## Notes
- Ensure the dataset is large enough to generate meaningful results.
- Adjust partitioning (`repartition(100)`) for optimized performance on your dataset.

---

## License
This project is licensed under the MIT License.
