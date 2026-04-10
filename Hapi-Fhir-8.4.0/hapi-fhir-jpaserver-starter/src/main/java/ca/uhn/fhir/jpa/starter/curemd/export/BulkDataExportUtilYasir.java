//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.api.PreferHeader;
import ca.uhn.fhir.rest.api.server.IRestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class BulkDataExportUtilYasir {
	public static final List<String> PATIENT_BULK_EXPORT_FORWARD_REFERENCE_RESOURCE_TYPES = List.of("Practitioner", "Organization");
	public static final String UNSUPPORTED_BINARY_TYPE = "Binary";

	private BulkDataExportUtilYasir() {
	}

	public static void validatePreferAsyncHeader(ServletRequestDetails theRequestDetails, String theOperationName) {
		String preferHeader = theRequestDetails.getHeader("Prefer");
		PreferHeader prefer = RestfulServerUtils.parsePreferHeader((IRestfulServer)null, preferHeader);
		if (!prefer.getRespondAsync()) {
			String var10002 = Msg.code(513);
			throw new InvalidRequestException(var10002 + "Must request async processing for " + theOperationName);
		}
	}

	public static String getServerBase(ServletRequestDetails theRequestDetails) {
		return StringUtils.removeEnd(theRequestDetails.getServerBaseForRequest(), "/");
	}
}
