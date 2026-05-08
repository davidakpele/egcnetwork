package com.example.geofencing.Services;


import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.geofencing.Enums.GeofenceType;
import com.example.geofencing.Payloads.CreateGeofenceRequest;
import com.geofence.grpc.DeleteGeofenceRequest;
import com.geofence.grpc.GeofenceResponse;
import com.geofence.grpc.ListGeofencesResponse;
import com.geofence.grpc.PointCheckRequest;
import com.geofence.grpc.PointCheckResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceGrpcService extends GeofenceServiceGrpc.GeofenceServiceImplBase {
 
    private final GeofenceService geofenceService;
 
    @Override
    public void createGeofence(CreateGeofenceRequest grpcRequest, StreamObserver<GeofenceResponse> responseObserver) {
        try {
            var request = CreateGeofenceRequest.builder()
                .name(grpcRequest.getName())
                .description(grpcRequest.getDescription())
                .geofenceType(GeofenceType.valueOf(grpcRequest.getGeofenceType()))
                .centerLat(grpcRequest.getCenterLat() != 0 ? grpcRequest.getCenterLat() : null)
                .centerLng(grpcRequest.getCenterLng() != 0 ? grpcRequest.getCenterLng() : null)
                .radiusMeters(grpcRequest.getRadiusMeters() != 0 ? grpcRequest.getRadiusMeters() : null)
                .enterAlert(grpcRequest.getEnterAlert())
                .exitAlert(grpcRequest.getExitAlert())
                .dwellAlert(grpcRequest.getDwellAlert())
                .dwellTimeSeconds(grpcRequest.getDwellTimeSeconds())
                .color(grpcRequest.getColor())
                .coordinates(grpcRequest.getCoordinatesList().stream()
                    .map(c -> new com.example.geofencing.Payloads.CreateGeofenceRequest.CoordinateDto(c.getLatitude(), c.getLongitude()))
                    .collect(Collectors.toList()))
                .build();
 
            var result = geofenceService.createGeofence(UUID.fromString(grpcRequest.getOwnerId()), request);
            responseObserver.onNext(toGrpcResponse(result));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
 
    @Override
    public void getGeofence(GetGeofenceRequest request, StreamObserver<GeofenceResponse> responseObserver) {
        try {
            var result = geofenceService.getGeofence(UUID.fromString(request.getGeofenceId()));
            responseObserver.onNext(toGrpcResponse(result));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }
 
    @Override
    public void listGeofences(ListGeofencesRequest request, StreamObserver<ListGeofencesResponse> responseObserver) {
        try {
            var result = geofenceService.getMyGeofences(
                UUID.fromString(request.getOwnerId()),
                request.getPage(), request.getSize() > 0 ? request.getSize() : 20,
                request.getActiveOnly());
 
            var builder = ListGeofencesResponse.newBuilder()
                .setTotal((int) result.getTotalElements())
                .setPage(result.getPage())
                .setSize(result.getSize());
 
            result.getContent().forEach(g -> builder.addGeofences(toGrpcResponse(g)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
 
    @Override
    public void deleteGeofence(DeleteGeofenceRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            geofenceService.deleteGeofence(
                UUID.fromString(request.getGeofenceId()), UUID.fromString(request.getOwnerId()));
            responseObserver.onNext(DeleteResponse.newBuilder().setSuccess(true).setMessage("Deleted").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
 
    @Override
    public void checkPointInGeofences(PointCheckRequest request, StreamObserver<PointCheckResponse> responseObserver) {
        try {
            var containing = geofenceService.getContainingGeofences(
                request.getLatitude(), request.getLongitude(),
                UUID.fromString(request.getUserId()));
 
            var builder = PointCheckResponse.newBuilder().setCount(containing.size());
            containing.forEach(g -> builder.addContainingGeofences(toGrpcResponse(g)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
 
    @Override
    public void watchGeofenceEvents(WatchRequest request, StreamObserver<GeofenceEvent> responseObserver) {
        // Long-lived streaming: client watches for geofence events
        // Events will be pushed to this stream from the location processing pipeline
        // For now, register the observer to receive future events
        log.info("Client subscribed to geofence events for user: {}", request.getUserId());
        // In production, store observer in a registry and push events as they occur
        // responseObserver is kept open until client disconnects or error
    }
 
    private GeofenceResponse toGrpcResponse(com.geofence.model.dto.GeofenceDto g) {
        var builder = GeofenceResponse.newBuilder()
            .setId(g.getId().toString())
            .setName(g.getName())
            .setOwnerId(g.getOwnerId().toString())
            .setGeofenceType(g.getGeofenceType().name())
            .setIsActive(g.getIsActive() != null && g.getIsActive())
            .setEnterAlert(g.getEnterAlert() != null && g.getEnterAlert())
            .setExitAlert(g.getExitAlert() != null && g.getExitAlert())
            .setDwellAlert(g.getDwellAlert() != null && g.getDwellAlert())
            .setDwellTimeSeconds(g.getDwellTimeSeconds() != null ? g.getDwellTimeSeconds() : 300);
 
        if (g.getDescription() != null) builder.setDescription(g.getDescription());
        if (g.getColor() != null) builder.setColor(g.getColor());
        if (g.getCenterLat() != null) builder.setCenterLat(g.getCenterLat());
        if (g.getCenterLng() != null) builder.setCenterLng(g.getCenterLng());
        if (g.getRadiusMeters() != null) builder.setRadiusMeters(g.getRadiusMeters());
        if (g.getCreatedAt() != null)
            builder.setCreatedAtMs(g.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
        if (g.getCoordinates() != null) {
            g.getCoordinates().forEach(c ->
                builder.addCoordinates(Coordinate.newBuilder()
                    .setLatitude(c.getLatitude()).setLongitude(c.getLongitude()).build()));
        }
        return builder.build();
    }
}