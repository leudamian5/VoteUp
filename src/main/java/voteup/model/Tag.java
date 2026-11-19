package voteup.model;

/**
 * Normalized tag name that can be attached to posts.
 */
public class Tag {

    /** Tag name without leading {@code #}. */
    private String name;

    /**
     * @param name normalized tag name
     */
    public Tag(String name) {
        this.name = name;
    }

    /**
     * @return tag name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name tag name
     */
    public void setName(String name) {
        this.name = name;
    }
}
