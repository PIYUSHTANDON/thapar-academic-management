package com.thapar.ams.desktop.ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

@FunctionalInterface
public interface TaskRunner {
  <T> void run(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure);
}
