package ch.hsr.dal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import ch.hsr.bll.CDAR_Contract;
import ch.hsr.bll.CDAR_Customer;
import ch.hsr.bll.CDAR_CustomerContractJoin;

public class CDAR_DatabaseMongoDB {
	private static final String ZIP = "zip";
	private static final String LOCATION = "location";
	private static final String NAME = "name";
	private static final String OID = "_id";
	private static final String DATE = "date";
	private static final String DESCRIPTION = "description";
	private static final String CONTRACT = "contract";
	private static final String CUSTOMER = "customer";

	private MongoDatabase db;
	private MongoClient client;
	private MongoClientURI mongoURI;

	public CDAR_DatabaseMongoDB() {
		init();
	}

	private void init() {
		Properties prop = new Properties();
		try {
			InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties");
			prop.load(in);
			mongoURI = new MongoClientURI(prop.getProperty("mongoURI"));
			client = new MongoClient(mongoURI);
			db = client.getDatabase(prop.getProperty("mongoDB"));
			clearCollection(CUSTOMER);
			clearCollection(CONTRACT);
		} catch (MongoException | IOException e) {
			e.printStackTrace();
		}
	}

	public void addEntry(CDAR_Contract contract) throws Exception {
		try {
			MongoCollection<Document> collContracts = db.getCollection(CONTRACT);
			Document doc = new Document(DESCRIPTION, contract.getDescription()).append(CUSTOMER, getCustomer(contract.getRefID().toString())).append(DATE, contract.getDate());
			collContracts.insertOne(doc);
		} catch (Exception e) {
			throw e;
		}
	}

	public void addEntry(CDAR_Customer customer) {
		try {
			MongoCollection<Document> coll = db.getCollection(CUSTOMER);
			Document doc = new Document(NAME, customer.getName()).append(LOCATION, customer.getLocation()).append(ZIP, customer.getZip());
			coll.insertOne(doc);
			System.out.println(doc.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<CDAR_Contract> getContracts() {
		ArrayList<CDAR_Contract> list = new ArrayList<CDAR_Contract>();

		try {
			MongoCollection<Document> coll = db.getCollection(CONTRACT);
			FindIterable<Document> cursor = coll.find();
			Iterator<Document> it = cursor.iterator();
			while (it.hasNext()) {
				Document element = it.next();
				String idString = element.get(OID).toString();
				String description = element.get(DESCRIPTION).toString();
				Document customer = (Document) element.get(CUSTOMER);
				String customerId = customer.get(OID).toString();
				String date = element.get(DATE).toString();
				list.add(new CDAR_Contract(idString, date, description, customerId));
			}
		} catch (MongoException e) {
			e.printStackTrace();
		}
		return list;
	}

	public ArrayList<CDAR_Customer> getCustomers() {
		ArrayList<CDAR_Customer> list = new ArrayList<CDAR_Customer>();

		try {
			MongoCollection<Document> coll = db.getCollection(CUSTOMER);

			FindIterable<Document> cursor = coll.find();
			Iterator<Document> it = cursor.iterator();
			while (it.hasNext()) {
				Document element = it.next();
				String idString = element.get(OID).toString();
				String name = element.get(NAME).toString();
				String location = element.get(LOCATION).toString();
				String zip = element.get(ZIP).toString();
				list.add(new CDAR_Customer(idString, name, location, zip));
			}
		} catch (MongoException e) {
			e.printStackTrace();
		}
		return list;
	}

	public ArrayList<CDAR_CustomerContractJoin> getJoins() {
		ArrayList<CDAR_CustomerContractJoin> list = new ArrayList<CDAR_CustomerContractJoin>();
		try {
			MongoCollection<Document> collContracts = db.getCollection(CONTRACT);
			collContracts.distinct(DESCRIPTION, String.class);
			FindIterable<Document> cursorContracts = collContracts.find();
			Iterator<Document> it = cursorContracts.iterator();
			while (it.hasNext()) {
				Document contract = it.next();
				String contractDescription = contract.get(DESCRIPTION).toString();
				String contractDate = contract.get(DATE).toString();
				Document customer = (Document) contract.get(CUSTOMER);
				String customer_id = customer.get(OID).toString();
				String customerName = customer.get(NAME).toString();
				String customerLocation = customer.get(LOCATION).toString();
				String customerZip = customer.get(ZIP).toString();
				list.add(new CDAR_CustomerContractJoin(customer_id, customerName, customerLocation, customerZip, contractDate, contractDescription));
			}
		} catch (MongoException e) {
			e.printStackTrace();
		}
		return list;
	}

	private Document getCustomer(String id) throws Exception {
		MongoCollection<Document> collCustomers = db.getCollection(CUSTOMER);
		ObjectId _id = new ObjectId(id);
		Document obj = new Document();
		obj.append(OID, _id);
		Document query = new Document();
		query.putAll((Document) query);
		Document customer = collCustomers.find(query).first();
		if (customer == null) {
			throw new Exception("No customer found");
		}
		return collCustomers.find(query).first();
	}

	private void clearCollection(String name) {
		try {
			MongoCollection<Document> coll = db.getCollection(name);

			FindIterable<Document> cursor = coll.find();
			Iterator<Document> it = cursor.iterator();
			while (it.hasNext()) {
				Document element = it.next();
				coll.deleteMany(element);
			}
		} catch (MongoException e) {
			e.printStackTrace();
		}
	}
}
