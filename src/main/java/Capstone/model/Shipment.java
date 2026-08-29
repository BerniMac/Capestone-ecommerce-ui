package Capstone.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Shipment {

    private String shipmentId;
    private String address;
    private LocalDate shipmentDate;
    private LocalDate deliveryDate;
    private String status;

    public Shipment() {
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getAddress() {
        return address;
    }

    public LocalDate getShipmentDate() {
        return shipmentDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public String getStatus() {
        return status;
    }
}
