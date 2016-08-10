

	import java.io.*;
	import java.util.*;
	import java.text.*;
	import java.io.File;
	
	import com.sforce.soap.partner.*;
	import com.sforce.ws.*;
	import com.sforce.async.*;
	import com.sforce.soap.partner.QueryResult;
	import com.sforce.ws.ConnectionException;
	import com.sforce.ws.ConnectorConfig;
	import com.sforce.ws.util.*;

	public class BulkLoader {

	  // Salesforce.com credentials
	  private String userName = "srinizkumar.konakanchi@wbconsultant.com.dv";
	  private String password = "Indian06#BCHi3drXR87QU8ncVsUKJ7Scb";
	  private String endPnt = "https://test.salesforce.com/services/Soap/u/17.0";

	  //enterprise connection
	  //static EnterpriseConnection eConnection;
	  
	  // the sObjects being uploaded
	  private String sObjectParent = "CA_Upload__c";
	  private String sObject = "CA_Upload_Record__c";
	  private Boolean isSuccessfulUpload = false;

	  String Ids = null;
	  
	  // the CSV file being uploaded
	  // ie /Users/Sriniz/Desktop/BulkAPI/myfile.csv
	  //public String csvFileName; //= "C:/Users/ctsuser1/Desktop/CAS 2 Project/ER69 - Local Clients/Client Avails Template - Copy.csv";
	  
	  
	  public static void main(String[] args) throws AsyncApiException,
	      ConnectionException, IOException {
	    BulkLoader example = new BulkLoader();
	    example.run();
	  }

	  
	  public void run() {

	    try {

	    	/*
	    	String currentDirectory = new java.io.File( "." ).getCanonicalPath();
		    
	        csvFileName = currentDirectory.replaceAll("\\\\", "/") + "/Client Avails Import Template.csv";

	        File f = new File(csvFileName);
	        if(!f.exists()) { 
	        	System.out.println("We could not locate 'Client Avails Import Template.csv' at "+currentDirectory+". Please place the file and try again!");
	        	System.exit(0);
	        }
	        
	        System.out.println("We are reading your file from..."+csvFileName);
			*/
	    	
	    	// Upload a CSV file for import
	    	//infinite loop
			while(true){
				runJob(sObjectParent, sObject, userName, password);
				Thread.sleep(30000);
			}
	    } catch (IOException io) {
	      io.printStackTrace();
	      System.out.println(io.getMessage());
	    } catch (NumberFormatException nf) {
	      run();
	    } catch (Exception e) {
	      e.printStackTrace();
	      System.out.println(e.getMessage());
	    }

	  }
  
	  
	  /**
	   * Creates a Bulk API job and uploads batches for a CSV file.
	   */
	  public void runJob(String sObjectTypeParent, String sobjectType, String userName, String password) throws AsyncApiException, ConnectionException,
	      IOException {
		System.out.println("Establishing connection to Salesforce...");  
		ConnectionPartner pc = new ConnectionPartner();
		ConnectionInformation connInfo = pc.getRestConnection(userName, password, endPnt);
        
		PartnerConnection partConn = connInfo.pConn;
		RestConnection connection = connInfo.rConn;
		
		/*
		//Insert parent record
	    //List<String> Ids;
	    CreateBulkLoadJobs blkjobsParent = new CreateBulkLoadJobs();
	    JobInfo job1 = blkjobsParent.createJob(sObjectTypeParent, connection);
	    File tmpFile1 = File.createTempFile("bulkAPIInsertParent", ".csv");
	    FileOutputStream tmpOutParent = new FileOutputStream(tmpFile1);
	    List<BatchInfo> batch1 = new ArrayList<BatchInfo>();
	    tmpOutParent.write("Upload_Status__c\n".getBytes("UTF-8"));
	    tmpOutParent.write("Waiting To Process\n".getBytes("UTF-8"));
	    blkjobsParent.createBatch(tmpOutParent, tmpFile1, batch1, connection, job1);
	    tmpFile1.delete();
	    blkjobsParent.closeJob(connection, job1.getId());
	    blkjobsParent.awaitCompletion(connection, job1, batch1);
	    Ids = blkjobsParent.checkResults(connection, job1, batch1);
	    System.out.println("Batch Id - "+Ids.get(0));
	    */
		
	    QueryResult queryResults = partConn.query("SELECT Id FROM CA_Upload__c WHERE Upload_Status__c = 'Uploaded' ORDER BY CreatedDate ASC");
	    System.out.println("query results size..."+queryResults.getSize());
	        
	    for(Integer i=0;i<queryResults.getSize();i++){
	    	
	    	CreateBulkLoadJobs blkjobs = null;
	    	JobInfo job = null;
	    	try{
	      	Ids = (String)queryResults.getRecords()[i].getField("Id");
	        System.out.println("Processing Batch Id - "+Ids);
	        
	        //QueryResult queryResultsAttach = partConn.query("SELECT Id,Body,Name,ParentId FROM Attachment where ParentId='"+Ids+"' AND Name like '%.csv'");
	        //String csvFile = (String)queryResultsAttach.getRecords()[0].getField("Body");

	        //Initiate CA upload records
	        blkjobs = new CreateBulkLoadJobs();
	        job = blkjobs.createJob(sobjectType, connection);
	        BatchInformation btch = blkjobs.createBatchesFromCSVFile(partConn, connection, job, Ids);
	        List<BatchInfo> batchInfoList = btch.batchInfoList;
	        String atchId = btch.attachmentId;
	        blkjobs.closeJob(connection, job.getId());
	        blkjobs.awaitCompletion(connection, job, batchInfoList);
	        isSuccessfulUpload = blkjobs.checkResults(connection, job, batchInfoList);
	        //System.out.println("File upload has been completed...");
	        
	        //Update the Staus of parent record to 'Waiting To Process'
		    CreateBulkLoadJobs blkjobsParent = new CreateBulkLoadJobs();
		    JobInfo job1 = blkjobsParent.createUpdateJob(sObjectTypeParent, connection);
		    File tmpFile1 = File.createTempFile("bulkAPIUpdateParent", ".csv");
		    FileOutputStream tmpOutParent = new FileOutputStream(tmpFile1);
		    List<BatchInfo> batch1 = new ArrayList<BatchInfo>();
		    if(isSuccessfulUpload){
		    	tmpOutParent.write("Id,Upload_Status__c\n".getBytes("UTF-8"));
		    	tmpOutParent.write((Ids+",Waiting To Process\n").getBytes("UTF-8"));
		    } else {
		    	tmpOutParent.write("Id,Upload_Status__c\n".getBytes("UTF-8"));
		    	tmpOutParent.write((Ids+",Failed\n").getBytes("UTF-8"));
		    }
		    blkjobsParent.createBatch(tmpOutParent, tmpFile1, batch1, connection, job1);
		    tmpFile1.delete();
		    blkjobsParent.closeJob(connection, job1.getId());
		    blkjobsParent.awaitCompletion(connection, job1, batch1);
		    blkjobsParent.checkResults(connection, job1, batch1);
		    System.out.println("Upload completed...Status of upload request has been updated to either Failed or Waiting To Process : "+Ids);
	    	} catch (Exception e) {
	    		//Update the Staus of parent record to 'Waiting To Process'
	    		blkjobs.closeJob(connection, job.getId());
			    CreateBulkLoadJobs blkjobsParent = new CreateBulkLoadJobs();
			    JobInfo job1 = blkjobsParent.createUpdateJob(sObjectTypeParent, connection);
			    File tmpFile1 = File.createTempFile("bulkAPIUpdateParent", ".csv");
			    FileOutputStream tmpOutParent = new FileOutputStream(tmpFile1);
			    List<BatchInfo> batch1 = new ArrayList<BatchInfo>();
			    tmpOutParent.write("Id,Upload_Status__c\n".getBytes("UTF-8"));
			    tmpOutParent.write((Ids+",Failed\n").getBytes("UTF-8"));
			    blkjobsParent.createBatch(tmpOutParent, tmpFile1, batch1, connection, job1);
			    tmpFile1.delete();
			    blkjobsParent.closeJob(connection, job1.getId());
			    blkjobsParent.awaitCompletion(connection, job1, batch1);
			    blkjobsParent.checkResults(connection, job1, batch1);
			    System.out.println("Status of upload request has been updated to Failed : "+Ids);
	    	}
	    	
	    }
	  }
	  
	}	
