package voteup.model;

/**
 * Contract for classes that play the Model role in VoteUp's MVC architecture.
 * Models notify interested observers when their state changes.
 */
public interface Model {

    /**
     * Notifies registered listeners that this model has changed.
     *
     * @param event description of the change
     */
    void notifyChanged(ModelEvent event);
}
