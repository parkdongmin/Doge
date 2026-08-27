package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.CloudSave

interface CloudSaveRepository {
    /** saves/{uid} 문서를 읽는다. 문서가 없으면 success(null). */
    suspend fun pull(uid: String): Result<CloudSave?>

    /** saves/{uid} 문서를 통째로 덮어쓴다. */
    suspend fun push(uid: String, save: CloudSave): Result<Unit>
}
