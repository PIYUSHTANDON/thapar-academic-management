package com.thapar.ams.desktop.ui.student;

import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.desktop.api.AccountApi;
import com.thapar.ams.desktop.api.WorkspaceApi;
import com.thapar.ams.desktop.ui.SettingsView;
import com.thapar.ams.desktop.ui.TaskRunner;
import java.util.function.Consumer;

public final class StudentSettingsView extends SettingsView {
  public StudentSettingsView(AccountApi account, WorkspaceApi workspace, TaskRunner tasks,
                             UserResponse user, Consumer<UserResponse> userChanged, Runnable passwordChanged,
                             Runnable signOut) {
    super(account, workspace, tasks, user, userChanged, passwordChanged, signOut);
  }
}
