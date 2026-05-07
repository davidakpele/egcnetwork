package com.example.geofencing.Entities;


import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import com.example.geofencing.Enums.AlertType;
import com.example.geofencing.Enums.GeofenceShape;
import com.example.geofencing.Enums.TriggerOnType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "geofences")
@SQLDelete(sql = "UPDATE geofences SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GeofenceShape shape = GeofenceShape.CIRCLE;

    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point center;

    private Double radius;

    @Column(columnDefinition = "GEOMETRY(Geometry, 4326)")
    private Geometry boundary;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "is_global", nullable = false)
    @Builder.Default
    private boolean isGlobal = false;

    @Column(name = "dwell_time_ms", nullable = false)
    @Builder.Default
    private int dwellTimeMs = 60_000;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_on", nullable = false)
    @Builder.Default
    private TriggerOnType triggerOn = TriggerOnType.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    @Builder.Default
    private AlertType alertType = AlertType.ALL;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "max_alerts")
    @Builder.Default
    private int maxAlerts = 0;

    @Column(name = "alert_count", nullable = false)
    @Builder.Default
    private int alertCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> metadata = Map.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean hasReachedMaxAlerts() {
        return maxAlerts > 0 && alertCount >= maxAlerts;
    }

    public void incrementAlertCount() {
        this.alertCount++;
    }
}