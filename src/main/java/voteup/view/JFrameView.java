package voteup.view;

import javax.swing.JFrame;

import voteup.controller.Controller;
import voteup.model.AbstractModel;
import voteup.model.Model;
import voteup.model.ModelListener;

/**
 * Abstract Swing frame view. Registers itself as a {@link ModelListener}
 * on the associated model.
 */
public abstract class JFrameView extends JFrame implements View, ModelListener {

    /**
     * Model this view observes.
     */
    private Model model;

    /**
     * Controller this view uses for user actions.
     */
    private Controller controller;

    /**
     * Creates a frame view linked to the given model and controller.
     *
     * @param model      model to observe
     * @param controller controller for actions
     */
    public JFrameView(Model model, Controller controller) {
        setModel(model);
        setController(controller);
    }

    /**
     * Registers this view as a listener on the current {@link AbstractModel}.
     */
    public void registerWithModel() {
        ((AbstractModel) model).addModelListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Controller getController() {
        return controller;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setController(Controller controller) {
        this.controller = controller;
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
     * Also registers this view as a model listener.
     */
    @Override
    public void setModel(Model model) {
        this.model = model;
        registerWithModel();
    }

    /**
     * Makes this window visible and ready for interaction.
     */
    public abstract void showWindow();
}
