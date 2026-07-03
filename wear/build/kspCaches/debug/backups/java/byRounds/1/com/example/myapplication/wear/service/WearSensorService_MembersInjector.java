package com.example.myapplication.wear.service;

import com.example.myapplication.wear.data.manager.WearDataManager;
import com.example.myapplication.wear.data.sensor.AccelerometerSensor;
import com.example.myapplication.wear.data.sensor.GyroscopeSensor;
import com.example.myapplication.wear.data.sensor.HeartRateSensor;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class WearSensorService_MembersInjector implements MembersInjector<WearSensorService> {
  private final Provider<HeartRateSensor> heartRateSensorProvider;

  private final Provider<AccelerometerSensor> accelerometerSensorProvider;

  private final Provider<GyroscopeSensor> gyroscopeSensorProvider;

  private final Provider<WearDataManager> wearDataManagerProvider;

  public WearSensorService_MembersInjector(Provider<HeartRateSensor> heartRateSensorProvider,
      Provider<AccelerometerSensor> accelerometerSensorProvider,
      Provider<GyroscopeSensor> gyroscopeSensorProvider,
      Provider<WearDataManager> wearDataManagerProvider) {
    this.heartRateSensorProvider = heartRateSensorProvider;
    this.accelerometerSensorProvider = accelerometerSensorProvider;
    this.gyroscopeSensorProvider = gyroscopeSensorProvider;
    this.wearDataManagerProvider = wearDataManagerProvider;
  }

  public static MembersInjector<WearSensorService> create(
      Provider<HeartRateSensor> heartRateSensorProvider,
      Provider<AccelerometerSensor> accelerometerSensorProvider,
      Provider<GyroscopeSensor> gyroscopeSensorProvider,
      Provider<WearDataManager> wearDataManagerProvider) {
    return new WearSensorService_MembersInjector(heartRateSensorProvider, accelerometerSensorProvider, gyroscopeSensorProvider, wearDataManagerProvider);
  }

  @Override
  public void injectMembers(WearSensorService instance) {
    injectHeartRateSensor(instance, heartRateSensorProvider.get());
    injectAccelerometerSensor(instance, accelerometerSensorProvider.get());
    injectGyroscopeSensor(instance, gyroscopeSensorProvider.get());
    injectWearDataManager(instance, wearDataManagerProvider.get());
  }

  @InjectedFieldSignature("com.example.myapplication.wear.service.WearSensorService.heartRateSensor")
  public static void injectHeartRateSensor(WearSensorService instance,
      HeartRateSensor heartRateSensor) {
    instance.heartRateSensor = heartRateSensor;
  }

  @InjectedFieldSignature("com.example.myapplication.wear.service.WearSensorService.accelerometerSensor")
  public static void injectAccelerometerSensor(WearSensorService instance,
      AccelerometerSensor accelerometerSensor) {
    instance.accelerometerSensor = accelerometerSensor;
  }

  @InjectedFieldSignature("com.example.myapplication.wear.service.WearSensorService.gyroscopeSensor")
  public static void injectGyroscopeSensor(WearSensorService instance,
      GyroscopeSensor gyroscopeSensor) {
    instance.gyroscopeSensor = gyroscopeSensor;
  }

  @InjectedFieldSignature("com.example.myapplication.wear.service.WearSensorService.wearDataManager")
  public static void injectWearDataManager(WearSensorService instance,
      WearDataManager wearDataManager) {
    instance.wearDataManager = wearDataManager;
  }
}
