package org.uxstudy.tablestorage.dec2020;

import org.javatuples.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableBatch;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.implementation.models.TableServiceErrorException;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableItem;
import com.azure.data.tables.models.UpdateMode;

/**
 * Hello world!
 *
 */
public class App 
{
    private TableServiceClient tableServiceClient;

    public App()
    {
        final String connvar = System.getenv("AZURE_TABLES_CONNECTION_STRING");
        System.out.println( "Hello Planet!" );
        tableServiceClient = new TableServiceClientBuilder()
        .connectionString(connvar)
        .buildClient();
    }

    public void Task1()
    {
        // Customers are organised by region, most often queries will be focused on customers in particular regions
        // Regions are Africa, Antarctica, Asia, Australia, Europe, North America, South America.
        // Each customer has a customer account number, a name, a street, city, country
        // Products are organised by category - brakes, drive train, accessories, frames. Each product has a product id, name, price and stock level
        // Orders are organised by customer account number and contain an order id, list of products, total cost of order

        // Task 1 - create a customer table and add the first customer to it.
        tableServiceClient.createTableIfNotExists("customers");            

        PagedIterable<TableItem> tables = tableServiceClient.listTables();
        for (TableItem table : tables)
        {
            System.out.println(table.getName());
        }
    }

    public void Task2()
    {
        // Task 2 - Add a new customer to the customer table. Give the customer a name, street, city, country, region and customer id.
        // Remember that customers will be organised by region
        // The customer id should be unique within the customer's region. Feel free to come up with any customer id format you prefer   
        TableClient customerTable = tableServiceClient.getTableClient("customers");
        TableEntity newCustomer = new TableEntity("Europe", "000001")
        .addProperty("Name", "Steven Clarke")
        .addProperty("Street", "Waverly Gate")
        .addProperty("City", "Edinburgh")
        .addProperty("Country", "Scotland");

        customerTable.upsertEntity(newCustomer);

        // Task 2a - write code that prints out all the customer names in the customer table.
        // Make sure that the name of the customer you just added is printed out

        for (TableEntity cust : customerTable.listEntities())
        {
            System.out.println(cust.getProperty("Name").toString());
        }
    }

    public void Task3()
    {
        //Task 3 - write code that creates a table to store products and a table to store orders
        tableServiceClient.createTableIfNotExists("products");
        tableServiceClient.createTableIfNotExists("orders");

        //  Now write code to add two products to the product table
        // Products are organised by product type - 'Brakes', 'Drive train', 'Wheels'
        // All products have a unique product id, name, price and stock level
        TableClient productsTable = tableServiceClient.getTableClient("products");
        TableEntity brakePads = new TableEntity("Brakes", "000001")
        .addProperty("Name", "Disc brake pads")
        .addProperty("Price", 13.99)
        .addProperty("StockLevel", 57);


        TableEntity brakeCables = new TableEntity("Brakes", "000002")
        .addProperty("Name", "Disc brake cables")
        .addProperty("Price", 23.99)
        .addProperty("StockLevel", 21);

        productsTable.upsertEntity(brakeCables);
        productsTable.upsertEntity(brakePads);

    }

    public void Task4()
    {
        // Task 4 - write code that edits the customer that you added earlier so that instead of a simple Name field there is 
        // a first name and a last name field
        TableClient customerTable = tableServiceClient.getTableClient("customers");
        for (TableEntity entity : customerTable.listEntities())
        {
            Object n = entity.getProperty("Name");
            if (n != null)
            {
                String name = n.toString();
                System.out.println(name);
                String[] names = name.split(" ");
                if (names.length > 1)
                {
                    String fName = names[0];
                    String lName = names[1];
                    System.out.println(fName);
                    System.out.println(lName);
    
                    TableEntity updatedCustomer = new TableEntity(entity.getPartitionKey(), entity.getRowKey());
                    Map<String, Object> props = entity.getProperties();
                    props.remove("Name");
                    props.put("FirstName", fName);
                    props.put("LastName", lName);
                    updatedCustomer.addProperties(props);
                    customerTable.updateEntity(updatedCustomer, UpdateMode.REPLACE);
                }
    
            }
            
        }
    }

