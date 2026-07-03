package com.example.myapplication.wear;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<HeartRateSensor> heartRateSensorProvider;

  private final Provider<AccelerometerSensor> accelerometerSensorProvider;

  private final Provider<GyroscopeSensor> gyroscopeSensorProvider;

  public MainActivity_MembersInjector(Provider<HeartRateSensor> heartRateSensorProvider,
      Provider<AccelerometerSensor> accelerometerSensorProvider,
      Provider<GyroscopeSensor> gyroscopeSensorProvider) {
    this.heartRateSensorProvider = heartRateSensorProvider;
    this.accelerometerSensorProvider = accelerometerSensorProvider;
    this.gyroscopeSensorProvider = gyroscopeSensorProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<HeartRateSensor> heartRateSensorProvider,
      Provider<AccelerometerSensor> accelerometerSensorProvider,
      Provider<GyroscopeSensor> gyroscopeSensorProvider) {
    return new MainActivity_MembersInjector(heartRateSensorProvider, accelerometerSensorProvider, gyroscopeSensorProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectHeartRateSensor(instance, heartRateSensorProvider.get());
    injectAccelerometerSensor(instance, accelerometerSensorProvider.get());
    injectGyroscopeSensor(instance, gyroscopeSensorProvider.get());
  }

  @InjectedFieldSignature("com.example.myapplication.wear.MainActivity.heartRateSensor")
  public static void injectHeartRateSensor(MainActivity instance, HeartRateSensor heartRateSensor) {
    instance.heartRateSensor = heartRateSensor;
  }

  @InjectedFieldSignature("com.example.myapplication.wear.MainActivity.accelerometerSensor")
  public static void injectAccelerometerSensor(MainActivity instance,
      AccelerometerSensor accelerometerSensor) {
    instance.accelerometerSensor = accelerometerSensor;
  }

  @InjectedFieldSignature("com.example.myapplication.wear.MainActivity.gyroscopeSensor")
  public static void injectGyroscopeSensor(MainActivity instance, GyroscopeSensor gyroscopeSensor) {
    instance.gyroscopeSensor = gyroscopeSensor;
  }
}
