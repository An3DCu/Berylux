package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.MY_CASES_CATEGORY_ID
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChallengeRulesTest {
    @Test
    fun `a title is needed, spaces don't count`() {
        assertThat(ChallengeRules.problems("   ", "")).containsExactly(ChallengeProblem.TITLE_EMPTY)
    }

    @Test
    fun `the title can have 40 characters, not 41`() {
        assertThat(ChallengeRules.problems("a".repeat(40), "")).isEmpty()
        assertThat(ChallengeRules.problems("a".repeat(41), "")).containsExactly(ChallengeProblem.TITLE_TOO_LONG)
    }

    @Test
    fun `the description is optional and can have 200 characters, not 201`() {
        assertThat(ChallengeRules.problems("Read", "b".repeat(200))).isEmpty()
        assertThat(ChallengeRules.problems("Read", "b".repeat(201)))
            .containsExactly(ChallengeProblem.DESCRIPTION_TOO_LONG)
    }

    @Test
    fun `every problem comes back at once`() {
        assertThat(ChallengeRules.problems("", "b".repeat(201)))
            .containsExactly(ChallengeProblem.TITLE_EMPTY, ChallengeProblem.DESCRIPTION_TOO_LONG)
    }

    @Test
    fun `the title is trimmed with its spaces squeezed, the description keeps its lines`() {
        assertThat(ChallengeRules.cleanTitle("  Read   a poem ")).isEqualTo("Read a poem")
        assertThat(ChallengeRules.cleanDescription("  Out loud.\nTo someone.  ")).isEqualTo("Out loud.\nTo someone.")
    }
}

class SaveCustomChallengeUseCaseTest {
    private val challenges = FakeChallengeRepository(
        challenges = listOf(Challenge(1, 1, "Brisk walk", "", Difficulty.EASY)),
    )
    private val save = SaveCustomChallengeUseCase(challenges)

    @Test
    fun `a new challenge goes in My cases, cleaned`() = runTest {
        val result = save(id = null, title = " Read  a poem ", description = " Out loud. ") as SaveResult.Saved
        val saved = challenges.challenge(result.id)!!
        assertThat(saved.title).isEqualTo("Read a poem")
        assertThat(saved.description).isEqualTo("Out loud.")
        assertThat(saved.categoryId).isEqualTo(MY_CASES_CATEGORY_ID)
        assertThat(saved.isCustom).isTrue()
    }

    @Test
    fun `a bad form saves nothing`() = runTest {
        assertThat(save(id = null, title = "", description = ""))
            .isEqualTo(SaveResult.Invalid(setOf(ChallengeProblem.TITLE_EMPTY)))
        assertThat(challenges.challenges.first()).hasSize(1)
    }

    @Test
    fun `changing one keeps its id`() = runTest {
        val id = (save(id = null, title = "Read a poem", description = "") as SaveResult.Saved).id
        assertThat(save(id = id, title = "Read two poems", description = "")).isEqualTo(SaveResult.Saved(id))
        assertThat(challenges.challenge(id)!!.title).isEqualTo("Read two poems")
    }

    @Test
    fun `a built-in challenge can't be changed`() = runTest {
        assertThat(save(id = 1, title = "Mine now", description = "")).isEqualTo(SaveResult.NotFound)
        assertThat(challenges.challenge(1)!!.title).isEqualTo("Brisk walk")
    }
}