    public void Task5(ArrayList<Pair<String,String>> products, Pair<String, String> customer, String ordNumber)
    {
        // Task 5 - write code that adds an order to the orders table.
        // An order is composed of one or more ordered items
        // Each ordered item stores the customer who made the order and the product that was ordered
        // If an order contains two or more ordered items, that order will be recorded in two or more rows of the table

        int itemNumber = 1;
        TableClient productsTable = tableServiceClient.getTableClient("products");
        TableClient customersTable = tableServiceClient.getTableClient("customers");
        TableEntity cust = customersTable.getEntity(customer.getValue0(), customer.getValue1());
        String orderNumber = cust.getProperty("LastName").toString() + ordNumber;
        for (Pair<String, String> product : products)
        {
            TableEntity prod = productsTable.getEntity(product.getValue0(), product.getValue1());
            String item = String.format("Item%d", itemNumber);
            TableEntity order = new TableEntity(orderNumber, item)
            .addProperty("ProductCategory", prod.getPartitionKey())
            .addProperty("ProductName", prod.getRowKey())
            .addProperty("CustomerRegion", cust.getPartitionKey())
            .addProperty("CustomerId", cust.getRowKey());

            TableClient ordersTable = tableServiceClient.getTableClient("orders");
            ordersTable.createEntity(order);
        }
    }

    public void Task6()
    {
        // Imagine that you are now running a loyalty program and you want to mark some  customers as Valued Customer
        // Write code that finds any customer who has placed an order and mark that customer as a Valued Customer
        TableClient ordersTable = tableServiceClient.getTableClient("orders");
        HashMap<String, String> customersWithOrders = new HashMap<String, String>();

        for (TableEntity order : ordersTable.listEntities())
        {
            customersWithOrders.put(order.getProperty("CustomerRegion").toString(), order.getProperty("CustomerId").toString());
        }
        customersWithOrders.forEach(
            (K, V) -> {
                TableClient customersTable = tableServiceClient.getTableClient("customers");
                TableEntity cust = customersTable.getEntity(K, V);
                cust.addProperty("ValuedCustomer", "Yes");
                customersTable.upsertEntity(cust);
            });
    }

    public void Task7()
    {
        // Now add multiple customers to the customer table. Is there a way you can do this without making separate service requests
        // to add each customer separately?
        TableClient customerTable = tableServiceClient.getTableClient("customers");
        TableEntity cust1 = new TableEntity("Europe", "000003")
        .addProperty("FirstName", "Andy")
        .addProperty("LastName", "Murray")
        .addProperty("Street", "1 Main Street")
        .addProperty("City", "Edinburgh")
        .addProperty("Country", "Scotland");

        TableEntity cust2 = new TableEntity("Europe", "000004")
        .addProperty("FirstName", "Kelly")
        .addProperty("LastName", "Macdonald")
        .addProperty("Street", "1 Main Street")
        .addProperty("City", "Edinburgh")
        .addProperty("Country", "Scotland");

        TableEntity cust3 = new TableEntity("Europe", "000005")
        .addProperty("FirstName", "Flora")
        .addProperty("LastName", "Macdonald")
        .addProperty("Street", "1 Main Street")
        .addProperty("City", "Edinburgh")
        .addProperty("Country", "Scotland");

        TableEntity cust4 = new TableEntity("Europe", "000006")
        .addProperty("FirstName", "Muriel")
        .addProperty("LastName", "Spark")
        .addProperty("Street", "1 Main Street")
        .addProperty("City", "Edinburgh")
        .addProperty("Country", "Scotland");

        customerTable.createBatch("Europe")
        .createEntity(cust1)
        .createEntity(cust2)
        .createEntity(cust3)
        .createEntity(cust4)
        .submitTransaction();


        
    }

    public static void main( String[] args )
    {
        App app = new App();
        app.Task1();
        app.Task2();
        app.Task3();
        app.Task4();

        Pair<String, String> product1 = new Pair<String, String>("Brakes", "Disc brake pads");
        Pair<String, String> product2 = new Pair<String, String>("Brakes", "Disc brake cables");
        Pair<String, String> customer = new Pair<String, String>("Europe", "000001");

        ArrayList<Pair<String, String>> productsToOrder = new ArrayList<Pair<String, String>>();
        productsToOrder.add(product1);
        productsToOrder.add(product2);

        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss");
        String orderNumber = myDateObj.format(myFormatObj);  

        app.Task5(productsToOrder, customer, orderNumber);
        app.Task7();

    }
}
