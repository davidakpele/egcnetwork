package com.example.geofencing.Components;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.springframework.stereotype.Component;

import com.example.geofencing.Enums.GeofenceType;
import com.example.geofencing.Payloads.CreateGeofenceRequest;
 
@Component
public class GeometryUtils {
 
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
 
    public static Point createPoint(double lat, double lng) {
        return FACTORY.createPoint(new Coordinate(lng, lat));
    }
 
    public static Geometry createGeofenceGeometry(GeofenceType type,
                                                   List<CreateGeofenceRequest.CoordinateDto> coordinates,
                                                   Double centerLat, Double centerLng, Double radiusMeters) {
        return switch (type) {
            case POLYGON, RECTANGLE -> createPolygon(coordinates);
            case CIRCLE -> createCircle(centerLat, centerLng, radiusMeters);
        };
    }
 
    public static Polygon createPolygon(List<CreateGeofenceRequest.CoordinateDto> coordinates) {
        if (coordinates == null || coordinates.size() < 3) {
            throw new IllegalArgumentException("Polygon requires at least 3 coordinates");
        }
        Coordinate[] coords = new Coordinate[coordinates.size() + 1];
        for (int i = 0; i < coordinates.size(); i++) {
            coords[i] = new Coordinate(coordinates.get(i).getLongitude(), coordinates.get(i).getLatitude());
        }
        coords[coordinates.size()] = coords[0]; // close ring
        return FACTORY.createPolygon(coords);
    }
 
    public static Geometry createCircle(double centerLat, double centerLng, double radiusMeters) {
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory(FACTORY);
        shapeFactory.setNumPoints(64);
        shapeFactory.setCentre(new Coordinate(centerLng, centerLat));
        double radiusDegrees = radiusMeters / 111320.0; // approximate meters to degrees
        shapeFactory.setWidth(radiusDegrees * 2);
        shapeFactory.setHeight(radiusDegrees * 2);
        return shapeFactory.createCircle();
    }
 
    public static boolean containsPoint(Geometry geometry, double lat, double lng) {
        Point point = createPoint(lat, lng);
        return geometry.contains(point);
    }
 
    public static double distanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dPhi/2) * Math.sin(dPhi/2)
            + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda/2) * Math.sin(dLambda/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
 
    public static List<CreateGeofenceRequest.CoordinateDto> geometryToCoordinates(Geometry geometry) {
        if (geometry == null) return List.of();
        Coordinate[] coords = geometry.getCoordinates();
        return java.util.Arrays.stream(coords)
            .map(c -> new CreateGeofenceRequest.CoordinateDto(c.getY(), c.getX()))
            .toList();
    }
}