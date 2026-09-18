package com.noorpro.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noorpro.app.data.ZakatDatabase
import com.noorpro.app.data.ZakatHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ZakatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZakatDatabase.getDatabase(application)
    private val dao = db.zakatDao()

    val history: StateFlow<List<ZakatHistoryEntity>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveZakatHistory(
        goldValue: Double,
        silverValue: Double,
        cashValue: Double,
        businessValue: Double,
        assetsValue: Double,
        totalAssets: Double,
        zakatDue: Double
    ) {
        viewModelScope.launch {
            dao.insert(
                ZakatHistoryEntity(
                    totalAssets = totalAssets,
                    zakatDue = zakatDue,
                    goldValue = goldValue,
                    silverValue = silverValue,
                    cashValue = cashValue,
                    businessValue = businessValue,
                    assetsValue = assetsValue
                )
            )
        }
    }
    
    fun deleteHistory(item: ZakatHistoryEntity) {
        viewModelScope.launch {
            dao.delete(item)
        }
    }
}
