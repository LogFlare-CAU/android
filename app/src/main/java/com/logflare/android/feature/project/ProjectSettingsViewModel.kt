package com.logflare.android.feature.project

import com.logflare.android.data.AuthRepository
import com.logflare.android.data.ProjectsRepository
import com.logflare.android.feature.usecase.GetProjectPermsUseCase
import com.logflare.android.feature.usecase.GetUsersUseCase
import com.logflare.android.feature.usecase.UpdateProjectPermUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProjectSettingsViewModel @Inject constructor(
    repo: ProjectsRepository,
    authRepository: AuthRepository,
    updateProjectPermUseCase: UpdateProjectPermUseCase,
    getUsersUseCase: GetUsersUseCase,
    getProjectpermsUseCase: GetProjectPermsUseCase,
) : ProjectEditorViewModel(
    repo,
    authRepository,
    updateProjectPermUseCase,
    getUsersUseCase,
    getProjectpermsUseCase,
)
