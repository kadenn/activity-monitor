package com.galaksiya;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/user")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(ActiveWindowLogController.class);

	// ES Search API Client
	RestHighLevelClient client = new RestHighLevelClient(
			RestClient.builder(new HttpHost("es01", 9200, "http"), new HttpHost("es02", 9201, "http"), new HttpHost("es03", 9202, "http")));

	@Get("/list")
	public String returnUserList() {

		// searchRequest for active-window-logs index
		SearchRequest searchRequest = new SearchRequest("active-window-logs");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchAllQuery());
		searchSourceBuilder.size(1000);
		searchRequest.source(searchSourceBuilder);

		// searchRequest synchronous execution
		SearchResponse searchResponse;
		try {
			searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			logger.error(e.getMessage());
			return "Error establishing a database connection.";
		}

		logger.info("status: " + searchResponse.status() + " | took: " + searchResponse.getTook() + " | terminatedEarly: " + searchResponse.isTerminatedEarly()
				+ " | timedOut: " + searchResponse.isTimedOut());

		// provides search results that can be iterated
		SearchHits hits = searchResponse.getHits();

		Set<String> uniqMacAdrresses = new HashSet<String>();
		JSONArray userInfoJsonArray = new JSONArray();// to keep result
		for (SearchHit hit : hits.getHits()) {
			JSONObject obj = new JSONObject(hit.getSourceAsString()); // converts ES result json to JSONObject
			JSONObject userInfo = new JSONObject(); // new JSONObject to keep user information

			String computerName = obj.getString("computer_name");
			String macAdress = obj.getString("mac_adress");

			// no duplicate
			if (uniqMacAdrresses.contains(macAdress)) {
				continue;
			} else {
				uniqMacAdrresses.add(macAdress);
				userInfo.put("mac_adress", macAdress);
				userInfo.put("computer_name", computerName);
				userInfoJsonArray.put(userInfo);
			}

		}

		return userInfoJsonArray.toString();
	}

}