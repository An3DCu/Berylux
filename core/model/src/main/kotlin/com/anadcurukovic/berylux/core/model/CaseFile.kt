package com.anadcurukovic.berylux.core.model

/** A case together with the challenge it's about, the way lists show it. */
data class CaseFile(
    val case: Case,
    val challenge: Challenge,
)
