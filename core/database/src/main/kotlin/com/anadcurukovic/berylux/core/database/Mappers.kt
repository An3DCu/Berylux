package com.anadcurukovic.berylux.core.database

import com.anadcurukovic.berylux.core.database.entity.CaseEntity
import com.anadcurukovic.berylux.core.database.entity.CategoryEntity
import com.anadcurukovic.berylux.core.database.entity.ChallengeEntity
import com.anadcurukovic.berylux.core.database.entity.ProfileEntity
import com.anadcurukovic.berylux.core.model.Case
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Profile

fun ProfileEntity.asModel() = Profile(id = id, name = name, lux = lux, pickSeed = pickSeed, createdAt = createdAt)

fun CategoryEntity.asModel() = Category(id = id, name = name, description = description, kind = kind)

fun ChallengeEntity.asModel() = Challenge(
    id = id,
    categoryId = categoryId,
    title = title,
    description = description,
    difficulty = difficulty,
    isCustom = isCustom,
)

fun Challenge.asEntity() = ChallengeEntity(
    id = id,
    categoryId = categoryId,
    title = title,
    description = description,
    difficulty = difficulty,
    isCustom = isCustom,
)

fun CaseEntity.asModel() = Case(
    id = id,
    challengeId = challengeId,
    status = status,
    isDaily = isDaily,
    acceptedAt = acceptedAt,
    acceptedOn = acceptedOn,
    closedAt = closedAt,
    closedOn = closedOn,
    luxEarned = luxEarned,
)
