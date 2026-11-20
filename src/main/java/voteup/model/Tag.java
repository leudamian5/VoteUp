package voteup.model;

import java.util.Objects;

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
     * Normalizes a raw tag string: trim, strip leading {@code #}, lowercase.
     *
     * @param raw raw tag text
     * @return normalized tag
     */
    public static Tag normalize(String raw) {
        String n = raw == null ? "" : raw.trim();
        if (n.startsWith("#")) {
            n = n.substring(1).trim();
        }
        return new Tag(n.toLowerCase());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag)) {
            return false;
        }
        Tag tag = (Tag) o;
        return Objects.equals(
                name == null ? null : name.toLowerCase(),
                tag.name == null ? null : tag.name.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name == null ? null : name.toLowerCase());
    }
}
