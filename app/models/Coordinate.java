package models;

import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;


@Entity
public class Coordinate extends Model {
    @Id
    private long id;

    @ManyToOne
    private StarMap starMap;

    @ManyToOne
    private Star star;

    private Double x;
    private Double y;
    private Double radius;

    /**
     * The EBean ORM finder method for database queries on ID.
     *
     * @return The finder method for StarMap.
     */
    public static Finder<Long, Coordinate> find() {
        return new Finder<Long, Coordinate>(Long.class, Coordinate.class);
    }

    /**
     * Constructor
     *
     * @param starMap
     * @param star
     * @param x
     * @param y
     * @param radius
     */
    public Coordinate(StarMap starMap, Star star, Double x, Double y, Double radius) {
        this.starMap = starMap;
        this.star = star;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public StarMap getStarMap() {
        return starMap;
    }

    public void setStarMap(StarMap starMap) {
        this.starMap = starMap;
    }

    public Star getStar() {
        return star;
    }

    public void setStar(Star star) {
        this.star = star;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getRadius() {
        return radius;
    }

    public void setRadius(Double radius) {
        this.radius = radius;
    }
}
