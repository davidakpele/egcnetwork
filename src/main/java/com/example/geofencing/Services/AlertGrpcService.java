package com.example.geofencing.Services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.geofence.grpc.AlertActionRequest;
import com.geofence.grpc.AlertProto;
import com.geofence.grpc.AlertResponse;
import com.geofence.grpc.GetAlertsResponse;
import com.geofence.grpc.WatchRequest;

import java.util.UUID;

import com.geofence.grpc.AlertServiceGrpc;
import com.geofence.grpc.GetAlertsRequest;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertGrpcService extends AlertServiceGrpc.AlertServiceImplBase {
 
    private final AlertService alertService;
 
    @Override
    public void getAlerts(GetAlertsRequest request, StreamObserver<GetAlertsResponse> responseObserver) {
        try {
            var result = alertService.getAlerts(
                UUID.fromString(request.getUserId()),
                request.getUnreadOnly(),
                request.getPage(),
                request.getSize() > 0 ? request.getSize() : 20);
 
            long unreadCount = alertService.getUnreadCount(UUID.fromString(request.getUserId()));
 
            var builder = GetAlertsResponse.newBuilder()
                .setTotal((int) result.getTotalElements())
                .setUnreadCount((int) unreadCount);
 
            result.getContent().forEach(a ->
                builder.addAlerts(AlertProto.newBuilder()
                    .setId(a.getId().toString())
                    .setUserId(a.getUserId().toString())
                    .setAlertType(a.getAlertType().name())
                    .setSeverity(a.getSeverity().name())
                    .setTitle(a.getTitle())
                    .setMessage(a.getMessage())
                    .setIsRead(a.getIsRead())
                    .setIsAcknowledged(a.getIsAcknowledged())
                    .setCreatedAtMs(a.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000)
                    .build()));
 
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
 
    @Override
    public void markAlertRead(AlertActionRequest request, StreamObserver<AlertResponse> responseObserver) {
        try {
            var alert = alertService.markRead(
                UUID.fromString(request.getAlertId()), UUID.fromString(request.getUserId()));
            responseObserver.onNext(AlertResponse.newBuilder()
                .setSuccess(true).setMessage("Alert marked as read").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
 
    @Override
    public void acknowledgeAlert(AlertActionRequest request, StreamObserver<AlertResponse> responseObserver) {
        try {
            alertService.acknowledge(
                UUID.fromString(request.getAlertId()), UUID.fromString(request.getUserId()));
            responseObserver.onNext(AlertResponse.newBuilder()
                .setSuccess(true).setMessage("Alert acknowledged").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
 
    @Override
    public void streamAlerts(WatchRequest request, StreamObserver<AlertProto> responseObserver) {
        log.info("Client subscribed to alert stream for user: {}", request.getUserId());
        // In production: register observer in a registry, push alerts as they are created
        // Observer stays open until client disconnects
    }
}