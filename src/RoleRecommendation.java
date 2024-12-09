import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.ml.fpm.FPGrowth;
import org.apache.spark.ml.fpm.FPGrowthModel;
import scala.collection.mutable.WrappedArray;
import org.apache.spark.sql.Encoders;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class RoleRecommendation {

        public static void main(String[] args) {
                SparkSession spark = SparkSession.builder()
                                .appName("RoleRecommendation")
                                .config("spark.driver.memory", "16g")
                                .config("spark.executor.memory", "16g")
                                .config("spark.sql.shuffle.partitions", "500")
                                .master("local[*]")
                                .getOrCreate();

                // Load data
                Dataset<Row> data = spark.read().format("csv")
                                .option("header", "true")
                                .load("C:\\Java\\FpGrowthDemo\\UserEntitlement.csv");

                // Total number of unique users
                long totalUsers = data.select("User").distinct().count();

                // Count the frequency of each entitlement
                Dataset<Row> entitlementCounts = data.groupBy("Entitlement")
                                .count()
                                .withColumnRenamed("count", "frequency");

                // Filter entitlements that appear in more than 80% of users
                double minSupportThreshold = 0.80 * totalUsers;
                Dataset<Row> filteredEntitlements = entitlementCounts
                                .filter(functions.col("frequency").geq(minSupportThreshold))
                                .select("Entitlement");

                // Convert filtered entitlements to a list for filtering transactions
                List<String> filteredEntitlementList = filteredEntitlements.as(Encoders.STRING()).collectAsList();

                // Define UDF to remove duplicates and filter entitlements
                UserDefinedFunction filterEntitlementsUDF = functions.udf((WrappedArray<String> items) -> {
                        return items.toSet().filter(item -> filteredEntitlementList.contains(item)).toSeq();
                }, DataTypes.createArrayType(DataTypes.StringType));

                // Convert data to dataset of lists of filtered entitlements per user
                Dataset<Row> transactions = data.groupBy("User")
                                .agg(filterEntitlementsUDF.apply(functions.collect_list("Entitlement")).as("items"))
                                .repartition(100) // Increase the number of partitions
                                .cache(); // Cache intermediate result

                // Apply FP-Growth
                FPGrowth fpGrowth = new FPGrowth()
                                .setItemsCol("items")
                                .setMinSupport(0.01) // Adjust support threshold
                                .setMinConfidence(0.8);

                FPGrowthModel model = fpGrowth.fit(transactions);

                // Filter itemsets to include only those with 3 or more entitlements
                Dataset<Row> frequentItemsets = model.freqItemsets()
                                .filter(functions.size(functions.col("items")).geq(3));

                // Generate association rules
                Dataset<Row> associationRules = model.associationRules();

                // Prepare CSV output for role recommendations
                List<String> roleRecommendationLines = new ArrayList<>();
                roleRecommendationLines.add("Role Recommendation,Associated Entitlements,Frequency");

                int roleIndex = 1;

                // Collect filtered itemsets and write to CSV
                for (Row row : frequentItemsets.collectAsList()) {
                        List<String> itemsetList = row.getList(row.fieldIndex("items"));
                        String itemset = String.join(",", itemsetList);
                        long frequency = row.getLong(row.fieldIndex("freq"));

                        roleRecommendationLines.add(
                                        escapeCsv("role" + roleIndex++) + "," +
                                                        escapeCsv(itemset) + "," +
                                                        frequency);
                }

                // Write role recommendations to CSV
                try {
                        Files.write(Paths.get("C:\\Java\\FpGrowthDemo\\RoleRecommendations.csv"),
                                        roleRecommendationLines, StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING);
                        System.out.println("Role Recommendations CSV file created successfully.");
                } catch (IOException e) {
                        e.printStackTrace();
                }

                // Prepare CSV output for association rules
                List<String> associationRulesLines = new ArrayList<>();
                associationRulesLines.add("Antecedent,Consequent,Confidence");

                // Collect association rules and write to CSV
                for (Row row : associationRules.collectAsList()) {
                        List<String> antecedent = row.getList(row.fieldIndex("antecedent"));
                        List<String> consequent = row.getList(row.fieldIndex("consequent"));
                        double confidence = row.getDouble(row.fieldIndex("confidence"));

                        associationRulesLines.add(
                                        escapeCsv(String.join(",", antecedent)) + "," +
                                                        escapeCsv(String.join(",", consequent)) + "," +
                                                        confidence);
                }

                // Write association rules to a separate CSV file
                try {
                        Files.write(Paths.get("C:\\Java\\FpGrowthDemo\\AssociationRules.csv"),
                                        associationRulesLines, StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING);
                        System.out.println("Association Rules CSV file created successfully.");
                } catch (IOException e) {
                        e.printStackTrace();
                } finally {
                        spark.stop();
                }
        }

        private static String escapeCsv(String value) {
                if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                        value = "\"" + value.replace("\"", "\"\"") + "\"";
                }
                return value;
        }
}
