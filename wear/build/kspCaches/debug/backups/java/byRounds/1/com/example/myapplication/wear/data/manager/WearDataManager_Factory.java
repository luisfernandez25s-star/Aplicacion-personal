package com.example.myapplication.wear.data.manager;

import android.content.Context;
import com.google.gson.Gson;
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
public final class WearDataManager_Factory implements Factory<WearDataManager> {
  private final Provider<Context> contextProvider;

  private final Provider<Gson> gsonProvider;

  public WearDataManager_Factory(Provider<Context> contextProvider, Provider<Gson> gsonProvider) {
    this.contextProvider = contextProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public WearDataManager get() {
    return newInstance(contextProvider.get(), gsonProvider.get());
  }

  public static WearDataManager_Factory create(Provider<Context> contextProvider,
      Provider<Gson> gsonProvider) {
    return new WearDataManager_Factory(contextProvider, gsonProvider);
  }

  public static WearDataManager newInstance(Context context, Gson gson) {
    return new WearDataManager(context, gson);
  }
}
