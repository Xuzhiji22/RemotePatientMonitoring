package rpm.model;

import java.util.Objects;

public final class Patient {

    private final String patientId;
    private final String name;
    private final int age;
    private final String ward;
    private final String email;
    private final String emergencyContact;

    public Patient(String patientId,
                   String name,
                   int age,
                   String ward,
                   String email,
                   String emergencyContact) {

        if (patientId == null || patientId.isBlank()) {
            throw new IllegalArgumentException("patientId cannot be null or empty");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }

        if (age < 0) {
            throw new IllegalArgumentException("age cannot be negative");
        }

        if (ward == null || ward.isBlank()) {
            throw new IllegalArgumentException("ward cannot be null or empty");
        }

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("email must contain @");
        }

        if (emergencyContact == null || emergencyContact.isBlank()) {
            throw new IllegalArgumentException("emergencyContact cannot be empty");
        }



        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.ward = ward;
        this.email = email;
        this.emergencyContact = emergencyContact;
    }
    // Package-private constructor for tests (NO validation)
    Patient(String patientId,
            String name,
            int age,
            String ward,
            String email,
            String emergencyContact,
            boolean skipValidation) {

        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.ward = ward;
        this.email = email;
        this.emergencyContact = emergencyContact;
    }





    public String patientId() {
        return patientId;
    }

    public String name() {
        return name;
    }

    public int age() {
        return age;
    }

    public String ward() {
        return ward;
    }

    public String email() {
        return email;
    }

    public String emergencyContact() {
        return emergencyContact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient)) return false;
        Patient other = (Patient) o;
        return Objects.equals(patientId, other.patientId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(patientId);
    }







    @Override
    public String toString() {
        return "Patient[" +
                "patientId=" + patientId +
                ", name=" + name +
                ", age=" + age +
                ", ward=" + ward +
                ", email=" + email +
                ", emergencyContact=" + emergencyContact +
                "]";
    }
}



