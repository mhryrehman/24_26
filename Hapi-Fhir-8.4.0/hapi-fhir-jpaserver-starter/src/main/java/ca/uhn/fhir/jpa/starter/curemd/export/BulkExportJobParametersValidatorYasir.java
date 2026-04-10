//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.batch2.api.IJobParametersValidator;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.binary.api.IBinaryStorageSvc;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryMatchResult;
import ca.uhn.fhir.jpa.searchparam.matcher.InMemoryResourceMatcher;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BulkExportJobParametersValidatorYasir implements IJobParametersValidator<BulkExportJobParameters> {
	/** @deprecated */
	@Deprecated(
			since = "6.3.10"
	)
	public static final String UNSUPPORTED_BINARY_TYPE = "Binary";
	@Autowired
	private DaoRegistry myDaoRegistry;
	@Autowired
	private InMemoryResourceMatcher myInMemoryResourceMatcher;
	@Autowired(
			required = false
	)
	private IBinaryStorageSvc myBinaryStorageSvc;

	public BulkExportJobParametersValidatorYasir() {
	}

	@Nullable
	public List<String> validate(RequestDetails theRequestDetails, @Nonnull BulkExportJobParameters theParameters) {
		List<String> errorMsgs = new ArrayList();
		List<String> resourceTypes = theParameters.getResourceTypes();
		if (resourceTypes != null && !resourceTypes.isEmpty()) {
			Iterator var5 = theParameters.getResourceTypes().iterator();

			while(var5.hasNext()) {
				String resourceType = (String)var5.next();
				if (resourceType.equalsIgnoreCase("Binary")) {
					errorMsgs.add("Bulk export of Binary resources is forbidden");
				} else if (!this.myDaoRegistry.isResourceTypeSupported(resourceType)) {
					errorMsgs.add("Resource type " + resourceType + " is not a supported resource type!");
				}
			}
		}

		if (!"application/fhir+ndjson".equalsIgnoreCase(theParameters.getOutputFormat())) {
			errorMsgs.add("The only allowed format for Bulk Export is currently application/fhir+ndjson");
		}

		if (!StringUtils.isBlank(theParameters.getExportIdentifier()) && this.myBinaryStorageSvc != null && !this.myBinaryStorageSvc.isValidBinaryContentId(theParameters.getExportIdentifier())) {
			errorMsgs.add("Export ID does not conform to the current blob storage implementation's limitations.");
		}

		BulkExportJobParameters.ExportStyle style = theParameters.getExportStyle();
		if (style == null) {
			errorMsgs.add("Export style is required");
		} else {
			switch (style) {
				case GROUP:
					if (theParameters.getGroupId() == null || theParameters.getGroupId().isEmpty()) {
						errorMsgs.add("Group export requires a group id, but none provided.");
					}
				case SYSTEM:
				case PATIENT:
			}
		}

		Iterator var12 = theParameters.getPostFetchFilterUrls().iterator();

		while(true) {
			while(var12.hasNext()) {
				String next = (String)var12.next();
				if (next.contains("?") && !StringUtils.isBlank(next.substring(next.indexOf(63) + 1))) {
					String resourceType = next.substring(0, next.indexOf(63));
					if (!this.myDaoRegistry.isResourceTypeSupported(resourceType)) {
						errorMsgs.add("Invalid post-fetch filter URL, unknown resource type: " + resourceType);
					} else {
						try {
							InMemoryMatchResult inMemoryMatchResult = this.myInMemoryResourceMatcher.canBeEvaluatedInMemory(next);
							if (!inMemoryMatchResult.supported()) {
								errorMsgs.add("Invalid post-fetch filter URL, filter is not supported for in-memory matching \"" + next + "\". Reason: " + inMemoryMatchResult.getUnsupportedReason());
							}
						} catch (InvalidRequestException var10) {
							errorMsgs.add("Invalid post-fetch filter URL. Reason: " + var10.getMessage());
						}
					}
				} else {
					errorMsgs.add("Invalid post-fetch filter URL, must be in the format [resourceType]?[parameters]: " + next);
				}
			}

			return errorMsgs;
		}
	}
}
