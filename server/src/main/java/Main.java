import java.io.FileInputStream;
import java.io.File;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import com.google.gson.Gson;

import static spark.Spark.*;

// class for storing inventory information
class Candy {
    String name;
    int id;
    int stock;
    int capacity;

    public Candy(String name, int id, int stock, int capacity) {
        this.name = name;
        this.id = id;
        this.stock = stock;
        this.capacity = capacity;
    }
}

// class used for parsing restock-cost POST request
class restockCandy {
    int id;
    String quantity; // convert when needed for calculations
    double minCost = -1;
}

public class Main {

    public static void main(String[] args) {
        //This is required to allow GET and POST requests with the header 'content-type'
        options("/*",
                (request, response) -> {
                        response.header("Access-Control-Allow-Headers",
                                "content-type");

                        response.header("Access-Control-Allow-Methods",
                                "GET, POST");


                    return "OK";
                });

        //This is required to allow the React app to communicate with this API
        before((request, response) -> response.header("Access-Control-Allow-Origin", "http://localhost:3000"));


        // Return JSON containing the candies for which the stock is less than 25% of it's capacity
        get("/low-stock", (request, response) -> {
            // XLSX file treated as shared resource bc we don't know where inventory gets updated
            ArrayList<Candy> lowStockCandies = new ArrayList<Candy>();

            File f = new File("resources/Inventory.xlsx");
            FileInputStream fis = new FileInputStream(f);
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next(); // skip header row
            // should error check in case format of xlsx file has changed, file has been moved / corrupted
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String name = row.getCell(0).getStringCellValue();
                int stock = (int)row.getCell(1).getNumericCellValue();
                int capacity = (int)row.getCell(2).getNumericCellValue();
                int id = (int)row.getCell(3).getNumericCellValue();

                if (1.0 * stock / capacity < 0.25) {
                    Candy x = new Candy(name, id, stock, capacity);
                    lowStockCandies.add(x);
                }
            }
            Gson gson = new Gson();
            return gson.toJson(lowStockCandies);
        });

        // Return JSON containing the total cost of restocking candy
        post("/restock-cost", (request, response) -> {
            double total_cost = 0;
            Gson gson = new Gson();
            Set<Integer> restockIds = new HashSet<Integer>();
            Map<Integer, restockCandy> toRestock = new HashMap<Integer, restockCandy>();

            // parse request and populate data structures
            final restockCandy[] parsedRequest = gson.fromJson(request.body(), restockCandy[].class);
            for (restockCandy candy : parsedRequest) {
                restockIds.add(candy.id);
                toRestock.put(candy.id, candy);
            }

            File f = new File("resources/Distributors.xlsx");
            FileInputStream fis = new FileInputStream(f);
            XSSFWorkbook wb = new XSSFWorkbook(fis);

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                XSSFSheet sheet = wb.getSheetAt(i);
                Iterator<Row> rowIterator = sheet.iterator();
                rowIterator.next(); // skip header row
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    // error prevention for sheets with empty rows
                    if (row.getCell(0) == null) {
                        break;
                    }
                    int id = (int)row.getCell(1).getNumericCellValue();
                    double cost = row.getCell(2).getNumericCellValue();

                    if (restockIds.contains(id)) {
                        if (toRestock.get(id).minCost == -1) {
                            // haven't seen a price for this candy yet
                            toRestock.get(id).minCost = cost;
                        }
                        else if (toRestock.get(id).minCost > cost) {
                            // cheaper than previously checked distributor
                            toRestock.get(id).minCost = cost;
                        }
                    }
                }
            }

            // add up cost of candies
            Iterator<Integer> it = restockIds.iterator();
            while (it.hasNext()) {
                int id = it.next();
                total_cost += toRestock.get(id).minCost * Integer.parseInt(toRestock.get(id).quantity);
            }

            // return total cost rounded to 2 decimal points
            return gson.toJson(Math.round(total_cost * 100.0) / 100.0);
        });
    }
}
