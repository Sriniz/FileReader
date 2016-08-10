import java.util.List;
import com.sforce.async.BatchInfo;


public class BatchInformation {

		List<BatchInfo> batchInfoList;
	    String attachmentId;

	    public BatchInformation(List<BatchInfo> batchInfoList, String attachmentId) {
	        this.batchInfoList = batchInfoList;
	        this.attachmentId = attachmentId;
	    }

}

