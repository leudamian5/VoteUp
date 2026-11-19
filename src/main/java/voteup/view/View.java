package voteup.view;

import voteup.controller.Controller;
import voteup.model.Model;

/**
 * Contract for classes that play the View role in VoteUp's MVC architecture.
 */
public interface View {

    /**
     * Associates a controller with this view.
     *
     * @param controller controller to use
     */
    void setController(Controller controller);

    /**
     * Returns the controller associated with this view.
     *
     * @return current controller
     */
    Controller getController();

    /**
     * Associates a model with this view (and typically registers as a listener).
     *
     * @param model model to observe
     */
    void setModel(Model model);

    /**
     * Returns the model observed by this view.
     *
     * @return current model
     */
    Model getModel();
}
