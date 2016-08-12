/**
 * @author Srinizkumar Konakanchi
 *
 */

package com.xlsxReadWrite;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public abstract class createFileAbstract {

	private static final String SF_ADD_ATTACHMENT = "/services/data/v20.0/sobjects/Attachment/";

	List<List<String>> parseJSON(JSONArray jsonArray, List<String> col) throws Exception {
		List<List<String>> recList = new ArrayList<List<String>>();
		System.out.println("jsonArray :" + jsonArray);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObj = jsonArray.getJSONObject(i);
			List<String> data = new ArrayList<String>();
			for (String ccol : col) {
				String[] colObj = ccol.split("\\.");
				if (colObj.length == 1) {
					if (!jsonObj.get(ccol).toString().startsWith("null")) {
						data.add(jsonObj.get(ccol).toString());
					} else {
						data.add("");
					}
				} else if (colObj.length == 2) {
					JSONObject caUpload = jsonObj.getJSONObject(colObj[0]);
					try {
						if (!caUpload.get(colObj[1]).toString().startsWith("null")) {
							data.add(caUpload.get(colObj[1]).toString());
						} else {
							data.add("");
						}
					} catch (Exception e) {
						data.add("");
					}
				} else if (colObj.length == 3) {
					JSONObject caUpload = jsonObj.getJSONObject(colObj[0]);
					try {
						JSONObject obj = caUpload.getJSONObject(colObj[1]);
						data.add(obj.getString(colObj[2]));
					} catch (Exception e) {
						data.add("");
					}
				} else if (colObj.length == 4) {
					try {
						JSONObject caUpload = jsonObj.getJSONObject(colObj[0]);
						JSONObject rp = caUpload.getJSONObject(colObj[1]);
						JSONObject obj = rp.getJSONObject(colObj[2]);
						data.add(obj.getString(colObj[3]));
					} catch (Exception e) {
						data.add("");
					}
				} else {
					data.add("BUG" + colObj.length);
				}
			}
			recList.add(data);
		}
		return recList;
	}

	public void uploadAttachment(String sessionId, String instanceUrl, String uploadId, String fileName)
			throws Exception {
		// Read the file
		byte[] data = IOUtils.toByteArray(new FileInputStream(fileName)); // #1
		JSONObject content = new JSONObject(); // #2
		if (fileName != null) {
			content.put("Name", fileName); // #3
		}
		if (fileName != null) {
			content.put("Description", fileName); // #4
		}

		content.put("Body", new String(Base64.encodeBase64(data))); // #5
		content.put("ParentId", uploadId); // #6
		PostMethod post = new PostMethod(instanceUrl + SF_ADD_ATTACHMENT); // #7
		post.setRequestHeader("Authorization", "OAuth " + sessionId); // #8
		post.setRequestEntity(new StringRequestEntity(content.toString(), "application/json", null)); // #9
		String contentId = null;
		HttpClient httpclient = new HttpClient();

		try {
			httpclient.executeMethod(post); // #10
			if (post.getStatusCode() == HttpStatus.SC_CREATED) {
				JSONObject response = new JSONObject(
						new JSONTokener(new InputStreamReader(post.getResponseBodyAsStream())));
				if (response.getBoolean("success")) {
					contentId = response.getString("id"); // #11
				}
			} else if (post.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				throw new Exception();
			}
		} finally {
			post.releaseConnection();
		}
	}

}
