package com.galaksiya;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ActiveWindowLogController {

	private static final Logger logger = LoggerFactory.getLogger(ActiveWindowLogController.class);

	RestHighLevelClient client = new RestHighLevelClient(
			RestClient.builder(new HttpHost("es01", 9200, "http"), new HttpHost("es02", 9201, "http"), new HttpHost("es03", 9202, "http")));

	@Post("/active-window-logs")
	public String returnActiveWindowLogs(@Body String postBodyString) {

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		JSONObject postBodyJson = new JSONObject(postBodyString);

		String computerName, macAdress, startDate, endDate;
		int notGivenCounter = 0;

		try {
			computerName = postBodyJson.getString("computer_name");
			logger.info("computer_name is " + computerName + ".");
		} catch (Exception JSONException) {
			computerName = null;
			notGivenCounter++;
			logger.info("computer_name is not given.");
		}

		try {
			macAdress = postBodyJson.getString("mac_adress");
			logger.info("mac_adress is " + macAdress + ".");
		} catch (Exception JSONException) {
			macAdress = null;
			notGivenCounter++;
			logger.info("mac_adress is not given.");
		}

		try {
			startDate = postBodyJson.getString("start_date");
			logger.info("start_date is " + startDate + ".");
		} catch (Exception JSONException) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, -2);
			date = cal.getTime();
			startDate = dateFormat.format(date);
			notGivenCounter++;
			logger.info("start_date is not given, assuming previous year.");
		}

		try {
			endDate = postBodyJson.getString("end_date");
			logger.info("end_date is " + endDate + ".");
		} catch (Exception JSONException) {
			endDate = dateFormat.format(date);
			notGivenCounter++;
			logger.info("end_date not given, assuming now.");
		}
		
		if (notGivenCounter == 4) {
			return "One or more properties must be given.";
		}

		// searchRequest for active-window-logs index
		SearchRequest searchRequest = new SearchRequest("active-window-logs");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		
		
		if (macAdress == null && computerName == null) {
			searchSourceBuilder.query(QueryBuilders.boolQuery()
				.must(QueryBuilders.rangeQuery("request_time").gte(startDate))
				.must(QueryBuilders.rangeQuery("request_time").lte(endDate)));

		} else if (macAdress == null && computerName != null) {
			searchSourceBuilder.query(QueryBuilders.boolQuery()
				.must(QueryBuilders.rangeQuery("request_time").gte(startDate))
				.must(QueryBuilders.rangeQuery("request_time").lte(endDate))
				.must(QueryBuilders.matchQuery("computer_name", computerName)));

		} else if (macAdress != null && computerName == null) {
			searchSourceBuilder.query(QueryBuilders.boolQuery()
				.must(QueryBuilders.rangeQuery("request_time").gte(startDate))
				.must(QueryBuilders.rangeQuery("request_time").lte(endDate))
				.must(QueryBuilders.matchQuery("mac_adress", macAdress)));

		} else {
			searchSourceBuilder.query(QueryBuilders.boolQuery()
				.must(QueryBuilders.rangeQuery("request_time").gte(startDate))
				.must(QueryBuilders.rangeQuery("request_time").lte(endDate))
				.must(QueryBuilders.matchQuery("computer_name", computerName))
				.must(QueryBuilders.matchQuery("mac_adress", macAdress)));
		}

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
		// to keep result
		JSONArray resultArray = new JSONArray();
		for (SearchHit hit : hits.getHits()) {
			JSONObject obj = new JSONObject(hit.getSourceAsString()); // converts ES result json to JSONObject
			resultArray.put(obj);
		}

		return resultArray.toString();
	}

	@Post("/active-window/add")
	public HttpResponse addLogToElasticSearch(String jsonString) {

		logger.info("Inserting active-window datas into elasticsearch: \n" + jsonString);

		IndexRequest request = new IndexRequest("active-window-logs");
		request.source(jsonString, XContentType.JSON);

		IndexResponse indexResponse;
		try {
			indexResponse = client.index(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			logger.error(e.getMessage());
			return HttpResponse.badRequest();
		}
		logger.info(indexResponse.toString());

		return HttpResponse.ok();
	}

}