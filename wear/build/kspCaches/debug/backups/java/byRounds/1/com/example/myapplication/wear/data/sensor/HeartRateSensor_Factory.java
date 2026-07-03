package com.example.myapplication.wear.data.sensor;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class HeartRateSensor_Factory implements Factory<HeartRateSensor> {
  private final Provider<Context> contextProvider;

  public HeartRateSensor_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public HeartRateSensor get() {
    return newInstance(contextProvider.get());
  }

  public static HeartRateSensor_Factory create(Provider<Context> contextProvider) {
    return new HeartRateSensor_Factory(contextProvider);
  }

  public static HeartRateSensor newInstance(Context context) {
    return new HeartRateSensor(context);
  }
}
