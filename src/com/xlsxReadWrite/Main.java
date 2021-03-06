/**
 * @author Srinizkumar Konakanchi
 *
 */

package com.xlsxReadWrite;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sforce.async.AsyncApiException;
import com.sforce.ws.ConnectionException;

public class Main {

	public static void main(String[] args) throws AsyncApiException, ConnectionException, IOException {

		// Salesforce.com credentials
		String userName = "upload@test.com";
		String password = "";
		String endPnt = "https://test.salesforce.com/services/Soap/u/37.0";

		BulkLoader example = new BulkLoader();
		example.run(userName, password, endPnt);

	}

}
