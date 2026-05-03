package com.logflare.android.feature.usecase

import com.example.logflare.core.model.ProjectData
import com.logflare.android.data.ProjectsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetProjectDetailUseCase @Inject constructor(
    private val projectsRepository: ProjectsRepository,
){
    suspend operator fun invoke(projectId: Int): ProjectData? {
         return projectsRepository.get(projectId)
    }
}