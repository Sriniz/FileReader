/**
 * @author Srinizkumar Konakanchi
 *
 */

package com.xlsxReadWrite;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectorConfig;

public class CreateXLSXFile extends createFileAbstract {

	LinkedHashMap<String, String> colToMap = new LinkedHashMap<String, String>();

	public void createFile(PartnerConnection partConn, BulkConnection restConn, ConnectorConfig partnerConfig,
			String uploadId) {

		try {
			String restEndPoint = partnerConfig.getServiceEndpoint().substring(0,partnerConfig.getServiceEndpoint().indexOf(".com")+4);
			HttpClient httpclient = new HttpClient();
			NameValuePair[] paramsQuery = new NameValuePair[1];
			paramsQuery[0] = new NameValuePair("q",
					"select id from Report where DeveloperName = 'CA_Upload_Records_report'");
			String queryURL = restEndPoint + "/services/data/v37.0/query";
			GetMethod mygetQuery = new GetMethod(queryURL);
			mygetQuery.setRequestHeader("Authorization", "Bearer " + partnerConfig.getSessionId());
			mygetQuery.setRequestHeader("Sforce-Query-Options", "batchSize=2000");
			mygetQuery.setQueryString(paramsQuery);
			int statusCode = httpclient.executeMethod(mygetQuery);
			System.out.println("Connection status:" + statusCode);
			JSONObject myquery1 = new JSONObject(
					new JSONTokener(new InputStreamReader(mygetQuery.getResponseBodyAsStream())));
			JSONArray results1 = myquery1.getJSONArray("records");
			JSONObject jsonObj = results1.getJSONObject(0);
			String reportId = jsonObj.get("Id").toString();
			/*
			 * QueryResult queryResults = partConn .query(
			 * "select id__c from CA_Report_Id__c where Name = 'Fail Report Id' LIMIT 1"
			 * ); String reportId =
			 * (String)queryResults.getRecords()[0].getField("Id__c").toString()
			 * ;
			 */
			String FileName = "FailedRecords";
			String xlsxFileName = FileName + ".xlsx";
			FileOutputStream fileOutputStream = new FileOutputStream(xlsxFileName);
			int RowNum = 0;
			SXSSFWorkbook workBook = new SXSSFWorkbook(250);
			CellStyle cellStyle = workBook.createCellStyle();
			CreationHelper createHelper = workBook.getCreationHelper();
			short dateFormat = 0;
			dateFormat = createHelper.createDataFormat().getFormat("MM/dd/yyyy");
			cellStyle.setDataFormat(dateFormat);
			SXSSFSheet sheet = (SXSSFSheet) workBook.createSheet(FileName);

			colToMap.put("CA_Upload_Record__c.", "");
			JSONObject reportDetails = getReportDetails(reportId, partnerConfig);
			HashMap<String, String> headerHashmap = getReportHeaderMap(reportDetails);
			HashMap<String, String> dataTypeHashmap = getReportDataTypeMap(reportDetails);
			List<String> colList = getReportColList(reportDetails, headerHashmap);
			List<String> dataTypeList = getReportDateTypeList(reportDetails, dataTypeHashmap);
			List<String> colAPIList = getReportAPIList(reportDetails);

			Row headerRow = sheet.createRow(RowNum++);

			// Add header
			for (int i = 0; i < colList.size(); i++) {
				headerRow.createCell(i).setCellValue(colList.get(i));
			}

			// Reading First set of data
			String reportURL = restEndPoint + "/services/data/v37.0/query";
			GetMethod myget = new GetMethod(reportURL);
			myget.setRequestHeader("Authorization", "Bearer " + partnerConfig.getSessionId());
			myget.setRequestHeader("Sforce-Query-Options", "batchSize=2000");
			String query = getQueryString(reportDetails, uploadId);
			NameValuePair[] params = new NameValuePair[1];
			params[0] = new NameValuePair("q", query);
			myget.setQueryString(params);
			String nextRecordsUrl = null;
			do {
				httpclient.executeMethod(myget);
				JSONObject myquery = new JSONObject(
						new JSONTokener(new InputStreamReader(myget.getResponseBodyAsStream())));
				JSONArray results = myquery.getJSONArray("records");
				List<List<String>> dataList = parseJSON(results, colAPIList);
				for (List<String> data : dataList) {
					Row currentRow = sheet.createRow(RowNum++);
					for (int i = 0; i < data.size(); i++) {
						if (dataTypeList.get(i).equalsIgnoreCase("date") && !data.get(i).equalsIgnoreCase("")) {
							DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
							Date d = format.parse(data.get(i));
							Cell c = currentRow.createCell(i);
							c.setCellValue(d);
							c.setCellStyle(cellStyle);
						} else if (dataTypeList.get(i).equalsIgnoreCase("double")
								&& !data.get(i).equalsIgnoreCase("")) {
							Double d = Double.valueOf(data.get(i));
							Cell c = currentRow.createCell(i);
							c.setCellValue(d);
						} else {
							currentRow.createCell(i).setCellValue(data.get(i));
						}
					}
				}
				((SXSSFSheet) sheet).flushRows(0);
				fileOutputStream.flush();
				try {
					nextRecordsUrl = myquery.getString("nextRecordsUrl");
				} catch (Exception e) {
					nextRecordsUrl = null;
				}
				reportURL = restEndPoint + nextRecordsUrl;
				System.out.println("reportURL " + reportURL);
				myget = new GetMethod(reportURL);
				myget.setRequestHeader("Authorization", "Bearer " + partnerConfig.getSessionId());
				myget.setRequestHeader("Sforce-Query-Options", "batchSize=2000");
			} while (nextRecordsUrl != null);
			workBook.write(fileOutputStream);
			fileOutputStream.close();
			workBook.dispose();
			workBook.close();

			System.out.println("I am done");
			uploadAttachment(partnerConfig.getSessionId(), restEndPoint,
					uploadId, xlsxFileName);

		} catch (Exception e) {
			System.out.println("unable to convert the file :" + e.toString());
			e.printStackTrace();
			// TODO: handle exception
		}

	}

