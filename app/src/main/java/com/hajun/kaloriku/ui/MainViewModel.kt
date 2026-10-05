package com.hajun.kaloriku.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hajun.kaloriku.KaloriKuApp
import com.hajun.kaloriku.data.AnalysisResult
import com.hajun.kaloriku.data.dailyGoalsFlow
import com.hajun.kaloriku.data.resolveDailyGoals
import com.hajun.kaloriku.notification.MealReminderWorker
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.Food
import com.hajun.kaloriku.data.FoodDatabase
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.data.NetworkResult
import com.hajun.kaloriku.data.Profile
import com.hajun.kaloriku.data.ReminderTimes
import com.hajun.kaloriku.data.Stats
import com.hajun.kaloriku.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import com.hajun.kaloriku.util.toLocalDate

sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Loading : AnalysisState
    data class Success(val result: AnalysisResult) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

/** Item hasil analisis AI atau input manual yang porsinya bisa diubah sebelum disimpan. */
data class EditableItem(
    val base: FoodItem,
    val portion: Double = 1.0,
    val id: Long = nextItemId.incrementAndGet()
) {
    val current: FoodItem get() = base.scaled(portion)

    private companion object {
        val nextItemId = java.util.concurrent.atomic.AtomicLong()
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as KaloriKuApp).container
    private val repository = container.repository
    private val ai = container.ai

    val entries: StateFlow<List<MealEntry>> = repository.entries
    val profile: StateFlow<Profile?> = repository.profile

    val remindersEnabled = repository.remindersEnabled
    val reminderTimes = repository.reminderTimes

    fun setReminderTimes(times: ReminderTimes) {
        repository.saveReminderTimes(times)
        val context = getApplication<Application>()
        if (remindersEnabled.value) {
            MealReminderWorker.scheduleAll(context, times)
        }
    }

    val dailyGoals: StateFlow<DailyGoals> = dailyGoalsFlow(repository.profile, repository.goals)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            resolveDailyGoals(repository.profile.value, repository.goals.value)
        )

    val dailyTarget: StateFlow<Int> = dailyGoals
        .map { it.calories }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, dailyGoals.value.calories)

    // --- Analisis foto ---
    var photo by mutableStateOf<Bitmap?>(null)
        private set
    var analysisState by mutableStateOf<AnalysisState>(AnalysisState.Idle)
        private set
    var mealType by mutableStateOf(MealType.fromHour(LocalTime.now().hour))
    val editableItems = mutableStateListOf<EditableItem>()
    val canRetry: Boolean get() = lastJpeg != null

    private var lastJpeg: ByteArray? = null
    private var analysisJob: Job? = null

    // ------------------------------------------------------------------ foto

    fun analyze(uri: Uri) {
        resetAnalysis()
        analysisState = AnalysisState.Loading
        analysisJob = viewModelScope.launch {
            val prepared = try {
                withContext(Dispatchers.Default) {
                    val bitmap = ImageUtils.loadScaledBitmap(getApplication(), uri)
                    bitmap to ImageUtils.toJpeg(bitmap)
                }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
            if (prepared == null) {
                analysisState = AnalysisState.Error("Foto tidak bisa dibuka. Coba pilih foto lain.")
                return@launch
            }
            photo = prepared.first
            lastJpeg = prepared.second
            runAnalysis(prepared.second)
        }
    }

    fun retry() {
        val jpeg = lastJpeg ?: return
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch { runAnalysis(jpeg) }
    }

    fun cancelAnalysis() = resetAnalysis()

    private suspend fun runAnalysis(jpeg: ByteArray) {
        analysisState = AnalysisState.Loading
        analysisState = when (val result = ai.analyzeFood(jpeg)) {
            is NetworkResult.Success -> {
                currentCoroutineContext().ensureActive()
                editableItems.clear()
                editableItems.addAll(result.data.items.map { EditableItem(it) })
                AnalysisState.Success(result.data)
            }
            is NetworkResult.Error -> AnalysisState.Error(result.message)
        }
    }

    // ----------------------------------------------------------------- manual

    fun startManualMeal() = resetAnalysis()

    // ------------------------------------------------------------ input manual

    /** Menambahkan makanan dari basis data lokal. Nilai gizi dihitung dari berat porsi. */
    fun addManualFood(food: Food, grams: Double) {
        if (!grams.isFinite() || grams !in 1.0..2000.0) return
        editableItems.add(EditableItem(food.toFoodItem(grams)))
        analysisState = AnalysisState.Success(
            AnalysisResult(
                isFood = true,
                items = editableItems.map { it.current },
                note = "Data gizi dari Tabel Komposisi Pangan Indonesia (TKPI)."
            )
        )
        photo = null
    }

    // ------------------------------------------------------------ penyuntingan

    fun setPortion(index: Int, portion: Double) {
        if (index !in editableItems.indices || !portion.isFinite() || portion <= 0.0) return
        val item = editableItems[index]
        if (item.base.grams * portion !in 1.0..3000.0) return
        editableItems[index] = item.copy(portion = portion)
    }

    /** Mengubah berat satu item secara langsung dalam gram (dipakai input manual). */
    fun setGrams(index: Int, grams: Double) {
        if (index !in editableItems.indices || !grams.isFinite() || grams !in 1.0..3000.0) return
        val item = editableItems[index]
        if (item.base.grams <= 0) return
        editableItems[index] = item.copy(portion = grams / item.base.grams)
    }

    fun removeItem(index: Int) {
        if (index in editableItems.indices) editableItems.removeAt(index)
    }

    fun searchFoods(query: String): List<Food> = FoodDatabase.search(query)

    val foodCategories: List<String> get() = FoodDatabase.categories

    // -------------------------------------------------------------- penyimpanan

    /** Menyimpan hasil analisis ke catatan makan. Mengembalikan false jika tidak ada item. */
    fun saveMeal(): Boolean {
        if (editableItems.isEmpty()) return false
        val now = System.currentTimeMillis()
        repository.addEntry(
            MealEntry(
                id = now,
                timestamp = now,
                mealType = mealType,
                items = editableItems.map { it.current }
            )
        )
        resetAnalysis()
        return true
    }

    fun deleteEntry(id: Long) = repository.deleteEntry(id)

    fun saveProfile(profile: Profile) = repository.saveProfile(profile)

    fun saveGoals(goals: DailyGoals) = repository.saveGoals(goals.copy(calories = dailyTarget.value))

    fun setRemindersEnabled(enabled: Boolean) {
        repository.setRemindersEnabled(enabled)
        val context = getApplication<Application>()
        if (enabled) MealReminderWorker.scheduleAll(context, reminderTimes.value) else MealReminderWorker.cancelAll(context)
    }

    // ----------------------------------------------------------------- bantuan

    fun todayEntries(): List<MealEntry> {
        val today = LocalDate.now()
        return entries.value.filter { it.timestamp.toLocalDate() == today }
    }

    fun todaySummary() = Stats.summarize(LocalDate.now(), entries.value)

    private fun resetAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        photo = null
        lastJpeg = null
        editableItems.clear()
        analysisState = AnalysisState.Idle
        mealType = MealType.fromHour(LocalTime.now().hour)
    }

}
