package rpm.model;

public record Patient(
        String patientId,
        String name,
        int age,
        String ward,
        String email,
        String emergencyContact
) {}