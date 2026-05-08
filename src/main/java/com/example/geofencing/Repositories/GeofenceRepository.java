package com.example.geofencing.Repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.geofencing.Entities.Geofence;
 
@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {
 
    Page<Geofence> findByOwnerIdAndIsActive(UUID ownerId, Boolean isActive, Pageable pageable);
 
    Page<Geofence> findByOwnerId(UUID ownerId, Pageable pageable);
 
    @Query(value = """
        SELECT g.* FROM geofences g
        WHERE g.is_active = true
        AND ST_Contains(g.geometry, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
        """, nativeQuery = true)
    List<Geofence> findContainingPoint(@Param("lat") double lat, @Param("lng") double lng);
 
    @Query(value = """
        SELECT g.* FROM geofences g
        WHERE g.is_active = true
        AND (g.owner_id = :userId OR g.is_global = true)
        AND ST_Contains(g.geometry, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
        """, nativeQuery = true)
    List<Geofence> findContainingPointForUser(
        @Param("lat") double lat, @Param("lng") double lng, @Param("userId") UUID userId);
 
    @Query(value = """
        SELECT g.* FROM geofences g
        WHERE g.is_active = true
        AND ST_DWithin(
            g.geometry::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            :radiusMeters
        )
        """, nativeQuery = true)
    List<Geofence> findWithinRadius(
        @Param("lat") double lat, @Param("lng") double lng, @Param("radiusMeters") double radiusMeters);
 
    @Query(value = """
        SELECT g.*, ST_Distance(
            g.geometry::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        ) AS distance
        FROM geofences g
        WHERE g.owner_id = :ownerId AND g.is_active = true
        ORDER BY distance
        LIMIT :limit
        """, nativeQuery = true)
    List<Geofence> findNearestByOwner(
        @Param("lat") double lat, @Param("lng") double lng,
        @Param("ownerId") UUID ownerId, @Param("limit") int limit);
 
    @Query(value = """
        SELECT ST_Distance(
            g.geometry::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        ) FROM geofences g WHERE g.id = :geofenceId
        """, nativeQuery = true)
    Double getDistanceToGeofence(
        @Param("geofenceId") UUID geofenceId, @Param("lat") double lat, @Param("lng") double lng);
}
