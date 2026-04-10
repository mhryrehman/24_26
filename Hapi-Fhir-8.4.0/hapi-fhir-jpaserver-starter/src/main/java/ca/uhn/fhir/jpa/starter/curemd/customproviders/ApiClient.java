package ca.uhn.fhir.jpa.starter.curemd.customproviders;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class ApiClient {

    private static final String API_URL = "https://app11.curemd.net/api/patientApp/patients";
    private static final String BEARER_TOKEN = "Bearer YOUR_ACCESS_TOKEN";

    private final RestTemplate restTemplate;

    public ApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public ca.uhn.fhir.jpa.starter.curemd.customproviders.PatientResponseDTO getPatientFromApi(String firstName, String lastName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("daOO-CEXnga0FXMUv3bsGtVi_7Zguu7XrW8Ybuj6svmw0Lyd4WJgbCX3QigIrNtjFoQWEmbsBQvTxk-BEKV0bE6GN2guDQS2J9ZS9W_PIJol9wuy6edKFaACQVqyFr9q2BTbGfrDutZyh9sy7oyRATfdwDuWPMyExi1kM5N_SOvkdn5__DzUOy4E0OZUcorTeTwQaXyUUOns0ECrRhVGaV3DWAOiKsR8q7p7jsiOfvIzeK9_p2KJo1gD38794Et6tZ30Pog9CBizkKTEwr5JSinJc08bpLYlqWMKRj6WaIjo_ydoo1ee6hj_QOEYNYK2WXLFuk4R0Xbhz2i9iGQ5jL4S2XDOqAook0A4D7IrQ2FyIbRIYF7FUrg6lsb4ueerFC4bLxM8FN48Ce3D8gmgVFKPYYW7U738lci9nybsPo5JkjumeolKaEjglVgkbmGbuYPrVF2OUeM0iVOC6JT1Oo69e_146AyrAX6Se_BH5tFiLryA39sIWl2i_5V4OJ2sdFQTqdiMwSJ6sd3yWt7vov5waNbf5sI72WqLJZgxVKGOXQtK"); // or use set("Authorization", ...)

        Map<String, String> body = new HashMap<>();
        body.put("FirsName", firstName);
        body.put("LastName", lastName);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<ca.uhn.fhir.jpa.starter.curemd.customproviders.PatientResponseDTO[]> response = restTemplate.exchange(
                API_URL,
                HttpMethod.POST,
                request,
                ca.uhn.fhir.jpa.starter.curemd.customproviders.PatientResponseDTO[].class
        );

        ca.uhn.fhir.jpa.starter.curemd.customproviders.PatientResponseDTO[] patients = response.getBody();
        return (patients != null && patients.length > 0) ? patients[0] : null;
    }
}
