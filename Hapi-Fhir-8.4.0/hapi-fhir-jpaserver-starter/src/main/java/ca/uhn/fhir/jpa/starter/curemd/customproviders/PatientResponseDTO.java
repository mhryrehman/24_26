package ca.uhn.fhir.jpa.starter.curemd.customproviders;

import java.util.List;

public class PatientResponseDTO {
    public int Id;
    public Address Address;
    public List<Phone> Phones;
    public Name Name;
    public String DOB;
    public String Email;
    public String Gender;
    public int LocationId;
    public String AccountNumber;

    public static class Address {
        public String Address1;
        public String Address2;
        public String City;
        public String State;
        public String ZipCode;
    }

    public static class Phone {
        public int Type;
        public String Number;
    }

    public static class Name {
        public String FirstName;
        public String LastName;
        public String MiddleName;
        public String Title;
        public String Suffix;
    }
}
