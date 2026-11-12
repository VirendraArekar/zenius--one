package com.zenius.one.modules.support;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sup_service_appointments")
public class SupServiceAppointmentEntity extends TenantScopedEntity {

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "appointment_number", nullable = false, length = 40)
    private String appointmentNumber;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "service_address", nullable = false, length = 255)
    private String serviceAddress;

    @Column(name = "technician_user_id")
    private UUID technicianUserId;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "SCHEDULED";

    @Column(name = "notes", length = 500)
    private String notes;

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public UUID getTechnicianUserId() {
        return technicianUserId;
    }

    public void setTechnicianUserId(UUID technicianUserId) {
        this.technicianUserId = technicianUserId;
    }

    public Instant getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(Instant scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

