package student.inti.gymratdev3;

public class Workout {
    private String focusArea;
    private String equipment;
    private String preparation;
    private String execution;

    // Constructor to initialize the Workout object
    public Workout(String focusArea, String equipment, String preparation, String execution) {
        this.focusArea = focusArea;
        this.equipment = equipment;
        this.preparation = preparation;
        this.execution = execution;
    }

    // Getters for each field
    public String getFocusArea() {
        return focusArea;
    }

    public String getEquipment() {
        return equipment;
    }

    public String getPreparation() {
        return preparation;
    }

    public String getExecution() {
        return execution;
    }

    // Setters for each field (optional if you want to modify the fields)
    public void setFocusArea(String focusArea) {
        this.focusArea = focusArea;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public void setPreparation(String preparation) {
        this.preparation = preparation;
    }

    public void setExecution(String execution) {
        this.execution = execution;
    }

    // Method to return a formatted string representation of the Workout object
    @Override
    public String toString() {
        return "Workout{" +
                "focusArea='" + focusArea + '\'' +
                ", equipment='" + equipment + '\'' +
                ", preparation='" + preparation + '\'' +
                ", execution='" + execution + '\'' +
                '}';
    }

    // Utility method to check if the workout has valid data
    public boolean isValidWorkout() {
        return focusArea != null && !focusArea.isEmpty() &&
                equipment != null && !equipment.isEmpty() &&
                preparation != null && !preparation.isEmpty() &&
                execution != null && !execution.isEmpty();
    }
}
