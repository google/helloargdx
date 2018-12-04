package com.github.claywilkinson.helloargdx;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;

public class NonARFragmentApplication  extends AndroidFragmentApplication {

  private ApplicationListener scene;

  public void setScene(ApplicationListener scene) {
    this.scene = scene;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
   return initializeForView(scene);
  }
}
