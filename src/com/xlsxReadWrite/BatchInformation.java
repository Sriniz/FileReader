/**
 * @author Srinizkumar Konakanchi
 *
 */

package com.xlsxReadWrite;

import java.util.List;
import com.sforce.async.BatchInfo;

public class BatchInformation {

	List<BatchInfo> batchInfoList;
	String attachmentId;
	Boolean isSuccessful;

	public BatchInformation(List<BatchInfo> batchInfoList, String attachmentId, Boolean isSuccessful) {
		this.batchInfoList = batchInfoList;
		this.attachmentId = attachmentId;
		this.isSuccessful = isSuccessful;
	}

}
