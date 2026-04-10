package ca.uhn.fhir.jpa.starter.curemd.customproviders.client;

// EhrPatientClient.java
import ca.uhn.fhir.jpa.starter.curemd.customproviders.models.EhrPatientData;
import ca.uhn.fhir.jpa.starter.curemd.customproviders.models.EhrPatientResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EhrPatientClient {

	private final RestTemplate restTemplate;

	public EhrPatientClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public EhrPatientData fetchPatientById(long patientId) {
		String url = UriComponentsBuilder
			.fromHttpUrl("http://cmdlhrltx579:7777/api/FHIR/Patient/{id}")
			.buildAndExpand(patientId)
			.toUriString();

		ResponseEntity<EhrPatientResponse> response =
			restTemplate.getForEntity(url, EhrPatientResponse.class);

		EhrPatientResponse body = response.getBody();
		if (body == null || !body.isSuccess() || body.getData() == null) {
			throw new IllegalStateException("EHR patient response is empty or unsuccessful");
		}

		return body.getData();
	}
}
