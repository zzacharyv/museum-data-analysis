package artInstituteChicago;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.sql.*;  

import java.sql.SQLException;

public class Data_Scraper {

	//query the api for artworks with a landscape subject, get the id, title, artist, and date finished of each artwork
	private static String endpoint = "https://api.artic.edu/api/v1/artworks/search?query[match][subject_titles]=landscape&limit=100&fields=id,title,artist_title,date_end,id";

	public static void main(String[] args) {
				try {
					//connect to mySQL
					Class.forName("com.mysql.cj.jdbc.Driver");
					Connection connection = null;
					try {
						connection = DriverManager
								.getConnection("jdbc:mysql://localhost:3306/test","username", "password");
		
					} catch (SQLException e) {
						System.out.println("Connection Failed");
						e.printStackTrace();
						return;
					}
					if (connection != null) {
						System.out.println("Connection Successful");
					} else {
						System.out.println("Failed Connection");
					}
		
					//create landscapes table in mySQL
					long start = System.currentTimeMillis();
					System.out.println("creating table");
					String createString =
							"create table LANDSCAPES " + "(ID integer NOT NULL, " +
									"TITLE varchar(300), " + "ARTIST_TITLE varchar(200), " +
									"DATE_END integer, " + "PRIMARY KEY (ID))";
					try (Statement stmt = connection.createStatement()) {
						stmt.executeUpdate(createString);
					} catch (SQLException e) {
						e.toString();
					}
					
					//access pages of api and put values into database
					System.out.println("getting values and updating table");
					apiToDatabase(connection, endpoint);
					
					long end = System.currentTimeMillis();
					long time = end-start;
					System.out.println("finished in " + time/1000 + " seconds");
					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
	}

	public static void apiToDatabase(Connection connection, String endpoint) {
		try {
			// the API only allows acess for the first 1000 elements on a query, so go through the first ten pages of size 100
			int count = 0;
			int error_count = 0;
			File myObj = new File("errors.txt");
			myObj.createNewFile();
			FileOutputStream fout = new FileOutputStream("errors.txt"); 
	        PrintStream out = new PrintStream(fout); 
	        
			for (int i=1; i<=10; i++) {
				System.out.print(".");
				URL url = new URL(endpoint+"&page="+i);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.connect();

				//Getting the response code
				int responsecode = conn.getResponseCode();

				if (responsecode != 200) {
					throw new RuntimeException("HttpResponseCode: " + responsecode);
				} else {

					String inline = "";
					Scanner scanner = new Scanner(url.openStream());

					//Write all the JSON data into a string using a scanner
					while (scanner.hasNext()) {
						inline += scanner.nextLine();
					}

					//Close the scanner
					scanner.close();

					//Using the JSON simple library parse the string into a json object
					JSONParser parse = new JSONParser();
					JSONObject data_obj = (JSONObject) parse.parse(inline);
					JSONArray data = (JSONArray) data_obj.get("data");
					for (int j=0; j<data.size(); j++) {
						JSONObject obj = (JSONObject) data.get(j);
						//create the SQL statement to insert the object into the database
						String artist_title="";
						if(obj.get("artist_title")!=null) {
							artist_title = (String) obj.get("artist_title");
						}
						String insertString = "INSERT INTO LANDSCAPES (ID, TITLE, ARTIST_TITLE, DATE_END) VALUES "
								+ "("+obj.get("id")+", \""+
								((String) obj.get("title")).replace("\"", "")+"\", \""+
								artist_title+
								"\", "+obj.get("date_end")+")";
						try (Statement stmt = connection.createStatement()) {
							//insert the object into the database 
							stmt.executeUpdate(insertString);
						} catch (Exception e) {
							//print errors to errors.txt
							out.println(e.getMessage());
							e.printStackTrace(out);
							out.println("\n");
							error_count++;
						}
						count++;
					}
				}
			}
			System.out.println("\nfinished with " + error_count + " errors out of " + count + " entries, see errors in errors.txt");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}