package io.fusionauth.example.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.ScimRequestBuilder;
import de.captaingoldfish.scim.sdk.client.builder.ListBuilder;
import de.captaingoldfish.scim.sdk.client.builder.ListBuilder.GetRequestBuilder;
import de.captaingoldfish.scim.sdk.client.http.BasicAuth;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;

public class ScimExample {

	// change these
	private static final String SCIM_SERVER_ENTITY_ID = "a647e989-1c7e-4386-9ec6-fa4fe6908906";
	private static final String EXISTING_USER_ID = "d4a3ba16-fc77-4a5c-8216-5f629a4afb62";
	private static final String FUSIONAUTH_HOST = "https://local.fusionauth.io";
	private static final String CLIENT_ID = "eb6fce6a-4ed8-4010-8091-1709fc823329";
	
	// probably won't need to change these
	private static final String SCIM_PERMISSIONS = "scim:user:read,scim:user:create";
	private static final String SCIM_APPLICATION_BASE_URL = FUSIONAUTH_HOST+"/api/scim/resource/v2/";
	


	public static void main(String[] args) throws Exception {
		String secret = "";
		String operation = "get";
		
		if (args.length >= 1) {
			secret = args[0];
		}
		if (args.length == 2) {
			operation = args[1];
		}
		
		Map<String, String> headersMap = new HashMap<String,String>();
		
		headersMap.put("Authorization", "Bearer "+getCredentials(secret));
		
		ScimClientConfig scimClientConfig = ScimClientConfig.builder()
				.connectTimeout(5)
				.requestTimeout(5)
				.socketTimeout(5)				
                .hostnameVerifier((s, sslSession) -> true)
				.httpHeaders(headersMap)
				.build();
		
		ScimRequestBuilder scimRequestBuilder = new ScimRequestBuilder(SCIM_APPLICATION_BASE_URL, scimClientConfig);
		
		
		if (operation == "get") {
			getUser(scimRequestBuilder, EXISTING_USER_ID);	
		} else if (operation == "list") {
			listUsers(scimRequestBuilder);	
		} else if (operation == "create") {
			headersMap.put("Content-type", "application/json");
			createUser(scimRequestBuilder, "test@example.com","password");
		} else {
			getUser(scimRequestBuilder, EXISTING_USER_ID);
		}
		
	}

	private static String getCredentials(String secret) throws AuthenticationException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost(FUSIONAUTH_HOST+"/oauth2/token");

	    List<NameValuePair> params = new ArrayList<NameValuePair>();
	    params.add(new BasicNameValuePair("grant_type", "client_credentials"));
	    params.add(new BasicNameValuePair("scope", "target-entity:"+SCIM_SERVER_ENTITY_ID +":"+SCIM_PERMISSIONS));
	    httpPost.setEntity(new UrlEncodedFormEntity(params));
	    
	    // auth using our client id and secret
	    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(CLIENT_ID,secret);
	    httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));	    
	    
	    CloseableHttpResponse response = client.execute(httpPost);
	    
	 // pull the access_token out of the response
	    String out = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
	  
	    ObjectMapper mapper = new ObjectMapper();
	    TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
	    Map<String, String> map = mapper.readValue(out, typeRef);
	    
	    String token = map.get("access_token");

	    client.close();
	    
	    return token;
	}

	private static void createUser(ScimRequestBuilder scimRequestBuilder, String email,String password) {
		
		List<Email> emails = new ArrayList<Email>();
		emails.add(new Email("primary", Boolean.TRUE,"primary", email, ""));
		User user = User.builder().password(password).emails(emails).active(true).build();
		
		String endpointPath = EndpointPaths.USERS; 
		ServerResponse<User> response = scimRequestBuilder.create(User.class, endpointPath).setResource(user)
				.sendRequest();
		if (response.isSuccess()) {
			User createdUser = response.getResource();
			System.out.println(createdUser);
		} else if (response.getErrorResponse() == null) {
			// the response was not an error response as described in RFC7644
			String errorMessage = response.getResponseBody();
			  System.out.println("error message status: " + response.getHttpStatus());
			  System.out.println("error message: " + errorMessage);
		} else {
			ErrorResponse errorResponse = response.getErrorResponse();
			// do something with it
			 System.out.println("error response: "+errorResponse);
		}
	}


	private static void getUser(ScimRequestBuilder scimRequestBuilder, String id) {

		String endpointPath = EndpointPaths.USERS; 
		ServerResponse<User> response = scimRequestBuilder.get(User.class, endpointPath, id).sendRequest();
		System.out.println(response);
		if (response.isSuccess()) 
		{
		  User returnedUser = response.getResource();
		  System.out.println(returnedUser);
		} else if (response.getErrorResponse() == null) {
			// the response was not an error response as described in RFC7644
			String errorMessage = response.getResponseBody();
			  System.out.println("error message status: " + response.getHttpStatus());
			  System.out.println("error message: " + errorMessage);
		} else {
			ErrorResponse errorResponse = response.getErrorResponse();
			// do something with it
			 System.out.println("error response: "+errorResponse);
		}
	}
	
	private static void listUsers(ScimRequestBuilder scimRequestBuilder) {
		String endpointPath = EndpointPaths.USERS; 
		ServerResponse<ListResponse<User>> response = scimRequestBuilder.list(User.class, endpointPath)
				.startIndex(1).count(5).get().sendRequest();

		if (response.isSuccess()) 
		{
		  ListResponse<User> returnedUserList = response.getResource();
		  // do something with it
		  System.out.println(returnedUserList);
		} else if (response.getErrorResponse() == null) {
			// the response was not an error response as described in RFC7644
			String errorMessage = response.getResponseBody();
			  System.out.println("error message status: " + response.getHttpStatus());
			  System.out.println("error message: " + errorMessage);
		} else {
			ErrorResponse errorResponse = response.getErrorResponse();
			// do something with it
			 System.out.println("error response: "+errorResponse);
		}
	}
}



