@file:OptIn(ExperimentalUuidApi::class, ExperimentalComposeUiApi::class)

package com.project.chat.presentation.ui.chatDetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.chat.presentation.Res
import com.project.chat.presentation.components.ChatHeader
import com.project.chat.presentation.components.EmptySection
import com.project.chat.presentation.mediapicker.rememberCameraLauncher
import com.project.chat.presentation.mediapicker.rememberMultiImagePickerLauncher
import com.project.chat.presentation.models.ChatUi
import com.project.chat.presentation.models.MessageUi
import com.project.chat.presentation.no_chat_selected
import com.project.chat.presentation.saved_to_device
import com.project.chat.presentation.select_a_chat
import com.project.chat.presentation.ui.chatDetail.components.AttachmentSourceBottomSheet
import com.project.chat.presentation.ui.chatDetail.components.ChatDetailHeader
import com.project.chat.presentation.ui.chatDetail.components.DateChip
import com.project.chat.presentation.ui.chatDetail.components.ImageViewerOverlay
import com.project.chat.presentation.ui.chatDetail.components.MessageBannerListener
import com.project.chat.presentation.ui.chatDetail.components.MessageBox
import com.project.chat.presentation.ui.chatDetail.components.MessageList
import com.project.chat.presentation.ui.chatDetail.components.PaginationScrollListener
import com.project.core.designsystem.components.avatar.ChatParticipantUi
import com.project.core.designsystem.theme.ChirpTheme
import com.project.core.designsystem.theme.extended
import com.project.core.presentation.util.ObserveAsEvents
import com.project.core.presentation.util.UiText
import com.project.core.presentation.util.clearFocusOnTap
import com.project.core.presentation.util.currentDeviceConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun ChatDetailRoot(
    chatId: String?,
    isDetailPresent: Boolean,
    onBack: () -> Unit,
    onChatMembersClick: () -> Unit,
    viewModel: ChatDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarState = remember { SnackbarHostState() }
    val messageListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val imagePicker = rememberMultiImagePickerLauncher(
        selectionLimit = 10,
    ) { picked ->
        viewModel.onAction(ChatDetailAction.OnAttachmentsPicked(picked))
    }

    val cameraLauncher = rememberCameraLauncher { picked ->
        viewModel.onAction(ChatDetailAction.OnAttachmentsPicked(listOf(picked)))
    }

    val savedToDeviceMessage = stringResource(Res.string.saved_to_device)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChatDetailEvent.OnChatLeft -> onBack()
            ChatDetailEvent.OnNewMessage -> {
                scope.launch {
                    messageListState.animateScrollToItem(0)
                }
            }

            is ChatDetailEvent.OnError -> {
                snackbarState.showSnackbar(event.error.asStringAsync())
            }

            ChatDetailEvent.OnAttachmentSaved -> {
                snackbarState.showSnackbar(savedToDeviceMessage)
            }
        }
    }

    // Launch the picker/camera only after the attachment sheet has fully closed. Presenting while the
    // sheet is still dismissing puts the picker on the sheet's transient window, which is then torn
    // down with it (picker never appears; camera session dies with -17281).
    LaunchedEffect(state.pendingAttachmentSource, state.isAttachmentSheetOpen) {
        val source = state.pendingAttachmentSource
        if (source != null && !state.isAttachmentSheetOpen) {
            // Wait one frame so the sheet's window is fully removed before we present.
            withFrameNanos { }
            when (source) {
                AttachmentSource.CAMERA -> cameraLauncher.launch()
                AttachmentSource.GALLERY -> imagePicker.launch()
            }
            viewModel.onAction(ChatDetailAction.OnAttachmentLaunchHandled)
        }
    }

    LaunchedEffect(chatId) {
        viewModel.onAction(ChatDetailAction.OnSelectChat(chatId))
    }

    LaunchedEffect(chatId, state.messages) {
        if (state.messages.isNotEmpty()) {
            messageListState.scrollToItem(0)
        }
    }

    BackHandler(
        enabled = !isDetailPresent,
    ) {
        scope.launch {
            // Add artificial delay to prevent detail back animation from showing
            // an unselected chat the moment we go back
            delay(300)
            viewModel.onAction(ChatDetailAction.OnSelectChat(null))
        }
        onBack()
    }

    ChatDetailScreen(
        state = state,
        messageListState = messageListState,
        isDetailPresent = isDetailPresent,
        isCameraAvailable = cameraLauncher.isAvailable,
        onAction = { action ->
            when (action) {
                is ChatDetailAction.OnChatMembersClick -> onChatMembersClick()
                is ChatDetailAction.OnBackClick -> onBack()
                else -> Unit
            }
            viewModel.onAction(action)
        },
        snackbarState = snackbarState,
    )
}

