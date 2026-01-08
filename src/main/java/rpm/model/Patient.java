package rpm.model;

import java.util.Objects;

public final class Patient {

    private final String patientId;
    private final String name;
    private final int age;
    private final String ward;
    private final String email;
    private final String emergencyContact;

    public Patient(
            String patientId,
            String name,
            int age,
            String ward,
            String email,
            String emergencyContact
    ) {
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
        return age == other.age()
                && Objects.equals(patientId, other.patientId())
                && Objects.equals(name, other.name())
                && Objects.equals(ward, other.ward())
                && Objects.equals(email, other.email())
                && Objects.equals(emergencyContact, other.emergencyContact());
    }


    @Override
    public int hashCode() {
        return Objects.hash(
                patientId,
                name,
                age,
                ward,
                email,
                emergencyContact
        );
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
