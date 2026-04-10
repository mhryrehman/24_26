package com.spi.smart_on_fhir.models;

/**
 * Author: Yasir Rehman
 * Description:
 * PatientDTO.
 */
public class PatientDTO {
    String id;
    String name;
    String dob;

    public PatientDTO(String id, String name, String dob) {
        this.id = id;
        this.name = name;
        this.dob = dob;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDob() {
        return dob;
    }

    @Override
    public String toString() {
        return "PatientStruct{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dob='" + dob + '\'' +
                '}';
    }
}