@Composable
fun ChatDetailScreen(
    state: ChatDetailState,
    messageListState: LazyListState,
    isDetailPresent: Boolean,
    snackbarState: SnackbarHostState,
    onAction: (ChatDetailAction) -> Unit,
    isCameraAvailable: Boolean = false,
) {
    val configuration = currentDeviceConfiguration()

    val realMessageItemCount = remember(state.messages) {
        state
            .messages
            .filter { it is MessageUi.LocalUserMessage || it is MessageUi.OtherUserMessage }
            .size
    }

    LaunchedEffect(messageListState) {
        snapshotFlow {
            messageListState.firstVisibleItemIndex to messageListState.layoutInfo.totalItemsCount
        }.filter { (firstVisibleIndex, totalItemsCount) ->
            firstVisibleIndex >= 0 && totalItemsCount > 0
        }.collect { (firstVisibleItemIndex, _) ->
            onAction(ChatDetailAction.OnFirstVisibleIndexChanged(firstVisibleItemIndex))
        }
    }

    MessageBannerListener(
        lazyListState = messageListState,
        messages = state.messages,
        isBannerVisible = state.bannerState.isVisible,
        onShowBanner = { index ->
            onAction(ChatDetailAction.OnTopVisibleIndexChanged(index))
        },
        onHide = {
            onAction(ChatDetailAction.OnHideBanner)
        },
    )

    PaginationScrollListener(
        lazyListState = messageListState,
        itemCount = realMessageItemCount,
        isPaginationLoading = state.isPaginationLoading,
        isEndReached = state.endReached,
        onNearTop = {
            onAction(ChatDetailAction.OnScrollToTop)
        },
    )

    var headerHeight by remember {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = if (!configuration.isWideScreen) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.extended.surfaceLower
        },
        snackbarHost = {
            SnackbarHost(snackbarState)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .clearFocusOnTap()
                .padding(innerPadding)
                .then(
                    if (configuration.isWideScreen) {
                        Modifier.padding(horizontal = 8.dp)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DynamicRoundedCornerColumn(
                    isCornersRounded = configuration.isWideScreen,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (state.chatUi == null) {
                        EmptySection(
                            title = stringResource(Res.string.no_chat_selected),
                            description = stringResource(Res.string.select_a_chat),
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    } else {
                        ChatHeader(
                            modifier = Modifier
                                .onSizeChanged {
                                    headerHeight = with(density) {
                                        it.height.toDp()
                                    }
                                },
                        ) {
                            ChatDetailHeader(
                                chatUi = state.chatUi,
                                isDetailPresent = isDetailPresent,
                                isChatOptionsDropDownOpen = state.isChatOptionsOpen,
                                onChatOptionsClick = {
                                    onAction(ChatDetailAction.OnChatOptionsClick)
                                },
                                onDismissChatOptions = {
                                    onAction(ChatDetailAction.OnDismissChatOptions)
                                },
                                onManageChatClick = {
                                    onAction(ChatDetailAction.OnChatMembersClick)
                                },
                                onLeaveChatClick = {
                                    onAction(ChatDetailAction.OnLeaveChatClick)
                                },
                                onBackClick = {
                                    onAction(ChatDetailAction.OnBackClick)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        MessageList(
                            messages = state.messages,
                            messageWithOpenMenu = state.messageWithOpenMenu,
                            listState = messageListState,
                            isPaginationLoading = state.isPaginationLoading,
                            paginationError = state.paginationError?.asString(),
                            onMessageLongClick = { message ->
                                onAction(ChatDetailAction.OnMessageLongClick(message))
                            },
                            onMessageRetryClick = { message ->
                                onAction(ChatDetailAction.OnRetryClick(message))
                            },
                            onDismissMessageMenu = {
                                onAction(ChatDetailAction.OnDismissMessageMenu)
                            },
                            onDeleteMessageClick = { message ->
                                onAction(ChatDetailAction.OnDeleteMessageClick(message))
                            },
                            onRetryPaginationClick = {
                                onAction(ChatDetailAction.OnRetryPaginationClick)
                            },
                            onAttachmentClick = { attachment ->
                                onAction(ChatDetailAction.OnAttachmentClick(attachment))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )

                        AnimatedVisibility(
                            visible = !configuration.isWideScreen,
                        ) {
                            MessageBox(
                                messageTextFieldState = state.messageTextFieldState,
                                isSendButtonEnabled = state.canSendMessage,
                                connectionState = state.connectionState,
                                pendingAttachments = state.pendingAttachments,
                                isSending = state.isSending,
                                onSendClick = {
                                    onAction(ChatDetailAction.OnSendMessageClick)
                                },
                                onAttachClick = {
                                    onAction(ChatDetailAction.OnAttachClick)
                                },
                                onRemoveAttachment = { id ->
                                    onAction(ChatDetailAction.OnRemoveAttachment(id))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding()
                                    .padding(
                                        vertical = 8.dp,
                                        horizontal = 16.dp,
                                    ),
                            )
                        }
                    }
                }

                if (configuration.isWideScreen) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                AnimatedVisibility(
                    visible = configuration.isWideScreen && state.chatUi != null,
                ) {
                    DynamicRoundedCornerColumn(
                        isCornersRounded = configuration.isWideScreen,
                    ) {
                        MessageBox(
                            messageTextFieldState = state.messageTextFieldState,
                            isSendButtonEnabled = state.canSendMessage,
                            connectionState = state.connectionState,
                            pendingAttachments = state.pendingAttachments,
                            isSending = state.isSending,
                            onSendClick = {
                                onAction(ChatDetailAction.OnSendMessageClick)
                            },
                            onAttachClick = {
                                onAction(ChatDetailAction.OnAttachClick)
                            },
                            onRemoveAttachment = { id ->
                                onAction(ChatDetailAction.OnRemoveAttachment(id))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .padding(8.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = state.bannerState.isVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = headerHeight + 16.dp),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                if (state.bannerState.formattedDate != null) {
                    DateChip(
                        date = state.bannerState.formattedDate.asString(),
                    )
                }
            }

            if (state.openedAttachment != null) {
                ImageViewerOverlay(
                    attachment = state.openedAttachment,
                    isSaving = state.isSavingAttachment,
                    onSaveClick = {
                        onAction(ChatDetailAction.OnSaveOpenedAttachment)
                    },
                    onDismiss = {
                        onAction(ChatDetailAction.OnDismissAttachmentViewer)
                    },
                )
            }

            if (state.isAttachmentSheetOpen) {
                AttachmentSourceBottomSheet(
                    isCameraAvailable = isCameraAvailable,
                    onTakePhoto = {
                        onAction(ChatDetailAction.OnDismissAttachmentSheet)
                        onAction(ChatDetailAction.OnTakePhotoClick)
                    },
                    onChooseFromGallery = {
                        onAction(ChatDetailAction.OnDismissAttachmentSheet)
                        onAction(ChatDetailAction.OnPickFromGalleryClick)
                    },
                    onDismiss = {
                        onAction(ChatDetailAction.OnDismissAttachmentSheet)
                    },
                )
            }
        }
    }
}

@Composable
private fun DynamicRoundedCornerColumn(
    isCornersRounded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = if (isCornersRounded) 8.dp else 0.dp,
                shape = if (isCornersRounded) RoundedCornerShape(24.dp) else RectangleShape,
                spotColor = Color.Black.copy(alpha = 0.2f),
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = if (isCornersRounded) RoundedCornerShape(24.dp) else RectangleShape,
            ),
    ) {
        content()
    }
}

@Preview
@Composable
private fun ChatDetailEmptyPreview() {
    ChirpTheme {
        ChatDetailScreen(
            state = ChatDetailState(),
            isDetailPresent = false,
            onAction = {},
            messageListState = rememberLazyListState(),
            snackbarState = remember { SnackbarHostState() },
        )
    }
}

@Preview
@Composable
private fun ChatDetailMessagesPreview() {
    ChirpTheme(darkTheme = true) {
        ChatDetailScreen(
            messageListState = rememberLazyListState(),
            state = ChatDetailState(
                messageTextFieldState = rememberTextFieldState(
                    initialText = "This is a new message!",
                ),
                canSendMessage = true,
                chatUi = ChatUi(
                    id = "1",
                    localParticipant = ChatParticipantUi(
                        id = "1",
                        username = "Philipp",
                        initials = "PH",
                    ),
                    otherParticipants = listOf(
                        ChatParticipantUi(
                            id = "2",
                            username = "Cinderella",
                            initials = "CI",
                        ),
                        ChatParticipantUi(
                            id = "3",
                            username = "Josh",
                            initials = "JO",
                        ),
                    ),
                    lastMessage = ChatMessage(
                        id = "1",
                        chatId = "1",
                        content = "This is a last chat message that was sent by Philipp " +
                            "and goes over multiple lines to showcase the ellipsis",
                        createdAt = Clock.System.now(),
                        senderId = "1",
                        deliveryStatus = ChatMessageDeliveryStatus.SENT,
                    ),
                    lastMessageSenderUsername = "Philipp",
                ),
                messages = (1..20).map {
                    if (it % 2 == 0) {
                        MessageUi.LocalUserMessage(
                            id = Uuid.random().toString(),
                            content = "Hello world!",
                            deliveryStatus = ChatMessageDeliveryStatus.SENT,
                            formattedSentTime = UiText.DynamicString("Friday, Aug 20"),
                        )
                    } else {
                        MessageUi.OtherUserMessage(
                            id = Uuid.random().toString(),
                            content = "Hello world!",
                            sender = ChatParticipantUi(
                                id = Uuid.random().toString(),
                                username = "John",
                                initials = "JO",
                            ),
                            formattedSentTime = UiText.DynamicString("Friday, Aug 20"),
                        )
                    }
                },
            ),
            isDetailPresent = true,
            onAction = {},
            snackbarState = remember { SnackbarHostState() },
        )
    }
}
