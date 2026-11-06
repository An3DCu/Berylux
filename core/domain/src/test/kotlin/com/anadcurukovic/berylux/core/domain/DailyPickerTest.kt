package com.anadcurukovic.berylux.core.domain

import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DailyPickerTest {
    private val body = Category(id = 1, name = "Body", description = "", kind = CategoryKind.REGULAR)
    private val mind = Category(id = 2, name = "Mind", description = "", kind = CategoryKind.REGULAR)
    private val epic = Category(id = 3, name = "Epic", description = "", kind = CategoryKind.BONUS)
    private val categories = listOf(body, mind, epic)

    private fun challenge(id: Long, category: Category, isCustom: Boolean = false) = Challenge(
        id = id,
        categoryId = category.id,
        title = "Challenge $id",
        description = "",
        difficulty = Difficulty.MEDIUM,
        isCustom = isCustom,
    )

    private val challenges =
        (1L..10L).map { challenge(it, body) } +
            (11L..20L).map { challenge(it, mind) } +
            (21L..25L).map { challenge(it, epic) } +
            challenge(100, body, isCustom = true)

    private val day = LocalDate.of(2026, 11, 6)
    private val seed = 1234L

    private fun pick(
        list: List<Challenge> = challenges,
        on: LocalDate = day,
        profileSeed: Long = seed,
        filter: Long? = null,
    ) = DailyPicker.pick(list, categories, on, profileSeed, filter)

    @Test
    fun `a day offers one daily case, one bonus case and four picks`() {
        val result = pick()
        assertThat(result.daily!!.categoryId).isNotEqualTo(epic.id)
        assertThat(result.bonus!!.categoryId).isEqualTo(epic.id)
        assertThat(result.picks).hasSize(4)
        assertThat(result.picks.map { it.categoryId }).doesNotContain(epic.id)
        assertThat(result.picks).doesNotContain(result.daily)
        assertThat(result.picks.toSet()).hasSize(4)
    }

    @Test
    fun `custom challenges are never picked`() {
        val result = pick()
        val picked = result.picks + listOfNotNull(result.daily, result.bonus)
        assertThat(picked.none { it.isCustom }).isTrue()
    }

    @Test
    fun `the pick stays the same all day`() {
        assertThat(pick()).isEqualTo(pick())
    }

    @Test
    fun `the order the challenges come in doesn't matter`() {
        assertThat(pick(list = challenges.reversed())).isEqualTo(pick())
    }

    @Test
    fun `the daily case changes from day to day`() {
        val dailies = (0L until 30L).map { pick(on = day.plusDays(it)).daily }
        assertThat(dailies.toSet().size).isGreaterThan(1)
    }

    @Test
    fun `two players get different days`() {
        val first = (0L until 10L).map { pick(on = day.plusDays(it), profileSeed = 1) }
        val second = (0L until 10L).map { pick(on = day.plusDays(it), profileSeed = 2) }
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `a category filter changes the picks, not the daily or bonus case`() {
        val unfiltered = pick()
        val filtered = pick(filter = mind.id)
        assertThat(filtered.picks).hasSize(4)
        assertThat(filtered.picks.all { it.categoryId == mind.id }).isTrue()
        assertThat(filtered.daily).isEqualTo(unfiltered.daily)
        assertThat(filtered.bonus).isEqualTo(unfiltered.bonus)
        assertThat(filtered.picks).doesNotContain(filtered.daily)
    }

    @Test
    fun `a small pool gives what it has`() {
        val small = listOf(challenge(1, body), challenge(2, mind))
        val result = pick(list = small)
        assertThat(result.daily).isNotNull()
        assertThat(result.picks).hasSize(1)
        assertThat(result.bonus).isNull()
    }

    @Test
    fun `no challenges, nothing to pick`() {
        assertThat(pick(list = emptyList())).isEqualTo(DailyPick(daily = null, bonus = null, picks = emptyList()))
    }
}
