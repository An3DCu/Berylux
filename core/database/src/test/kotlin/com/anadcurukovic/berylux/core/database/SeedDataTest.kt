package com.anadcurukovic.berylux.core.database

import com.anadcurukovic.berylux.core.database.seed.SeedData
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.MY_CASES_CATEGORY_ID
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/** Checks the seed file itself, the way a reviewer would. */
class SeedDataTest {
    private val seed = File("src/main/assets/${SeedData.ASSET_PATH}").inputStream().use(SeedData::parse)

    @Test
    fun `eight categories, six regular, one bonus and My cases`() {
        assertThat(seed.categories.groupingBy { it.kind }.eachCount())
            .containsExactly(CategoryKind.REGULAR, 6, CategoryKind.BONUS, 1, CategoryKind.CUSTOM, 1)
        assertThat(seed.categories.single { it.kind == CategoryKind.CUSTOM }.id).isEqualTo(MY_CASES_CATEGORY_ID)
    }

    @Test
    fun `My cases starts empty`() {
        assertThat(seed.challenges.none { it.categoryId == MY_CASES_CATEGORY_ID }).isTrue()
    }

    @Test
    fun `fifty-six challenges with ids 1 to 56`() {
        assertThat(seed.challenges.map { it.id }).containsExactlyElementsIn(1L..56L)
    }

    @Test
    fun `every regular category has 3 easy, 3 medium and 2 hard`() {
        seed.categories.filter { it.kind == CategoryKind.REGULAR }.forEach { category ->
            val counts = seed.challenges
                .filter { it.categoryId == category.id }
                .groupingBy { it.difficulty }
                .eachCount()
            assertThat(counts).containsExactly(Difficulty.EASY, 3, Difficulty.MEDIUM, 3, Difficulty.HARD, 2)
        }
    }

    @Test
    fun `the bonus category has 8 hard challenges`() {
        val bonus = seed.categories.single { it.kind == CategoryKind.BONUS }
        val challenges = seed.challenges.filter { it.categoryId == bonus.id }
        assertThat(challenges).hasSize(8)
        assertThat(challenges.all { it.difficulty == Difficulty.HARD }).isTrue()
    }

    @Test
    fun `every challenge has a known category, a title and a description`() {
        val categoryIds = seed.categories.map { it.id }.toSet()
        seed.challenges.forEach {
            assertThat(it.categoryId).isIn(categoryIds)
            assertThat(it.title).isNotEmpty()
            assertThat(it.description).isNotEmpty()
        }
    }

    @Test
    fun `no two challenges share a title`() {
        val titles = seed.challenges.map { it.title }
        assertThat(titles.toSet()).hasSize(titles.size)
    }
}
