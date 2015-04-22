package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.List;


@Entity
public class Star extends Model {
    @Id
    private long id;

    @ManyToMany(mappedBy="stars")
    private List<StarMap> starMaps;

    @OneToMany(mappedBy="star", cascade = CascadeType.ALL)
    private List<Coordinate> coordinates;

    private String name;
    private String type;

    /**
     * The EBean ORM finder method for database queries on ID.
     *
     * @return The finder method for Stars.
     */
    public static Finder<Long, Star> find() {
        return new Finder<Long, Star>(Long.class, Star.class);
    }

    /**
     * Constructor
     * @param starName
     * @param type
     */
    public Star(String starName, String type) {
        this.name = starName;
        this.type = type;
    }


    /**
     * Checks if the given star name exists in database.
     *
     * @param starName The star name to search.
     * @return True if star exists, false otherwise.
     */
    public static boolean starExists(String starName) {
        return Star.find().where().eq("name", starName).findUnique() != null;
    }

    /**
     * Gets the requested Star.
     *
     * @param starName The star to retrieve
     * @return The requested Star, or null if it doesn't exist.
     */
    public static Star getStar(String starName) {
        Star star = Star.find().where().eq("name", starName).findUnique();

        return star != null ? star : null;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<StarMap> getStarMaps() {
        return starMaps;
    }

    public void setStarMaps(List<StarMap> starMaps) {
        this.starMaps = starMaps;
    }
}
