package rpm.model;

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
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.ward = ward;
        this.email = email;
        this.emergencyContact = emergencyContact;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getWard() {
        return ward;
    }

    public String getEmail() {
        return email;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }
}
