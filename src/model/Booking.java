package model;

import java.sql.Date;

//---------------------Booking Schema-----------------
public class Booking {
    private int userId;
    private int carId;
    private Date startDate;

    public Booking(int userId, int carId, Date startDate) {
        this.userId = userId;
        this.carId = carId;
        this.startDate = startDate;
    }

    public int getUserId() { return userId; }
    public int getCarId() { return carId; }
    public Date getStartDate() { return startDate; }
}
