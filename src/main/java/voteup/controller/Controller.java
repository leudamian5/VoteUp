package voteup.controller;

import voteup.model.Model;
import voteup.view.View;

/**
 * Contract for classes that play the Controller role in VoteUp's MVC architecture.
 */
public interface Controller {

    /**
     * Associates a model with this controller.
     *
     * @param model model to manage
     */
    void setModel(Model model);

    /**
     * Returns the model managed by this controller.
     *
     * @return current model
     */
    Model getModel();

    /**
     * Associates a view with this controller.
     *
     * @param view view to control
     */
    void setView(View view);

    /**
     * Returns the view controlled by this controller.
     *
     * @return current view
     */
    View getView();
}
