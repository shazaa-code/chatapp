package model;

/**
 * Abstract base for all persistent domain objects.
 * Provides a shared integer ID field.
 */
public abstract class AbstractEntity {

    protected int id;

    public AbstractEntity() {}

    public AbstractEntity(int id) {
        this.id = id;
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }
}
