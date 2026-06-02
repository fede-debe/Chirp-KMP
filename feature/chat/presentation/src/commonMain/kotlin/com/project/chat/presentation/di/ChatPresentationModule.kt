package com.project.chat.presentation.di

import com.project.chat.presentation.ui.chatDetail.ChatDetailViewModel
import com.project.chat.presentation.ui.chatList.ChatListViewModel
import com.project.chat.presentation.ui.chatListDetail.ChatListDetailViewModel
import com.project.chat.presentation.ui.createChat.CreateChatViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatPresentationModule = module {
    viewModelOf(::ChatListViewModel)
    viewModelOf(::ChatListDetailViewModel)
    viewModelOf(::CreateChatViewModel)
    viewModelOf(::ChatDetailViewModel)
}
