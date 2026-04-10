package ca.uhn.fhir.jpa.starter.curemd.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Server Interceptor used to override the ValidationResult responses to always return successful even if the
 * validation returns errors.
 *
 */
@SuppressWarnings("unused")
@Interceptor
public class ValidationResultOverrideInterceptor {

	private static final Logger ourLog = LoggerFactory.getLogger(ValidationResultOverrideInterceptor.class);

	@Hook(Pointcut.VALIDATION_COMPLETED)
	public ValidationResult validationCompleted(
		IBaseResource theBaseResource,
		String theRawBaseResource,
		ValidationResult theValidationResult) {

		List<SingleValidationMessage> validationMessages = theValidationResult.getMessages();
		List<SingleValidationMessage> filteredValidationMessages = new ArrayList<>();
		for (SingleValidationMessage validationMessage : validationMessages  ){
			if (validationMessage.getSeverity() == ResultSeverityEnum.ERROR || validationMessage.getSeverity() == ResultSeverityEnum.FATAL) {
				ourLog.info("Error or Fatal result found in validation");
				filteredValidationMessages.add(validationMessage);
			}
		}
		theValidationResult = new ValidationResult(theValidationResult.getContext(), filteredValidationMessages);
		return theValidationResult;
	}


}

