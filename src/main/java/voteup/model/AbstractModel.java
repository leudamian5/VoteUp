package voteup.model;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Abstract root of the model hierarchy. Provides observer-list management
 * and notification behavior for concrete models such as {@link VoteUpModel}.
 */
public abstract class AbstractModel implements Model {

    /**
     * Listeners interested in {@link ModelEvent}s from this model.
     */
    private final ArrayList<ModelListener> listeners = new ArrayList<>(5);

    /**
     * Creates an empty listener list.
     */
    public AbstractModel() {
        // default
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyChanged(ModelEvent event) {
        @SuppressWarnings("unchecked")
        ArrayList<ModelListener> list = (ArrayList<ModelListener>) listeners.clone();
        Iterator<ModelListener> it = list.iterator();
        while (it.hasNext()) {
            ModelListener ml = it.next();
            ml.modelChanged(event);
        }
    }

    /**
     * Registers a listener for model change events.
     *
     * @param l listener to add
     */
    public void addModelListener(ModelListener l) {
        listeners.add(l);
    }

    /**
     * Unregisters a listener for model change events.
     *
     * @param l listener to remove
     */
    public void removeModelListener(ModelListener l) {
        listeners.remove(l);
    }
}
