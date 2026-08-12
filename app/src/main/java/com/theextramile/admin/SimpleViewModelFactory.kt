package com.theextramile.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory simple para crear ViewModels con dependencias.
 *
 * Uso:
 *   val vm: MyViewModel = viewModel(
 *       factory = SimpleViewModelFactory { MyViewModel(myRepo) }
 *   )
 */
class SimpleViewModelFactory<T : ViewModel>(
    private val creator: () -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
