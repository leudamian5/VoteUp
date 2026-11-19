package voteup.model;

/**
 * Observer interested in {@link ModelEvent} notifications from a {@link Model}.
 */
public interface ModelListener {

    /**
     * Called when a model this listener is registered with has changed.
     *
     * @param event the change event
     */
    void modelChanged(ModelEvent event);
}
