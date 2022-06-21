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

	public static void main(String[] args) throws Exception {
		String secret = "";
		if (args.length == 1) {
			secret = args[0];
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
		
		final String scimApplicationBaseUrl = "https://local.fusionauth.io/api/scim/resource/v2/";
		System.out.println("here1");
		ScimRequestBuilder scimRequestBuilder = new ScimRequestBuilder(scimApplicationBaseUrl, scimClientConfig);
		System.out.println("here2");
		//getUser(scimRequestBuilder, "d4a3ba16-fc77-4a5c-8216-5f629a4afb62");

		// headersMap.put("Content-type", "application/json");
//		createUser(scimRequestBuilder, "goldfish2");
		
		
		listUsers(scimRequestBuilder);
		System.out.println("here3");
	}

	private static String getCredentials(String secret) throws AuthenticationException, IOException {
		CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost("https://local.fusionauth.io/oauth2/token");

	    List<NameValuePair> params = new ArrayList<NameValuePair>();
	    params.add(new BasicNameValuePair("grant_type", "client_credentials"));
	    params.add(new BasicNameValuePair("scope", "target-entity:a647e989-1c7e-4386-9ec6-fa4fe6908906:scim:user:read,scim:user:create"));
	    httpPost.setEntity(new UrlEncodedFormEntity(params));
	    
	    // auth using our client id and secret
	    UsernamePasswordCredentials creds
	      = new UsernamePasswordCredentials("eb6fce6a-4ed8-4010-8091-1709fc823329",secret);
	    httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));	    
	    
	    CloseableHttpResponse response = client.execute(httpPost);
	    
	 // pull the access_token out of the response
	    String out = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
	    ObjectMapper mapper = new ObjectMapper();
	    TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
	    Map<String, String> map = mapper.readValue(out, typeRef);
	    

	    String token = map.get("access_token");

	    System.out.println(out);
	    client.close();
	    
	    return token;
	}

	private static void createUser(ScimRequestBuilder scimRequestBuilder, String username) {
		List<Email> emails = new ArrayList<Email>();
		emails.add(new Email("primary", Boolean.TRUE,"foo", "foo@example.com", ""));
		User user = User.builder().password("pass1234").emails(emails).active(true).build();
//		User user = User.builder().password("pass1234").userName(username).active(true).build();
		
		System.out.println(user.toPrettyString());
		
		String endpointPath = EndpointPaths.USERS; // holds the value "/Users"
		ServerResponse<User> response = scimRequestBuilder.create(User.class, endpointPath).setResource(user)
				.sendRequest();
		if (response.isSuccess()) {
			User createdUser = response.getResource();
			System.out.println(createdUser);
		} else if (response.getErrorResponse() == null) {
			// the response was not an error response as described in RFC7644
			String errorMessage = response.getResponseBody();
			  System.out.println("1" + response.getHttpStatus());
			  System.out.println("1" + errorMessage);
		} else {
			ErrorResponse errorResponse = response.getErrorResponse();
			// do something with it
			 System.out.println("2"+errorResponse);
		}
	}

	private static void getUser(ScimRequestBuilder scimRequestBuilder, String id) {

		String endpointPath = EndpointPaths.USERS; // holds the value "/Users"
		ServerResponse<User> response = scimRequestBuilder.get(User.class, endpointPath, id).sendRequest();
		if (response.isSuccess()) 
		{
		  User returnedUser = response.getResource();
		  System.out.println(returnedUser);
		}
		else if(response.getErrorResponse() == null)
		{
		  // the response was not an error response as described in RFC7644
		  String errorMessage = response.getResponseBody();
		  System.out.println("1" + response.getHttpStatus());
		  System.out.println("1" + errorMessage);
		}
		else
		{
		  ErrorResponse errorResponse = response.getErrorResponse();
		  // do something with it
		  System.out.println("2"+errorResponse);
		}
	}
	
	private static void listUsers(ScimRequestBuilder scimRequestBuilder) {
		String endpointPath = EndpointPaths.USERS; // holds the value "/Users"
		 ListBuilder<User> foo = scimRequestBuilder.list(User.class, endpointPath)
				.startIndex(1)
		                                                                  .count(5);
//		                                                                  .attributes("userName")
//		                                                                  .filter("username", Comparator.CO, "ai")
//		                                                                     .and("locale", Comparator.EQ, "EN")
//		                                                                  .build()
//		                                                                  .sortBy("username")
		                                                                  ;
//		                                                                  .sortOrder(SortOrder.DESCENDING)
//		                                                                .get(); // http method to use (get or post)
		                                                                //.sendRequest();
		
		System.out.println(foo);
		
	
		ServerResponse<ListResponse<User>> response = foo.get().sendRequest();
		if (response.isSuccess()) 
		{
		  ListResponse<User> returnedUserList = response.getResource();
		  // do something with it
		  System.out.println(returnedUserList);
		}
		else if(response.getErrorResponse() == null)
		{
		  // the response was not an error response as described in RFC7644
		  String errorMessage = response.getResponseBody();
		  System.out.println("1" + response.getHttpStatus());
		  System.out.println("1" + errorMessage);
		}
		else
		{
		  ErrorResponse errorResponse = response.getErrorResponse();
		  // do something with it
		  System.out.println("2"+errorResponse);
		}
	}
}



