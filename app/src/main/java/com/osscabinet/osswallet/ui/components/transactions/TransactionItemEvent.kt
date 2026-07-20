package com.osscabinet.ossWallet.ui.components.transactions

import com.osscabinet.ossWallet.domain.model.TransactionStatus

sealed interface TransactionItemEvent {
    data class Click(val id: String) : TransactionItemEvent
    data class Expand(val id: String) : TransactionItemEvent
    data class Collapse(val id: String) : TransactionItemEvent
    data class StatusChange(val id: Long, val status: TransactionStatus) : TransactionItemEvent
    data class Clone(val id: Long) : TransactionItemEvent
    data class Delete(val id: Long) : TransactionItemEvent
    data class Split(val id: Long) : TransactionItemEvent
    data class Jump(val id: Long) : TransactionItemEvent
    data class FullSwipeRight(val id: Long) : TransactionItemEvent
    data class ConfirmPlanned(val paymentId: Long, val scheduledDate: Long) : TransactionItemEvent
    data class CancelPlanned(val paymentId: Long, val scheduledDate: Long) : TransactionItemEvent
}
