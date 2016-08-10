import com.sforce.async.*; 
import com.sforce.soap.partner.*;

public class ConnectionInformation {

	    PartnerConnection pConn;
	    RestConnection rConn;

	    public ConnectionInformation(PartnerConnection pConn, RestConnection rConn) {
	        this.pConn = pConn;
	        this.rConn = rConn;
	    }

}
