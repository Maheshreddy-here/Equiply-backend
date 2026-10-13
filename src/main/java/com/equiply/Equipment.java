package com.equiply;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_holder_id")
    private User currentHolder;

    private LocalDateTime dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedValue;

    @Column(length = 30)
    private String conditionGrade;

    @Column(precision = 10, scale = 2)
    private BigDecimal rentalPricePerDay;

    @Column(precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal recommendedRentPerDay;

    @Column(precision = 10, scale = 2)
    private BigDecimal recommendedDeposit;

    public Equipment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }
    public User getCurrentHolder() { return currentHolder; }
    public void setCurrentHolder(User currentHolder) { this.currentHolder = currentHolder; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }
    public String getConditionGrade() { return conditionGrade; }
    public void setConditionGrade(String conditionGrade) { this.conditionGrade = conditionGrade; }
    public BigDecimal getRentalPricePerDay() { return rentalPricePerDay; }
    public void setRentalPricePerDay(BigDecimal rentalPricePerDay) { this.rentalPricePerDay = rentalPricePerDay; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getRecommendedRentPerDay() { return recommendedRentPerDay; }
    public void setRecommendedRentPerDay(BigDecimal recommendedRentPerDay) { this.recommendedRentPerDay = recommendedRentPerDay; }
    public BigDecimal getRecommendedDeposit() { return recommendedDeposit; }
    public void setRecommendedDeposit(BigDecimal recommendedDeposit) { this.recommendedDeposit = recommendedDeposit; }
    public boolean isArchived() {
    return archived;
}

public void setArchived(boolean archived) {
    this.archived = archived;
}
}
