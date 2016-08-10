

	import java.io.*;
	import java.util.*;
	import java.text.*;
	import java.io.File;
	
	import com.sforce.ws.*;
	import com.sforce.soap.partner.*;
	import com.sforce.soap.partner.sobject.*;
	//import com.sforce.soap.partner.PartnerConnection;
	import com.sforce.soap.partner.SaveResult;
	import com.sforce.soap.partner.Error;
	import com.sforce.soap.partner.sobject.SObject;
	
	import com.sforce.async.*;
	import com.sforce.ws.ConnectionException;
	import com.sforce.ws.ConnectorConfig;
	
public class ConnectionPartner {
	  
	  /**
	   * Create the RestConnection used to call Bulk API operations.
	   */
	  public ConnectionInformation getRestConnection(String userName, String password, String endPoint)
	      throws ConnectionException, AsyncApiException {
		PartnerConnection pConn;
		ConnectorConfig partnerConfig = new ConnectorConfig();
	    partnerConfig.setUsername(userName);
	    partnerConfig.setPassword(password);
	    partnerConfig.setAuthEndpoint(endPoint);
	    // Creating the connection automatically handles login and stores
	    // the session in partnerConfig
	    //eConnection = Connector.newConnection(partnerConfig);
	    pConn = new PartnerConnection(partnerConfig);
	    // When PartnerConnection is instantiated, a login is implicitly
	    // executed and, if successful,
	    // a valid session is stored in the ConnectorConfig instance.
	    // Use this key to initialize a RestConnection:
	    ConnectorConfig config = new ConnectorConfig();
	    config.setSessionId(partnerConfig.getSessionId());
	    // The endpoint for the Bulk API service is the same as for the normal
	    // SOAP uri until the /Soap/ part. From here it's '/async/versionNumber'
	    String soapEndpoint = partnerConfig.getServiceEndpoint();
	    //System.out.println("soapEndpoint"+soapEndpoint);
	    String apiVersion = "17.0";
	    String restEndpoint = soapEndpoint.substring(0, soapEndpoint
	        .indexOf("Soap/"))
	        + "async/" + apiVersion;
	    config.setRestEndpoint(restEndpoint);
	    //System.out.println("restEndpoint.."+restEndpoint);
	    // This should only be false when doing debugging.
	    config.setCompression(true);
	    // Set this to true to see HTTP requests and responses on stdout
	    config.setTraceMessage(false);
	    RestConnection connection = new RestConnection(config);
	    return new ConnectionInformation(pConn,connection);
	  }

}
