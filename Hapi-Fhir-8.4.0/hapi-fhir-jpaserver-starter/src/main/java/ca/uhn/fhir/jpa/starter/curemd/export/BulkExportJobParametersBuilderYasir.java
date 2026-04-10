//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ca.uhn.fhir.jpa.starter.curemd.export;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.api.server.bulk.BulkExportJobParameters;
import ca.uhn.fhir.util.ArrayUtil;
import ca.uhn.fhir.util.DatatypeUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.*;
import java.util.stream.Collectors;

public class BulkExportJobParametersBuilderYasir {
	public static final String FARM_TO_TABLE_TYPE_FILTER_REGEX = "(?:,)(?=[A-Z][a-z]+\\?)";
	private Set<String> myResourceTypes;
	private Date mySince;
	private Date myUntil;
	private Set<String> myFilters;
	private String myOutputFormat;
	private BulkExportJobParameters.ExportStyle myExportStyle;
	private List<String> myPatientIds = new ArrayList();
	private String myGroupId;
	private boolean myExpandMdm;
	private RequestPartitionId myPartitionId;
	private String myExportIdentifier;
	private Set<String> myPostFetchFilterUrls;

	public BulkExportJobParametersBuilderYasir() {
	}

	public BulkExportJobParametersBuilderYasir resourceTypes(IPrimitiveType<String> theResourceTypes) {
		this.myResourceTypes = theResourceTypes == null ? null : ArrayUtil.commaSeparatedListToCleanSet(theResourceTypes.getValueAsString());
		return this;
	}

	public BulkExportJobParametersBuilderYasir since(IPrimitiveType<Date> theSince) {
		this.mySince = DatatypeUtil.toDateValue(theSince);
		return this;
	}

	public BulkExportJobParametersBuilderYasir until(IPrimitiveType<Date> theUntil) {
		this.myUntil = DatatypeUtil.toDateValue(theUntil);
		return this;
	}

	public BulkExportJobParametersBuilderYasir filters(List<IPrimitiveType<String>> theFilters) {
		this.myFilters = this.parseFilters(theFilters);
		return this;
	}

	public BulkExportJobParametersBuilderYasir outputFormat(IPrimitiveType<String> theOutputFormat) {
		this.myOutputFormat = theOutputFormat != null ? theOutputFormat.getValueAsString() : "application/fhir+ndjson";
		return this;
	}

	public BulkExportJobParametersBuilderYasir exportStyle(BulkExportJobParameters.ExportStyle theExportStyle) {
		this.myExportStyle = theExportStyle;
		return this;
	}

	public BulkExportJobParametersBuilderYasir patientIds(List<IPrimitiveType<String>> thePatientIds) {
		this.myPatientIds = thePatientIds == null ? null : (List)thePatientIds.stream().map(IPrimitiveType::getValueAsString).collect(Collectors.toList());
		return this;
	}

	public BulkExportJobParametersBuilderYasir groupId(IIdType theGroupId) {
		this.myGroupId = DatatypeUtil.toStringValue(theGroupId);
		return this;
	}

	public BulkExportJobParametersBuilderYasir expandMdm(IPrimitiveType<Boolean> theExpandMdm) {
		Boolean booleanValue = DatatypeUtil.toBooleanValue(theExpandMdm);
		this.myExpandMdm = booleanValue != null && booleanValue;
		return this;
	}

	public BulkExportJobParametersBuilderYasir partitionId(RequestPartitionId thePartitionId) {
		this.myPartitionId = thePartitionId;
		return this;
	}

	public BulkExportJobParametersBuilderYasir exportIdentifier(IPrimitiveType<String> theExportIdentifier) {
		this.myExportIdentifier = DatatypeUtil.toStringValue(theExportIdentifier);
		return this;
	}

	public BulkExportJobParametersBuilderYasir postFetchFilterUrl(List<IPrimitiveType<String>> thePostFetchFilterUrl) {
		this.myPostFetchFilterUrls = this.parseFilters(thePostFetchFilterUrl);
		return this;
	}

	public BulkExportJobParameters build() {
		BulkExportJobParameters result = new BulkExportJobParameters();
		result.setExpandMdm(this.myExpandMdm);
		result.setExportIdentifier(this.myExportIdentifier);
		result.setExportStyle(this.myExportStyle);
		result.setFilters(this.myFilters);
		result.setGroupId(this.myGroupId);
		result.setOutputFormat(this.myOutputFormat);
		result.setPartitionId(this.myPartitionId);
		result.setPatientIds(this.myPatientIds);
		result.setResourceTypes(this.myResourceTypes);
		result.setSince(this.mySince);
		result.setUntil(this.myUntil);
		result.setPostFetchFilterUrls(this.myPostFetchFilterUrls);
		return result;
	}

	private Set<String> parseFilters(List<IPrimitiveType<String>> theFilters) {
		Set<String> retVal = null;
		if (theFilters != null) {
			retVal = new HashSet();
			Iterator var3 = theFilters.iterator();

			while(var3.hasNext()) {
				IPrimitiveType<String> next = (IPrimitiveType)var3.next();
				String typeFilterString = next.getValueAsString();
				Arrays.stream(typeFilterString.split("(?:,)(?=[A-Z][a-z]+\\?)")).filter(StringUtils::isNotBlank).forEach(retVal::add);
			}
		}

		return retVal;
	}
}
