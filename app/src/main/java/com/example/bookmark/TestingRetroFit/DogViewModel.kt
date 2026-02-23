package com.example.bookmark.TestingRetroFit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch


//sealed class UiState{
//    object Loading: UiState()
//    data class Succes(val dogs:List<String>) :UiState()
//    data class Error(val message: String): UiState()
//}
//
//class DogViewModel : ViewModel(){
//    private val repository = PostRepository()
//
//    private var _state: MutableState<UiState> = mutableStateOf(UiState.Loading)
//
//    var state: State<UiState> = _state
//
//    init {
//        fetchPosts()
//    }
//
//
//    private fun fetchPosts(){
//        viewModelScope.launch{
//            _state.value = UiState.Loading
//            try{
//                val response = repository.getPosts()
//                _state.value = UiState.Succes(response)
//
//            }catch (e:Exception){
//                _state.value = UiState.Error("Failed to Load( ${e.localizedMessage})")
//            }
//
//
//        }
//
//    }
//
//}