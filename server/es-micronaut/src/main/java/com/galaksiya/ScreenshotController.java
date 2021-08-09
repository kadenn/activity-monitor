package com.galaksiya;

import java.math.BigInteger;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.imageio.ImageIO;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import sun.misc.BASE64Decoder;

@Controller
public class ScreenshotController {

	private static final Logger logger = LoggerFactory.getLogger(ScreenshotController.class);

	RestHighLevelClient client = new RestHighLevelClient(
			RestClient.builder(new HttpHost("es01", 9200, "http"), new HttpHost("es02", 9201, "http"), new HttpHost("es03", 9202, "http")));

	@Post("/screenshots")
	public String returnScreenshotLogs(@Body String postBodyString) {
		
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
		SearchRequest searchRequest = new SearchRequest("screenshot-logs");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		
		
		if (macAdress == null && computerName == null) {
			searchSourceBuilder.query(QueryBuilders.boolQuery()
				.must(QueryBuilders.rangeQuery("time").gte(startDate))
				.must(QueryBuilders.rangeQuery("time").lte(endDate)));

		} else if (macAdress == null && computerName != null) {
			searchSourceBuilder.query(QueryBuilders.boolQuery()
				.must(QueryBuilders.rangeQuery("time").gte(startDate))
				.must(QueryBuilders.rangeQuery("time").lte(endDate))
				.must(QueryBuilders.matchQuery("computer_name", computerName)));

		} else if (macAdress != null && computerName == null) {
			searchSourceBuilder.query(QueryBuilders.boolQuery()
				.must(QueryBuilders.rangeQuery("time").gte(startDate))
				.must(QueryBuilders.rangeQuery("time").lte(endDate))
				.must(QueryBuilders.matchQuery("mac_adress", macAdress)));

		} else {
			searchSourceBuilder.query(QueryBuilders.boolQuery()
				.must(QueryBuilders.rangeQuery("time").gte(startDate))
				.must(QueryBuilders.rangeQuery("time").lte(endDate))
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

	@Consumes({ MediaType.APPLICATION_OCTET_STREAM })
	@Post("/screenshot/add")
	public HttpResponse addScreenshotLogs(String jsonString) {

		String SCREENSHOT_SAVE_DIRECTORY = "/home/screenshots";

		File directory = new File(SCREENSHOT_SAVE_DIRECTORY);
		if (!directory.exists()) {
			directory.mkdir();
		}

		JSONObject obj = new JSONObject(jsonString);
		String base64String = obj.getString("base64String");
		String time = obj.getString("time");
		String timeZone = obj.getString("timezone");
		String macAdress = obj.getString("mac_adress");
		String computerName = obj.getString("computer_name");

		try {
			// create a buffered image
			BASE64Decoder decoder = new BASE64Decoder();
			byte[] imageByte = decoder.decodeBuffer(base64String);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			BufferedImage image = ImageIO.read(bis);
			bis.close();

			// write the image to a file
			String time_for_title = time.replace(' ', '_').replace('/', '-');
			String ss_title = "screenshot_" + macAdress + "_" + computerName + "_" + timeZone + "_" + time_for_title + ".png";
			File outputfile = new File(SCREENSHOT_SAVE_DIRECTORY + "/" + ss_title);
			logger.info("Saving " + ss_title + " to " + SCREENSHOT_SAVE_DIRECTORY);
			ImageIO.write(image, "png", outputfile);
			obj.put("screenshot_title", ss_title);

		} catch (IOException e) {
			logger.error(e.getMessage());
			return HttpResponse.notFound();
		}

		addImageDataToElasticSearch(obj);

		return HttpResponse.ok();
	}

	@Get(value = "/screenshot/{screenshotTitle}", produces = MediaType.ALL)
	public byte[] returnImageByTitle(@PathVariable String screenshotTitle) {

		byte[] imageData;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			File image = new File("/home/screenshots/" + screenshotTitle);
			Files.copy(image.toPath(), baos);
			imageData = baos.toByteArray();
			baos.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
			return null;
		}

		return imageData;
	}

	public void addImageDataToElasticSearch(JSONObject obj) {

		obj.remove("base64String");
		String newJsonString = obj.toString();

		logger.info("Inserting screenshot datas into elasticsearch: \n" + newJsonString);

		IndexRequest request = new IndexRequest("screenshot-logs");
		request.source(newJsonString, XContentType.JSON);

		IndexResponse indexResponse;
		try {
			indexResponse = client.index(request, RequestOptions.DEFAULT);
			logger.info(indexResponse.toString());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

	}

}