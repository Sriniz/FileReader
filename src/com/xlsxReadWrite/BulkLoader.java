/**
 * @author Srinizkumar Konakanchi
 *
 */

package com.xlsxReadWrite;

import java.io.*;
import java.util.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.*;

import com.sforce.soap.partner.*;
import com.sforce.ws.*;
import com.sforce.async.*;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.util.*;

public class BulkLoader {

	public String endPoint;

	// the sObjects being uploaded
	private String sObjectParent = "CA_Upload__c";
	private String sObject = "CA_Upload_Record__c";
	private Boolean isSuccessfulUpload = false;

	String Ids = null;

	// the CSV file being uploaded
	// ie /Users/Sriniz/Desktop/BulkAPI/myfile.csv
	// public String csvFileName; //= "C:/Users/ctsuser1/Desktop/CAS 2
	// Project/ER69 - Local Clients/Client Avails Template - Copy.csv";

	public void run(String userName, String password, String endPnt) {

		try {

			/*
			 * String currentDirectory = new java.io.File( "."
			 * ).getCanonicalPath();
			 * 
			 * csvFileName = currentDirectory.replaceAll("\\\\", "/") + "/Client
			 * Avails Import Template.csv";
			 * 
			 * File f = new File(csvFileName); if(!f.exists()) {
			 * System.out.println(
			 * "We could not locate 'Client Avails Import Template.csv' at "
			 * +currentDirectory+". Please place the file and try again!");
			 * System.exit(0); }
			 * 
			 * System.out.println("We are reading your file from..."
			 * +csvFileName);
			 */

			// Upload a CSV file for import
			// infinite loop
			while (true) {
				runJob(sObjectParent, sObject, userName, password, endPnt);
				Thread.sleep(30000);
			}
		} catch (IOException io) {
			io.printStackTrace();
			System.out.println(io.getMessage());
		} catch (NumberFormatException nf) {
			// run();
			nf.printStackTrace();
			System.out.println(nf.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

	}

	/**
	 * Creates a Bulk API job and uploads batches for a CSV file.
	 */
	public void runJob(String sObjectTypeParent, String sobjectType, String userName, String password, String endPnt)
			throws AsyncApiException, ConnectionException, IOException {
		System.out.println("Establishing connection to Salesforce...");
		ConnectionPartner pc = new ConnectionPartner();
		ConnectionInformation connInfo = pc.getRestConnection(userName, password, endPnt);
		PartnerConnection partConn = connInfo.pConn;
		RestConnection connection = connInfo.rConn;
		ConnectorConfig partnerConfig = connInfo.partnerConfig;
		String restEndPoint = partnerConfig.getServiceEndpoint().substring(0,partnerConfig.getServiceEndpoint().indexOf(".com")+4);

		QueryResult queryResults = partConn
				.query("SELECT Id FROM CA_Upload__c WHERE Upload_Status__c = 'Uploaded' ORDER BY CreatedDate ASC");
		System.out.println("query results size..." + queryResults.getSize());

		for (Integer i = 0; i < queryResults.getSize(); i++) {

			CreateBulkLoadJobs blkjobs = null;
			JobInfo job = null;
			try {
				Ids = (String) queryResults.getRecords()[i].getField("Id");
				System.out.println("Processing Batch Id - " + Ids);

				// Initiate CA upload records

				blkjobs = new CreateBulkLoadJobs();
				job = blkjobs.createJob(sobjectType, connection);
				BatchInformation btch = blkjobs.createBatchesFromCSVFile(partConn, connection, job, Ids, false);
				List<BatchInfo> batchInfoList = btch.batchInfoList;
				String atchId = btch.attachmentId;
				Boolean isSuccesful = btch.isSuccessful;
				blkjobs.closeJob(connection, job.getId());
				blkjobs.awaitCompletion(connection, job, batchInfoList);
				isSuccessfulUpload = blkjobs.checkResults(connection, job, batchInfoList);
				
				// Update the Status of parent record to 'Waiting To Process' or
				// 'Failed'
				if (isSuccesful && isSuccessfulUpload)
					updateCAUpload(partnerConfig.getSessionId(), restEndPoint,
							Ids, "wp");
				else
					updateCAUpload(partnerConfig.getSessionId(), restEndPoint,
							Ids, "f");

			} catch (Exception e) {
				try {
					updateCAUpload(partnerConfig.getSessionId(), restEndPoint,
							Ids, "f");
					System.out.println("Status of upload request has been updated to Failed : " + Ids);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		}

		QueryResult queryResults1 = partConn
				.query("SELECT Id FROM CA_Upload__c WHERE Upload_Status__c = 'Preparing File'");
		System.out.println("File pending results size..." + queryResults1.getSize());

		for (Integer i = 0; i < queryResults1.getSize(); i++) {
			Ids = (String) queryResults1.getRecords()[i].getField("Id");
			System.out.println("Creating xlsx file for - " + Ids);

			CreateXLSXFile cx = new CreateXLSXFile();
			cx.createFile(partConn, connection, partnerConfig, Ids);

			try {
				updateCAUpload(partnerConfig.getSessionId(), restEndPoint, Ids,
						"c");
				System.out.println("Error File has been prepared and status is set to Completed : " + Ids);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void updateCAUpload(String sessionID, String instanceUrl, String uploadId, String type) throws Exception {
		PostMethod post = new PostMethod(instanceUrl + "/services/data/v37.0/sobjects/CA_Upload__c/" + uploadId) {
			@Override
			public String getName() {
				return "PATCH";
			}
		};
		post.setRequestHeader("Authorization", "Bearer " + sessionID);
		HttpClient httpclient = new HttpClient();
		JSONObject caUpload = new JSONObject();
		if (type == "c")
			caUpload.put("Upload_Status__c", "Completed");
		else if (type == "wp")
			caUpload.put("Upload_Status__c", "Waiting To Process");
		else if (type == "f")
			caUpload.put("Upload_Status__c", "Failed");
		post.setRequestEntity(new StringRequestEntity(caUpload.toString(), "application/json", "UTF-8"));
		int sc = httpclient.executeMethod(post);
		System.out.println("updateStatus " + sc);
		return;
	}

}
