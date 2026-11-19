package voteup.model;

import java.awt.event.ActionEvent;

/**
 * Event used to notify listeners of changes in model state.
 * Extends {@link ActionEvent} for compatibility with the course MVC pattern.
 */
public class ModelEvent extends ActionEvent {

    /**
     * Optional payload associated with the change (e.g. a Post or User).
     */
    private final Object data;

    /**
     * Creates a model event without extra payload data.
     *
     * @param source        event source (typically the model)
     * @param id            event id
     * @param actionCommand descriptive command string
     */
    public ModelEvent(Object source, int id, String actionCommand) {
        this(source, id, actionCommand, null);
    }

    /**
     * Creates a model event with optional payload data.
     *
     * @param source        event source (typically the model)
     * @param id            event id
     * @param actionCommand descriptive command string
     * @param data          optional payload
     */
    public ModelEvent(Object source, int id, String actionCommand, Object data) {
        super(source, id, actionCommand);
        this.data = data;
    }

    /**
     * Returns optional payload data for this event.
     *
     * @return payload or {@code null}
     */
    public Object getData() {
        return data;
    }
}
