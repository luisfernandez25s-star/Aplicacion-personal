package com.example.myapplication.wear.data.sensor;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AccelerometerSensor_Factory implements Factory<AccelerometerSensor> {
  private final Provider<Context> contextProvider;

  public AccelerometerSensor_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AccelerometerSensor get() {
    return newInstance(contextProvider.get());
  }

  public static AccelerometerSensor_Factory create(Provider<Context> contextProvider) {
    return new AccelerometerSensor_Factory(contextProvider);
  }

  public static AccelerometerSensor newInstance(Context context) {
    return new AccelerometerSensor(context);
  }
}
