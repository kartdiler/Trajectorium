package trajectorium.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

public class CommandHistory {
    private static final Logger log = LoggerFactory.getLogger(CommandHistory.class);

    private final Deque<Command> undoStack = new ArrayDeque<Command>();
    private final Deque<Command> redoStack = new ArrayDeque<Command>();
    private final int limit;

    private Runnable onChange = () -> {};

    public CommandHistory(int limit) {
        this.limit = limit;
    }

    public void execute(Command c) {
        log.info("Команда: {}", c.getName());
        c.execute();
        if (c.isUndoable()) {
            undoStack.push(c);
            if (undoStack.size() > limit) {
                undoStack.removeLast();
            }
            redoStack.clear();
        } else {
            undoStack.clear();
            redoStack.clear();
        }
        onChange.run();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }

        Command c = undoStack.pop();
        log.info("Отмена: {}", c.getName());
        c.undo();
        redoStack.push(c);
        onChange.run();
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }

        Command c = redoStack.pop();
        log.info("Повтор: {}", c.getName());
        c.execute();
        undoStack.push(c);
        onChange.run();
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }
}
