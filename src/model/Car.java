package model;

//----------------------Car Schema--------------------
public class Car {
    private int id;
    private String model;
    private double price;
    private boolean available;

    public Car(int id, String model, double price, boolean available) {
        this.id = id;
        this.model = model;
        this.price = price;
        this.available = available;
    }

    public int getId() { return id; }
    public String getModel() { return model; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return available; }
}