	public JSONObject getReportDetails(String reportId, ConnectorConfig partnerConfig) throws Exception {
		HttpClient httpclient = new HttpClient();
		String restEndPoint = partnerConfig.getServiceEndpoint().substring(0,partnerConfig.getServiceEndpoint().indexOf(".com")+4);
		String reportURL = restEndPoint
				+ "/services/data/v37.0/analytics/reports/" + reportId + "/describe";
		GetMethod myget = new GetMethod(reportURL);
		myget.setRequestHeader("Authorization", "Bearer " + partnerConfig.getSessionId());
		httpclient.executeMethod(myget);
		JSONObject myquery;
		myquery = new JSONObject(new JSONTokener(new InputStreamReader(myget.getResponseBodyAsStream())));
		return myquery;
	}

	public HashMap<String, String> getReportHeaderMap(JSONObject reportDetails) throws Exception {
		HashMap<String, String> reportkeyHeader = new HashMap<String, String>();
		JSONObject reportExtendedMetadata = reportDetails.getJSONObject("reportExtendedMetadata");
		JSONObject detailColumns = reportExtendedMetadata.getJSONObject("detailColumnInfo");
		@SuppressWarnings("rawtypes")
		Iterator iter = detailColumns.keys();
		while (iter.hasNext()) {
			String col = (String) iter.next();
			JSONObject value = detailColumns.getJSONObject(col);
			for (Map.Entry<String, String> entry : colToMap.entrySet()) {
				String key = entry.getKey();
				String kvalue = entry.getValue();
				if (col.startsWith(key)) {
					col = col.replace(key, kvalue);
					reportkeyHeader.put(col, value.getString("label"));
				}
			}
		}
		return reportkeyHeader;
	}

