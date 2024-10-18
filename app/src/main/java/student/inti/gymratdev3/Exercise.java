package student.inti.gymratdev3;

public class Exercise {
    private String name;
    private String focusArea;
    private String equipment;
    private String preparation;
    private String execution;

    // No-argument constructor required for Firestore
    public Exercise() {
    }

    // Parameterized constructor
    public Exercise(String name, String focusArea, String equipment, String preparation, String execution) {
        this.name = name;
        this.focusArea = focusArea;
        this.equipment = equipment;
        this.preparation = preparation;
        this.execution = execution;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFocusArea() {
        return focusArea;
    }

    public void setFocusArea(String focusArea) {
        this.focusArea = focusArea;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getPreparation() {
        return preparation;
    }

    public void setPreparation(String preparation) {
        this.preparation = preparation;
    }

    public String getExecution() {
        return execution;
    }

    public void setExecution(String execution) {
        this.execution = execution;
    }
}
