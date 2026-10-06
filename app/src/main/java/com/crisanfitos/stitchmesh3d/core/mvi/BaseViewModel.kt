package com.crisanfitos.stitchmesh3d.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel base para arquitectura MVI (Model-View-Intent) con flujo unidireccional de datos (UDF).
 *
 * @param S Tipo de estado inmutable (implementa ViewState)
 * @param I Tipo de intención de usuario (implementa ViewIntent)
 * @param E Tipo de efecto secundario de un solo uso (implementa ViewEffect)
 */
abstract class BaseViewModel<S : ViewState, I : ViewIntent, E : ViewEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<E>()
    val effect: SharedFlow<E> = _effect.asSharedFlow()

    /**
     * Procesa una intención de usuario o evento del sistema.
     */
    abstract fun processIntent(intent: I)

    /**
     * Actualiza el estado actual de forma atómica.
     */
    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    /**
     * Emite un efecto secundario de un solo uso en el viewModelScope.
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