	public HashMap<String, String> getReportDataTypeMap(JSONObject reportDetails) throws Exception {
		HashMap<String, String> reportkeyDataType = new HashMap<String, String>();
		JSONObject reportExtendedMetadata = reportDetails.getJSONObject("reportExtendedMetadata");
		JSONObject detailColumns = reportExtendedMetadata.getJSONObject("detailColumnInfo");
		@SuppressWarnings("rawtypes")
		Iterator iter = detailColumns.keys();
		while (iter.hasNext()) {
			String col = (String) iter.next();
			JSONObject value = detailColumns.getJSONObject(col);
			for (Map.Entry<String, String> entry : colToMap.entrySet()) {
				String key = entry.getKey();
				String kvalue = entry.getValue();
				if (col.startsWith(key)) {
					col = col.replace(key, kvalue);
					reportkeyDataType.put(col, value.getString("dataType"));
				}
			}
		}
		return reportkeyDataType;
	}

	public List<String> getReportColList(JSONObject reportDetails, HashMap<String, String> fieldColMap)
			throws Exception {
		List<String> colList = new ArrayList<String>();
		JSONObject reportMetadata = reportDetails.getJSONObject("reportMetadata");
		JSONArray detailColumns = reportMetadata.getJSONArray("detailColumns");
		for (int i = 0; i < detailColumns.length(); i++) {
			String col = detailColumns.get(i).toString();
			for (Map.Entry<String, String> entry : colToMap.entrySet()) {
				String key = entry.getKey();
				String kvalue = entry.getValue();
				if (col.startsWith(key)) {
					col = col.replace(key, kvalue);
					colList.add(fieldColMap.get(col));
				}
			}
		}
		return colList;
	}

	public List<String> getReportDateTypeList(JSONObject reportDetails, HashMap<String, String> fieldColMap)
			throws Exception {
		List<String> colList = new ArrayList<String>();
		JSONObject reportMetadata = reportDetails.getJSONObject("reportMetadata");
		JSONArray detailColumns = reportMetadata.getJSONArray("detailColumns");
		for (int i = 0; i < detailColumns.length(); i++) {
			String col = detailColumns.get(i).toString();
			for (Map.Entry<String, String> entry : colToMap.entrySet()) {
				String key = entry.getKey();
				String kvalue = entry.getValue();
				if (col.startsWith(key)) {
					col = col.replace(key, kvalue);
					colList.add(fieldColMap.get(col));
				}
			}
		}
		return colList;
	}

	public List<String> getReportAPIList(JSONObject reportDetails) throws Exception {
		List<String> colList = new ArrayList<String>();
		JSONObject reportMetadata = reportDetails.getJSONObject("reportMetadata");
		JSONArray detailColumns = reportMetadata.getJSONArray("detailColumns");
		for (int i = 0; i < detailColumns.length(); i++) {
			String col = detailColumns.get(i).toString();
			for (Map.Entry<String, String> entry : colToMap.entrySet()) {
				String key = entry.getKey();
				String kvalue = entry.getValue();
				if (col.startsWith(key)) {
					col = col.replace(key, kvalue);
					colList.add(col);
				}
			}
		}
		return colList;
	}

	public String getQueryString(JSONObject reportDetails, String uploadId) throws Exception {
		String reportQuery = null;
		JSONObject reportMetadata = reportDetails.getJSONObject("reportMetadata");
		JSONArray detailColumns = reportMetadata.getJSONArray("detailColumns");
		reportQuery = "SELECT ";
		for (int i = 0; i < detailColumns.length(); i++) {
			String col = detailColumns.get(i).toString();

			for (Map.Entry<String, String> entry : colToMap.entrySet()) {
				String key = entry.getKey();
				String kvalue = entry.getValue();
				if (col.startsWith(key)) {
					col = col.replace(key, kvalue);
					reportQuery += col + ",";
				}
			}
		}
		reportQuery = reportQuery.substring(0, reportQuery.length() - 1); // remove
																			// the
																			// last
																			// comma
		reportQuery += " FROM CA_Upload_Record__c WHERE CA_Upload__c = '" + uploadId
				+ "' AND CA_upload_status__c ='Failed'";
		System.out.println("\nFinal Query :" + reportQuery);
		return reportQuery;
	}

}
