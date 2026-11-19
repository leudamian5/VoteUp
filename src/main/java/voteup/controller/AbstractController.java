package voteup.controller;

import voteup.model.Model;
import voteup.view.View;

/**
 * Abstract root of the controller hierarchy. Holds references to the
 * linked {@link Model} and {@link View}.
 */
public abstract class AbstractController implements Controller {

    /**
     * View managed by this controller.
     */
    private View view;

    /**
     * Model managed by this controller.
     */
    private Model model;

    /**
     * Creates a controller with no model or view yet.
     */
    public AbstractController() {
        // default
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Model getModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(View view) {
        this.view = view;
    }
}
