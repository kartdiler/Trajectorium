package trajectorium.command;

public interface Command {
    void execute();
    void undo();
    String getName();
    default boolean isUndoable() {
        return true;
    }
}
