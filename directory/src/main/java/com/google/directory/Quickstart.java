package com.google.directory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Group;
import com.google.api.services.admin.directory.model.Groups;
import com.google.api.services.admin.directory.model.Member;
import com.google.api.services.admin.directory.model.Members;

/**
 * 
 * @author tahir
 *
 */
public class Quickstart {
	/** Application name. */
	private static final String APPLICATION_NAME = "Directory API Java Quickstart";

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(
			System.getProperty("user.home"),
			".credentials/admin-directory_v1-java-quickstart");

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory
			.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/**
	 * Global instance of the scopes required by this quickstart.
	 *
	 * If modifying these scopes, delete your previously saved credentials at
	 * ~/.credentials/admin-directory_v1-java-quickstart
	 */
	private static final List<String> SCOPES = Arrays.asList(
			DirectoryScopes.ADMIN_DIRECTORY_GROUP,
			DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER);

	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	public static Credential authorize() throws IOException {
		// Load client secrets.
		InputStream in = Quickstart.class
				.getResourceAsStream("/client_secret.json");
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
				.setDataStoreFactory(DATA_STORE_FACTORY)
				.setAccessType("offline").build();
		Credential credential = new AuthorizationCodeInstalledApp(flow,
				new LocalServerReceiver()).authorize("user");
		System.out.println("Credentials saved to "
				+ DATA_STORE_DIR.getAbsolutePath());
		return credential;
	}

	/**
	 * Build and return an authorized Admin SDK Directory client service.
	 * 
	 * @return an authorized Directory client service
	 * @throws IOException
	 */
	public static Directory getDirectoryService() throws IOException {
		Credential credential = authorize();
		return new Directory.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME).build();
	}

	public static void main(String[] args) throws IOException {
		Map<String, Map<String, List<String>>> domainTree = new LinkedHashMap<String, Map<String, List<String>>>();
		Map<String, List<String>> groupMembers = new LinkedHashMap<String, List<String>>();
		// Build a new authorized API client service.
		Directory service = getDirectoryService();
		String grpNextToken = null;
		do {
			Groups groups = service.groups().list().setDomain("reancloud.com")
					.setMaxResults(500).setPageToken(grpNextToken).execute();
			if (groups == null || groups.size() == 0) {
				System.out.println("No groups found.");
			} else {
				System.out
						.println("Fetching groups and their members for reancloud.com");
				for (Group group : groups.getGroups()) {
					if (group != null) {
						/*
						 * System.out.println("Group :" + group.getEmail() +
						 * " ( " + group.getName() + " ) ");
						 */
						List<String> nameList = new LinkedList<String>();
						String nextToken = null;
						do {
							Members members = service.members()
									.list(group.getId()).setMaxResults(500)
									.setPageToken(nextToken).execute();
							if (members != null) {
								List<Member> memberList = members.getMembers();
								if (memberList != null) {
									for (Member m : memberList) {
										if (m != null) {
											GMember member = new GMember();
											member.setEmail(m.getEmail());
											member.setName(m.getEtag());
											member.setRole(m.getRole());
											nameList.add(m.getEmail() + " ("
													+ m.getRole() + ")");
										}
									}
								}
							}
							nextToken = members.getNextPageToken();
							/* System.out.println("Next Token : " + nextToken); */
						} while (nextToken != null);
						/*
						 * System.out.println("Total Members : " +
						 * nameList.size());
						 */
						groupMembers
								.put(group.getEmail() + " (" + nameList.size()
										+ ")", nameList);
					}
				}
			}
			grpNextToken = groups.getNextPageToken();
		} while (grpNextToken != null);
		domainTree.put("reancloud.com (" + groupMembers.keySet().size() + ")",
				groupMembers);
		System.out.println("Creating domain tree in json format..");
		/*
		 * Gson gson = new GsonBuilder().setPrettyPrinting().create();
		 * 
		 * // 1. Java object to JSON, and save into a file
		 * gson.toJson(domainTree, new FileWriter("/tmp/domainTree.json"));
		 */

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
		mapper.writeValue(new File("C:\\Users\\REAN-CLOUD\\Desktop\\Google Project\\reancloud.com.json"), domainTree);

		// Print the first 10 users in the domain.
		/*
		 * Users result = service.users().list().setDomain("reancloud.com")
		 * .setMaxResults(10).setOrderBy("email").execute(); List<User> users =
		 * result.getUsers(); if (users == null || users.size() == 0) {
		 * System.out.println("No users found."); } else {
		 * System.out.println("Users:"); for (User user : users) {
		 * System.out.println(user.getName().getFullName()); } }
		 */
	}
}