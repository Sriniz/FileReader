/**
 * @author Srinizkumar Konakanchi
 *
 */

package com.xlsxReadWrite;
/*
Description: This is used to create async bulk upload jobs in salesforce.
Author: Srinizkumar Konakanchi
*/

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.*;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.util.Base64;
import com.sun.org.apache.xerces.internal.parsers.SAXParser;

import jdk.internal.org.xml.sax.InputSource;
import jdk.internal.org.xml.sax.SAXException;
import jdk.internal.org.xml.sax.XMLReader;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.CSVReader;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import com.sforce.async.RestConnection;

import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JRuntimeException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CreateBulkLoadJobs {

	/**
	 * Create a new job using the Bulk API.
	 */
	public JobInfo createJob(String sobjectType, RestConnection connection) throws AsyncApiException {
		JobInfo job = new JobInfo();
		job.setObject(sobjectType);
		job.setOperation(OperationEnum.insert);
		job.setConcurrencyMode(ConcurrencyMode.Serial);
		job.setContentType(ContentType.CSV);
		job = connection.createJob(job);
		// System.out.println(job);
		return job;
	}

	/**
	 * Create a new job using the Bulk API.
	 */
	public JobInfo createUpdateJob(String sobjectType, RestConnection connection) throws AsyncApiException {
		JobInfo job = new JobInfo();
		job.setObject(sobjectType);
		job.setOperation(OperationEnum.update);
		job.setContentType(ContentType.CSV);
		job = connection.createJob(job);
		// System.out.println(job);
		return job;
	}

	/**
	 * Create and upload batches using a CSV file. The file into the appropriate
	 * size batch files.
	 * 
	 */
	public BatchInformation createBatchesFromCSVFile(PartnerConnection partConn, RestConnection connection,
			JobInfo jobInfo, String parentId, Boolean isSuccess)
					throws IOException, ConnectionException, AsyncApiException, InvalidFormatException {
		List<BatchInfo> batchInfos = new ArrayList<BatchInfo>();
		String aId = "";
		Boolean isSuccessful = false;
		// input file
		QueryResult queryResultsAttach = partConn
				.query("SELECT Id,Body,Name,ParentId FROM Attachment where ParentId='" + parentId + "'");
		// AND Name like '%.csv'");
		String xlsxFile = (String) queryResultsAttach.getRecords()[0].getField("Body");
		File f1 = File.createTempFile("bulkAPIInsert_Attachment", ".xlsx");
		f1.deleteOnExit();

		FileOutputStream inputFile = new FileOutputStream(f1);
		inputFile.write(Base64.decode(xlsxFile.getBytes()));
		inputFile.close();

		/*
		 * xls2xlsx x2xl = new xls2xlsx(); File f1 = x2xl.xls2xlsxConverter(f);
		 */
		// Create the CSV header row
		QueryResult queryResults1 = partConn.query(
				"SELECT Id,Template_Fields_1__c,Template_Fields_2__c,Template_Fields_3__c,CA_Upload__c FROM CA_Upload_Template_Fields__c");
		String headerString = (String) queryResults1.getRecords()[0].getField("Template_Fields_1__c")
				+ (String) queryResults1.getRecords()[0].getField("Template_Fields_2__c")
				+ ((String) queryResults1.getRecords()[0].getField("Template_Fields_3__c") == null ? ""
						: (String) queryResults1.getRecords()[0].getField("Template_Fields_3__c"))
				+ (String) queryResults1.getRecords()[0].getField("CA_Upload__c") + "\n";

		// byte[] headerBytes =
		// ("Video_Version__c,Country__c,Language__c,Language_Type__c,Channel__c,Format__c,Account_Name__c,Start_Date__c,End_Date__c,Status__c,Category__c,Price_Tier__c,WS_Cost__c,SR_Price__c,Episode_Price_Tier__c,Episode_WS_Cost__c,Episode_SR_Price__c,Pre_Order_Date__c,Suppression_Date__c,Notes__c,Client_Title_ID__c,Local_Data_Rating__c,Local_Data_No_Of_Episodes__c,Local_Edit_Required__c,Change_Context__c,CA_Upload__c\n").getBytes("UTF-8");
		File tmpFile = File.createTempFile("bulkAPIInsert", ".csv");
		// XLSXTOCSV x = new XLSXTOCSV(parentId);
		XLSX2CSV x = new XLSX2CSV();

		try {
			int maxRowsPerBatch = 1000; // 10 thousand rows per batch
			int currentLines = 0;
			String csvString = x.xlsx2csvConverter(f1, headerString.split(",").length - 2);

			csvString = csvString.replace("\n", "," + parentId + "\n");
			csvString = headerString + csvString;
			FileOutputStream tmpOut = new FileOutputStream(tmpFile);

			String[] line = csvString.split("\n", -1);
			System.out.println("line.length..." + line.length);
			Integer j = 1;
			for (Integer i = 0; i < line.length; i++) {
				byte[] bytes = (line[i] + "\n").getBytes("UTF-8");
				tmpOut.write(bytes);
				currentLines++;
				if (currentLines > maxRowsPerBatch) {
					System.out.println("Created a bulk load batch for salesforce..." + j);
					createBatch(tmpOut, tmpFile, batchInfos, connection, jobInfo);
					tmpOut = new FileOutputStream(tmpFile);
					tmpOut.write(headerString.getBytes("UTF-8"));
					currentLines = 0;
					j++;
				}
			}

			// Finished processing all rows
			// Create a final batch for any remaining data
			if (currentLines > 1) {
				createBatch(tmpOut, tmpFile, batchInfos, connection, jobInfo);
			}
			isSuccessful = true;
		} catch (Exception e) {
			System.out.println("Something went wrong...");
			isSuccessful = false;
			e.printStackTrace();
		} finally {
			tmpFile.deleteOnExit();
			f1.deleteOnExit();
		}
		return new BatchInformation(batchInfos, aId, isSuccessful);
	}

	/**
	 * Create a batch by uploading the contents of the file. This closes the
	 * output stream.
	 */
	public void createBatch(FileOutputStream tmpOut, File tmpFile, List<BatchInfo> batchInfos,
			RestConnection connection, JobInfo jobInfo) throws IOException, AsyncApiException {
		tmpOut.flush();
		tmpOut.close();
		FileInputStream tmpInputStream = new FileInputStream(tmpFile);
		try {
			BatchInfo batchInfo = connection.createBatchFromStream(jobInfo, tmpInputStream);
			// System.out.println(batchInfo);
			batchInfos.add(batchInfo);

		} finally {
			tmpInputStream.close();
		}
	}

	/**
	 * Closes the job
	 */
	public void closeJob(RestConnection connection, String jobId) throws AsyncApiException {
		JobInfo job = new JobInfo();
		job.setId(jobId);
		job.setState(JobStateEnum.Closed);
		connection.updateJob(job);
	}

	/**
	 * Wait for a job to complete by polling the Bulk API.
	 */
	public void awaitCompletion(RestConnection connection, JobInfo job, List<BatchInfo> batchInfoList)
			throws AsyncApiException {
		long sleepTime = 0L;
		Set<String> incomplete = new HashSet<String>();
		for (BatchInfo bi : batchInfoList) {
			incomplete.add(bi.getId());
		}
		while (!incomplete.isEmpty()) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
			}
			// System.out.println("We are uploading your file...Please wait..."
			// + incomplete.size());
			// System.out.println("We are uploading your file...Please
			// wait...");
			sleepTime = 10000L;
			BatchInfo[] statusList = connection.getBatchInfoList(job.getId()).getBatchInfo();
			for (BatchInfo b : statusList) {
				if (b.getState() == BatchStateEnum.Completed || b.getState() == BatchStateEnum.Failed) {
					if (incomplete.remove(b.getId())) {
						// System.out.println("BATCH STATUS:\n" + b);
					}
				}
			}
		}
	}

	/**
	 * Gets the results of the operation and checks for errors.
	 */
	public Boolean checkResults(RestConnection connection, JobInfo job, List<BatchInfo> batchInfoList)
			throws AsyncApiException, IOException {
		// batchInfoList was populated when batches were created and submitted
		List<String> Ids = new ArrayList<String>();
		Boolean isSuccess = true;
		for (BatchInfo b : batchInfoList) {
			CSVReader rdr = new CSVReader(connection.getBatchResultStream(job.getId(), b.getId()));
			List<String> resultHeader = rdr.nextRecord();
			int resultCols = resultHeader.size();

			List<String> row;
			while ((row = rdr.nextRecord()) != null) {
				Map<String, String> resultInfo = new HashMap<String, String>();
				for (int i = 0; i < resultCols; i++) {
					resultInfo.put(resultHeader.get(i), row.get(i));
				}
				boolean success = Boolean.valueOf(resultInfo.get("Success"));
				boolean created = Boolean.valueOf(resultInfo.get("Created"));
				String id = resultInfo.get("Id");
				String error = resultInfo.get("Error");
				if (success && created) {
					// System.out.println("Created row with id " + id);
					Ids.add(id);
					isSuccess = true;
				} else if (!success) {
					isSuccess = false;
					System.out.println("Failed with error: " + error);
				}
			}
		}
		return isSuccess;
	}

}
